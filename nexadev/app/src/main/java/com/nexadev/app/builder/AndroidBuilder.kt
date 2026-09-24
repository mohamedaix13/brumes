package com.nexadev.app.builder

import com.nexadev.app.mistral.MistralApi
import org.json.JSONObject
import java.io.File

/**
 * Builder : génère un projet Android complet à partir d une demande en langage naturel,
 * comme sur Codex/Astra desktop. Le modèle Mistral produit un plan puis les fichiers ;
 * chaque fichier écrit est réel et vérifié sur le disque.
 */
class AndroidBuilder(
    private val apiKey: String,
    private val model: String
) {
    /** Le modèle retourne un JSON {name, description, files:[{path,content}]}. */
    fun generate(userRequest: String, onProgress: (String) -> Unit): JSONObject {
        onProgress("Planification du projet avec " + model + "…")
        val system = "Tu es un générateur d applications Android. Réponds UNIQUEMENT avec un JSON valide, sans markdown, de la forme : " +
            '{"name":"NomApp","description":"...","files":[{"path":"chemin","content":"contenu"}]}' + ". " +
            "Génère un projet Android minimal mais complet et compilable avec Gradle (Kotlin + Compose) : " +
            "settings.gradle.kts, build.gradle.kts, app/build.gradle.kts, AndroidManifest.xml, " +
            "MainActivity.kt (package com.exemple.generated), ressources (strings.xml, thème) et README.md. " +
            "Demande de l utilisateur : " + userRequest
        val resp = MistralApi.chat(apiKey, model, listOf("system" to system, "user" to userRequest), temperature = 0.3)
        val content = resp.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        val cleaned = content
            .replace(Regex("^" + BT + BT + BT + "json\\n?"), "")
            .replace(Regex(BT + BT + BT + "$"), "")
            .trim()
        return JSONObject(cleaned)
    }

    /** Écrit chaque fichier sur le disque et vérifie qu il existe réellement. */
    fun materialize(root: File, plan: JSONObject, onProgress: (String) -> Unit): List<File> {
        val written = mutableListOf<File>()
        val files = plan.getJSONArray("files")
        for (i in 0 until files.length()) {
            val f = files.getJSONObject(i)
            val path = f.getString("path")
            val content = f.getString("content")
            val target = File(root, path).apply { parentFile?.mkdirs() }
            target.writeText(content)
            require(target.exists() && target.length() > 0) { "Échec d écriture : " + path }
            written.add(target)
            onProgress("OK " + path + " (" + content.length + " octets)")
        }
        return written
    }
}
