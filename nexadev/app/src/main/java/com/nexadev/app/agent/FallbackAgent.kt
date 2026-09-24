package com.nexadev.app.agent

import com.nexadev.app.mistral.MistralApi
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Agent de secours : boucle d'outils native (type Codex) pilotée par l'API Mistral,
 * utilisée si le binaire Rust Codex n'est pas présent dans l'APK.
 * Outils : lecture/écriture de fichiers, liste de répertoire, exécution de commandes shell.
 * Chaque modification de fichier passe par confirm (l'utilisateur accepte ou rejette).
 */
class FallbackAgent(
    private val root: File,
    private val apiKey: String,
    private val model: String,
    private val confirm: (path: String, newContent: String) -> Boolean
) {
    private val history = mutableListOf<Pair<String, String>>()

    private val TOOLS = """
        [
          {"type":"function","function":{"name":"read_file","description":"Lit un fichier du projet","parameters":{"type":"object","properties":{"path":{"type":"string"}},"required":["path"]}}},
          {"type":"function","function":{"name":"list_dir","description":"Liste un répertoire","parameters":{"type":"object","properties":{"path":{"type":"string"}},"required":["path"]}}},
          {"type":"function","function":{"name":"write_file","description":"Crée ou remplace un fichier (soumis à confirmation)","parameters":{"type":"object","properties":{"path":{"type":"string"},"content":{"type":"string"}},"required":["path","content"]}}},
          {"type":"function","function":{"name":"run_command","description":"Exécute une commande shell Android (sh)","parameters":{"type":"object","properties":{"cmd":{"type":"string"}},"required":["cmd"]}}}
        ]
    """.trimIndent()

    /** Exécute une demande utilisateur en boucle agent (max 8 tours d'outils). */
    fun run(userRequest: String, onLog: (String) -> Unit): String {
        history.add("user" to userRequest)
        var final = ""
        for (turn in 0 until 8) {
            val resp = MistralApi.chat(apiKey, model, history, TOOLS)
            val msg = resp.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
            val toolCalls = msg.optJSONArray("tool_calls")
            if (toolCalls == null || toolCalls.length() == 0) {
                final = msg.getString("content") ?: "(réponse vide)"
                history.add("assistant" to final)
                break
            }
            history.add("assistant" to msg.toString())
            val results = JSONArray()
            for (i in 0 until toolCalls.length()) {
                val tc = toolCalls.getJSONObject(i)
                val fn = tc.getJSONObject("function")
                val name = fn.getString("name")
                val args = JSONObject(fn.optString("arguments", "{}"))
                val result = try { execTool(name, args, onLog) } catch (e: Exception) { "ERREUR: " + e.message }
                onLog("→ " + name + " = " + result.take(200))
                results.put(JSONObject()
                    .put("role", "tool")
                    .put("tool_call_id", tc.getString("id"))
                    .put("content", result.take(15000)))
            }
            history.add("tool" to results.toString())
        }
        return final
    }

    private fun safe(path: String): File {
        val f = File(root, path).canonicalFile
        require(f.startsWith(root.canonicalFile)) { "Chemin hors projet interdit: " + path }
        return f
    }

    private fun execTool(name: String, args: JSONObject, onLog: (String) -> Unit): String = when (name) {
        "read_file" -> safe(args.getString("path")).readText().take(30000)
        "list_dir" -> safe(args.optString("path", ".")).listFiles()?.joinToString("\n") ?: "(vide)"
        "write_file" -> {
            val f = safe(args.getString("path"))
            val content = args.getString("content")
            if (!confirm(f.relativeTo(root).path, content)) "REFUSÉ par l'utilisateur"
            else { f.parentFile?.mkdirs(); f.writeText(content); "OK: " + f.name + " (" + content.length + " octets écrits)" }
        }
        "run_command" -> {
            val cmd = args.getString("cmd")
            onLog("$ " + cmd)
            val p = ProcessBuilder("sh", "-c", cmd).directory(root).start()
            val out = p.inputStream.bufferedReader().readText().take(20000)
            val err = p.errorStream.bufferedReader().readText().take(5000)
            p.waitFor()
            "exit=" + p.exitValue() + "\n" + out + "\n" + err
        }
        else -> "Outil inconnu: " + name
    }
}
