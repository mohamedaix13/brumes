package com.nexadev.app.builder

import com.nexadev.app.mistral.MistralApi
import org.json.JSONObject
import java.io.File

/**
 * Builder : crée un projet Android complet à partir d'une demande en langage naturel.
 * L'agent Mistral génère la structure (Gradle, manifest, activités Compose, ressources),
 * puis le projet peut être compilé en APK via la CI GitHub (sans PC).
 */
class AppBuilder(
    private val apiKey: String,
    private val model: String,
    private val onLog: (String) -> Unit
) {
    private val SYSTEM = """Tu es un générateur d'applications Android.
A partir d'une demande utilisateur, produis UNIQUEMENT un objet JSON valide (sans markdown) :
{"appName":"NomSansEspaces","description":"courte description","files":[{"path":"chemin/relatif","content":"contenu complet"}]}
Contraintes :
- Kotlin + Jetpack Compose, minSdk 26, targetSdk 34, pas de dépendances externes (utiliser java.net si réseau).
- Inclus TOUJOURS : settings.gradle.kts, build.gradle.kts, app/build.gradle.kts, app/src/main/AndroidManifest.xml, MainActivity.kt, res/values/strings.xml, res/values/themes.xml.
- Un seul écran Compose si possible. Code complet, sans placeholder TODO."""

    /** Génère et écrit le projet dans [outDir]. Retourne la description. */
    fun buildFromPrompt(prompt: String, outDir: File): String {
        onLog("Génération du projet avec Mistral ($model)…")
        val resp = MistralApi.chat(apiKey, model, listOf(
            "system" to SYSTEM,
            "user" to prompt
        ), temperature = 0.3)
        val content = resp.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        val json = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = JSONObject(json)
        val desc = obj.optString("description", "")
        val files = obj.getJSONArray("files")
        onLog("Écriture de " + files.length() + " fichiers…")
        for (i in 0 until files.length()) {
            val f = files.getJSONObject(i)
            val target = File(outDir, f.getString("path"))
            require(target.canonicalPath.startsWith(outDir.canonicalPath)) { "chemin invalide" }
            target.parentFile?.mkdirs()
            target.writeText(f.getString("content"))
            onLog("  + " + f.getString("path"))
        }
        onLog("Projet prêt dans " + outDir.name)
        return desc
    }
}