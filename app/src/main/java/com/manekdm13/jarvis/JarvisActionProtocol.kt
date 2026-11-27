package com.manekdm13.jarvis

import org.json.JSONObject

internal data class JarvisPhoneActionRequest(
    val command: String,
    val reason: String
)

/**
 * Parses a narrowly scoped action proposal from the model. This parser does not
 * execute anything: the command must still pass the local allowlist and the
 * user must approve it in the Android app.
 */
internal object JarvisActionProtocol {
    fun parse(reply: String): JarvisPhoneActionRequest? {
        val normalized = reply.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        if (!normalized.startsWith("{") || !normalized.endsWith("}")) return null
        return runCatching {
            val json = JSONObject(normalized)
            if (json.optString("type") != "phone_action") return null
            val command = json.optString("command").trim()
            if (command.isBlank() || command.length > 2_000) return null
            JarvisPhoneActionRequest(
                command = command,
                reason = json.optString("reason").trim().take(500)
            )
        }.getOrNull()
    }
}
