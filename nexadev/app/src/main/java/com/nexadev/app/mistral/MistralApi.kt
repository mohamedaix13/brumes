package com.nexadev.app.mistral

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Client officiel de l'API Mistral (La Plateforme). La clé appartient à l'utilisateur. */
object MistralApi {
    const val BASE = "https://api.mistral.ai/v1"
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Vérifie la clé et retourne la liste des modèles autorisés par cette clé. */
    fun listModels(apiKey: String): List<String> {
        val req = Request.Builder().url("$BASE/models")
            .header("Authorization", "Bearer $apiKey").build()
        http.newCall(req).execute().use { resp ->
            check(resp.isSuccessful) { "Clé Mistral invalide (HTTP ${resp.code})" }
            val data = JSONObject(resp.body!!.string()).getJSONArray("data")
            return (0 until data.length()).map { data.getJSONObject(it).getString("id") }.sorted()
        }
    }

    /** Chat completion non-streaming avec outils optionnels. */
    fun chat(apiKey: String, model: String, messages: List<Pair<String, String>>, tools: String? = null, temperature: Double = 0.2): JSONObject {
        val msgs = JSONArray()
        messages.forEach { (role, content) -> msgs.put(JSONObject().put("role", role).put("content", content)) }
        val body = JSONObject()
            .put("model", model)
            .put("messages", msgs)
            .put("temperature", temperature)
        if (tools != null) body.put("tools", JSONArray(tools))
        val req = Request.Builder().url("$BASE/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody(JSON)).build()
        http.newCall(req).execute().use { resp ->
            val txt = resp.body!!.string()
            check(resp.isSuccessful) { "Mistral HTTP ${resp.code}: ${txt.take(400)}" }
            return JSONObject(txt)
        }
    }
}
