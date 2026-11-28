package com.manekdm13.jarvis

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Local Ollama inference adapter.
 *
 * Configure Ollama on a reachable computer/server and expose its API only on a trusted
 * private network. Android's localhost is the phone itself, not the user's PC.
 * This adapter does not upload prompts to a hosted model provider.
 */
internal object JarvisOllamaClient {
    data class Model(val name: String, val sizeBytes: Long = 0L, val modifiedAt: String = "")

    data class Reply(
        val text: String,
        val model: String,
        val promptTokens: Int = 0,
        val responseTokens: Int = 0,
        val totalDurationNanos: Long = 0L
    )

    fun listModels(baseUrl: String, timeoutMs: Int = 12_000): List<Model> {
        val response = request(baseUrl, "/api/tags", "GET", null, timeoutMs)
        val models = response.optJSONArray("models") ?: JSONArray()
        return (0 until models.length()).mapNotNull { index ->
            val item = models.optJSONObject(index) ?: return@mapNotNull null
            val name = item.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Model(name, item.optLong("size", 0L), item.optString("modified_at"))
        }
    }

    fun chat(
        baseUrl: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        history: List<Pair<String, String>> = emptyList(),
        timeoutMs: Int = 180_000
    ): Reply {
        require(model.isNotBlank()) { "Choose an installed Ollama model first." }
        require(userPrompt.isNotBlank()) { "The message cannot be empty." }

        val messages = JSONArray()
        if (systemPrompt.isNotBlank()) {
            messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
        }
        history.takeLast(12).forEach { (role, content) ->
            val safeRole = if (role == "assistant") "assistant" else "user"
            if (content.isNotBlank()) messages.put(JSONObject().put("role", safeRole).put("content", content.take(6000)))
        }
        messages.put(JSONObject().put("role", "user").put("content", userPrompt.take(12_000)))

        val payload = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("stream", false)
            .put("options", JSONObject().put("temperature", 0.4).put("num_ctx", 4096))

        val response = request(baseUrl, "/api/chat", "POST", payload, timeoutMs)
        val message = response.optJSONObject("message")
        val text = message?.optString("content")?.trim().orEmpty()
        if (text.isBlank()) throw IllegalStateException("Ollama returned an empty response.")
        return Reply(
            text = text,
            model = response.optString("model", model),
            promptTokens = response.optInt("prompt_eval_count", 0),
            responseTokens = response.optInt("eval_count", 0),
            totalDurationNanos = response.optLong("total_duration", 0L)
        )
    }

    fun health(baseUrl: String, timeoutMs: Int = 8_000): Boolean {
        val connection = open(baseUrl, "/", "GET", timeoutMs)
        return try {
            connection.responseCode in 200..299
        } finally {
            connection.disconnect()
        }
    }

    private fun request(
        baseUrl: String,
        path: String,
        method: String,
        payload: JSONObject?,
        timeoutMs: Int
    ): JSONObject {
        val connection = open(baseUrl, path, method, timeoutMs)
        try {
            if (payload != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IllegalStateException("Ollama returned HTTP $status: ${body.take(400)}")
            }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun open(baseUrl: String, path: String, method: String, timeoutMs: Int): HttpURLConnection {
        val normalized = baseUrl.trim().trimEnd('/')
        val url = URL(normalized + path)
        require(url.protocol == "https" || (url.protocol == "http" && isPrivateHost(url.host))) {
            "Use HTTPS, or HTTP only for a private LAN Ollama server."
        }
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = minOf(timeoutMs, 30_000)
            readTimeout = timeoutMs
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun isPrivateHost(host: String): Boolean {
        val value = host.lowercase()
        return value == "localhost" || value == "127.0.0.1" || value.startsWith("192.168.") ||
            value.startsWith("10.") || value.startsWith("172.16.") || value.endsWith(".local")
    }
}
