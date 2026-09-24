package com.nexadev.app.codex

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter

/**
 * Pont vers le véritable moteur Codex (open source, Apache-2.0, github.com/openai/codex).
 * Le binaire Rust cross-compilé pour Android ARM64 est embarqué sous le nom
 * "libcodex.so" (jniLibs) et exécuté en mode app-server (JSON-RPC sur stdio).
 */
class CodexProcess(
    private val ctx: Context,
    private val projectDir: File,
    private val mistralKey: String,
    private val model: String
) {
    private var proc: Process? = null
    private var out: PrintWriter? = null

    val available: Boolean get() = locateBinary() != null

    private fun locateBinary(): File? {
        val dir = File(ctx.applicationInfo.nativeLibraryDir)
        return dir.listFiles()?.firstOrNull { it.name.startsWith("libcodex") }
    }

    /** Écrit le config.toml de Codex avec Mistral comme fournisseur principal. */
    private fun writeConfig(codexHome: File) {
        val cfg = """
            # Fournisseur IA principal : Mistral (La Plateforme). OpenAI désactivé par défaut.
            model = "$model"
            model_provider = "mistral"
            approval_policy = "on-request"
            sandbox_mode = "workspace-write"

            [model_providers.mistral]
            name = "Mistral"
            base_url = "https://api.mistral.ai/v1"
            env_key = "MISTRAL_API_KEY"
            wire_api = "chat"
        """.trimIndent()
        File(codexHome, "config.toml").writeText(cfg)
    }

    @Synchronized
    fun start(): Boolean {
        val bin = locateBinary() ?: return false
        val codexHome = File(ctx.filesDir, "codex-home").apply { mkdirs() }
        writeConfig(codexHome)
        val pb = ProcessBuilder(bin.absolutePath, "app-server")
            .directory(projectDir)
            .redirectErrorStream(false)
        pb.environment().apply {
            put("HOME", codexHome.absolutePath)
            put("CODEX_HOME", codexHome.absolutePath)
            put("MISTRAL_API_KEY", mistralKey)
            put("PATH", "/system/bin:/system/xbin")
            put("TERM", "dumb")
            put("TMPDIR", ctx.cacheDir.absolutePath)
        }
        proc = pb.start()
        out = PrintWriter(proc!!.outputStream, true)
        return true
    }

    fun send(json: String) { out?.println(json) }

    /** Flux d'événements JSON (une ligne = un événement du protocole app-server). */
    fun events(onEvent: (String) -> Unit, onExit: (Int) -> Unit) {
        val p = proc ?: return
        Thread {
            try {
                BufferedReader(InputStreamReader(p.inputStream)).useLines { lines ->
                    lines.forEach { if (it.isNotBlank()) onEvent(it.trim()) }
                }
            } catch (_: Exception) { }
            onExit(p.waitFor())
        }.start()
    }

    fun stop() {
        try { proc?.destroy() } catch (_: Exception) { }
        proc = null
    }
}
