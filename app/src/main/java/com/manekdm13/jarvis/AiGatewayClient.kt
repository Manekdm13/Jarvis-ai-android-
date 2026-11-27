package com.manekdm13.jarvis

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal object AiGatewayClient {
    /** Uses the same Android HTTP stack as chat, but performs a safe unauthenticated GET. */
    fun checkHealth(baseUrl: String, token: String): String {
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
            val configured = json.optBoolean("authenticationConfigured", false)
            val serverFingerprint = json.optString("appTokenFingerprint").takeIf { it.matches(Regex("[a-f0-9]{16}")) }
            val normalizedToken = token.replace(Regex("[\\s\\uFEFF]+"), "").removeSurrounding("\"").removeSurrounding("'")
            val localFingerprint = if (normalizedToken.isNotEmpty()) {
                MessageDigest.getInstance("SHA-256")
                    .digest(normalizedToken.toByteArray(Charsets.UTF_8))
                    .take(8).joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
            } else null
            val authStatus = when {
                !configured -> "Worker app-token secret is missing or too short."
                localFingerprint == null -> "Enter the saved app token to verify authentication."
                serverFingerprint == null -> "Worker is older than the token-diagnostic update; fingerprint unavailable."
                localFingerprint == serverFingerprint -> "Saved app token MATCHES the Worker."
                else -> "Saved app token DOES NOT MATCH the Worker. The 64-character token values must be identical."
            }
            val providers = json.optJSONObject("providers")
            val aiNames = listOf(
                "Gemini" to "gemini",
                "Groq" to "groq",
                "OpenRouter" to "openrouter",
                "Cloudflare Workers AI" to "workers_ai"
            ).filter { (_, key) -> providers?.optBoolean(key, false) == true }
                .map { it.first }
            val researchNames = listOf(
                "Cloudflare Browser" to "cloudflare_browser",
                "TinyFish" to "tinyfish"
            ).filter { (_, key) -> providers?.optBoolean(key, false) == true }
                .map { it.first }
            val capabilityStatus = buildString {
                append("AI providers: ")
                append(aiNames.takeIf { it.isNotEmpty() }?.joinToString() ?: "none configured")
                append(". Web research: ")
                append(researchNames.takeIf { it.isNotEmpty() }?.joinToString() ?: "none configured")
                append(".")
            }
            return "Android reached the gateway. $authStatus $capabilityStatus"
        } finally {
            connection.disconnect()
        }
    }

    fun reply(baseUrl: String, token: String, message: String, history: String): String {
        require(token.isNotBlank()) { "Enter your private gateway access token in JARVIS settings." }
        val cleanToken = token.replace(Regex("[\\s\\uFEFF]+"), "").removeSurrounding("\"").removeSurrounding("'")
        val cleanBase = baseUrl.trim().trimEnd('/')
        require(cleanBase.startsWith("https://")) { "Enter a valid HTTPS Worker URL in AI gateway URL." }
        val endpoint = if (cleanBase.endsWith("/v1/chat")) cleanBase else "$cleanBase/v1/chat"
        val payload = JSONObject()
            .put("message", message)
            .put("history", history.takeLast(12_000))

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $cleanToken")
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

    /** Runs a user-requested browser task through the authenticated Worker; TinyFish credentials stay server-side. */
    fun runWebAgent(baseUrl: String, token: String, targetUrl: String, goal: String): String {
        require(token.isNotBlank()) { "Enter your private gateway access token in JARVIS settings." }
        val cleanToken = token.replace(Regex("[\\s\\uFEFF]+"), "").removeSurrounding("\"").removeSurrounding("'")
        val cleanBase = baseUrl.trim().trimEnd('/')
        require(cleanBase.startsWith("https://")) { "Enter a valid HTTPS Worker URL in AI gateway URL." }
        require(targetUrl.trim().startsWith("https://") || targetUrl.trim().startsWith("http://")) {
            "Enter a website address beginning with https:// or http://."
        }
        require(goal.isNotBlank()) { "Describe the browser task first." }
        val endpoint = if (cleanBase.endsWith("/v1/web-agent")) cleanBase else "$cleanBase/v1/web-agent"
        val payload = JSONObject().put("url", targetUrl.trim()).put("goal", goal.trim())
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 180_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $cleanToken")
        }
        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val response = runCatching { JSONObject(responseText) }.getOrNull()
            if (status !in 200..299) {
                val message = response?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: "TinyFish web agent returned HTTP $status."
                val details = response?.optString("details")?.takeIf { it.isNotBlank() }
                throw IllegalStateException(if (details != null) "$message $details" else message)
            }
            val result = response?.opt("result")
            return when (result) {
                is org.json.JSONObject -> result.toString(2)
                is org.json.JSONArray -> result.toString(2)
                null -> response?.toString(2) ?: "TinyFish completed the task but returned no details."
                else -> result.toString()
            }
        } finally {
            connection.disconnect()
        }
    }

}
