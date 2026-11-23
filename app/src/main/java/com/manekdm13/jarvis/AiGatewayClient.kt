package com.manekdm13.jarvis

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal object AiGatewayClient {
    /** Uses the same Android HTTP stack as chat, but performs a safe unauthenticated GET. */
    fun checkHealth(baseUrl: String): String {
        val cleanBase = baseUrl.trim().trimEnd('/')
        require(cleanBase.startsWith("https://")) { "Enter a valid HTTPS Worker URL in AI gateway URL." }
        val endpoint = if (cleanBase.endsWith("/health")) cleanBase else "$cleanBase/health"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) return "Worker health check returned HTTP $status."
            val json = JSONObject(body)
            if (!json.optBoolean("ok", false)) return "Worker responded, but health status is not OK."
            return "Android app connected successfully. Gateway is online; authenticationConfigured=${json.optJSONObject("providers")?.let { "provider flags available" } ?: "unknown"}."
        } finally {
            connection.disconnect()
        }
    }

    fun reply(baseUrl: String, token: String, message: String, history: String): String {
        require(token.isNotBlank()) { "Enter your private gateway access token in JARVIS settings." }
        val cleanBase = baseUrl.trim().trimEnd('/')
        require(cleanBase.startsWith("https://")) { "Enter a valid HTTPS Worker URL in AI gateway URL." }
        val endpoint = if (cleanBase.endsWith("/v1/chat")) cleanBase else "$cleanBase/v1/chat"
        val payload = JSONObject()
            .put("message", message)
            .put("history", history.takeLast(12_000))

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
                val messageText = response?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: "AI gateway returned HTTP $status."
                val providerDetails = response?.optJSONArray("details")?.let { details ->
                    (0 until details.length()).mapNotNull { index ->
                        details.optString(index).takeIf { it.isNotBlank() }
                    }.distinct().joinToString("; ")
                }?.takeIf { it.isNotBlank() }
                throw IllegalStateException(
                    if (providerDetails != null) "$messageText ($providerDetails)" else messageText
                )
            }
            return response?.optString("reply")?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("The AI provider returned an empty reply.")
        } finally {
            connection.disconnect()
        }
    }
}
