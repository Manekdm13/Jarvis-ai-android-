package com.manekdm13.jarvis

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal object AiGatewayClient {
    fun reply(baseUrl: String, token: String, message: String, history: String, mode: String): String {
        require(token.isNotBlank()) { "Enter your private gateway access token in JARVIS settings." }
        val cleanBase = baseUrl.trim().trimEnd('/')
        require(cleanBase.startsWith("https://")) { "Enter a valid HTTPS Worker URL in AI gateway URL." }
        val endpoint = if (cleanBase.endsWith("/v1/chat")) cleanBase else "$cleanBase/v1/chat"
        val payload = JSONObject()
            .put("message", message)
            .put("history", history.takeLast(12_000))
            .put("mode", mode)

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val response = runCatching { JSONObject(responseText) }.getOrNull()
            if (status !in 200..299) {
                throw IllegalStateException(response?.optString("error")
                    ?.takeIf { it.isNotBlank() } ?: "AI gateway returned HTTP $status.")
            }
            return response?.optString("reply")?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("The AI provider returned an empty reply.")
        } finally {
            connection.disconnect()
        }
    }
}
