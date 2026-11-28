package com.manekdm13.jarvis

import org.json.JSONObject

internal data class JarvisPhoneActionRequest(
    val command: String,
    val reason: String
)

/** Parses one narrowly scoped phone-action proposal from the model. */
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
            JarvisPhoneActionRequest(command, json.optString("reason").trim().take(500))
        }.getOrNull()
    }
}

/**
 * Autonomous-by-default policy for routine, reversible actions.
 * Anything that can expose private content, enter text, click an arbitrary
 * control, send/submit, purchase, delete, call, or alter security stays gated.
 */
internal object JarvisActionPolicy {
    fun canAutoExecute(rawCommand: String): Boolean {
        val command = rawCommand.trim().lowercase()
        if (command in setOf("back", "go back", "home", "go home", "scroll up", "scroll down")) return true
        if (command in setOf("settings", "open settings", "wifi", "wi-fi", "bluetooth",
                "camera", "open camera", "alarm", "show alarms")) return true
        if (command.startsWith("open ") && command.removePrefix("open ").isNotBlank()) return true
        if ((command.startsWith("search ") || command.startsWith("google ")) &&
            command.substringAfter(' ').isNotBlank()) return true
        if ((command.startsWith("navigate to ") || command.startsWith("map ")) &&
            command.substringAfter(" to ", command.removePrefix("map ")).isNotBlank()) return true
        return false
    }
}
