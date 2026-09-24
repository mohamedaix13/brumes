package com.nexadev.app.builder

import com.nexadev.app.mistral.MistralApi
import org.json.JSONObject
import java.io.File

/** Genere un projet Android complet a partir d'une demande en langage naturel. */
class AndroidBuilder(private val apiKey: String, private val model: String) {

    /** Le modele retourne un JSON {name, description, files:[{path,content}]}. */
    fun generate(userRequest: String, onProgress: (String) -> Unit): JSONObject {
        onProgress("Planification du projet avec " + model + "...")
        val system = "Tu es un generateur d'applications Android. Reponds UNIQUEMENT avec un JSON valide, " +
            "sans markdown, de la forme : " +
            '"""' + '{"name":"NomApp","description":"...","files":[{"path":"chemin","content":"contenu"}]}' + '"""' +
            ". Genere un projet Android minimal mais complet et compilable avec Gradle (Kotlin + Compose) : " +
            "settings.gradle.kts, build.gradle.kts, app/build.gradle.kts, gradle.properties avec " +
            "android.useAndroidX=true, AndroidManifest.xml, MainActivity.kt (package com.exemple.generated), " +
            "ressources (strings.xml, theme) et README.md. Demande de l'utilisateur : " + userRequest
        val resp = MistralApi.chat(apiKey, model, listOf("system" to system, "user" to userRequest), temperature = 0.3)
        val content = resp.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        var cleaned = content.trim()
        if (cleaned.startsWith("`json`")) cleaned = cleaned.removePrefix("`json`").trim()
        if (cleaned.startsWith("`")) cleaned = cleaned.removePrefix("`").trim()
        if (cleaned.endsWith("`")) cleaned = cleaned.removeSuffix("`").trim()
        val start = cleaned.indexOf("{")
        val end = cleaned.lastIndexOf("}")
        require(start in 0..end) { "Reponse du modele non JSON" }
        return JSONObject(cleaned.substring(start, end + 1))
    }

    /** Ecrit chaque fichier sur le disque et verifie qu'il existe reellement. */
    fun materialize(root: File, plan: JSONObject, onProgress: (String) -> Unit): List<File> {
        val written = mutableListOf<File>()
        val files = plan.getJSONArray("files")
        for (i in 0 until files.length()) {
            val f = files.getJSONObject(i)
            val path = f.getString("path")
            val content = f.getString("content")
            val target = File(root, path).apply { parentFile?.mkdirs() }
            target.writeText(content)
            require(target.exists() && target.length() > 0) { "Echec d'ecriture : " + path }
            written.add(target)
            onProgress("OK " + path + " (" + content.length + " octets)")
        }
        return written
    }
}
