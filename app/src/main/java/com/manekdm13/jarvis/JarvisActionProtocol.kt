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

/** Risk classification is deliberately independent from the model's claimed intent. */
internal enum class JarvisActionRisk {
    ROUTINE_REVERSIBLE,
    EXTERNAL_NAVIGATION,
    PRIVATE_DATA_ACCESS,
    USER_INPUT,
    EXTERNAL_TRANSMISSION,
    IRREVERSIBLE_OR_FINANCIAL,
    UNKNOWN
}

/**
 * Local policy gate for proposed phone actions. The model cannot lower an action's
 * risk by describing it as safe. Only narrow, reversible actions are autonomous.
 * Any screen reading, text entry, arbitrary tap, message/share, or unknown command
 * requires a fresh user approval in the foreground.
 */
internal object JarvisActionPolicy {
    fun classify(rawCommand: String): JarvisActionRisk {
        val command = rawCommand.trim().lowercase()
        if (command.isBlank()) return JarvisActionRisk.UNKNOWN

        if (command in setOf("read screen", "summarize screen", "what's on my screen", "what is on my screen")) {
            return JarvisActionRisk.PRIVATE_DATA_ACCESS
        }
        if (command.startsWith("type ") || command.startsWith("tap ") || command.startsWith("click ")) {
            return JarvisActionRisk.USER_INPUT
        }
        if (command.startsWith("send ") || command.startsWith("share ") ||
            command.startsWith("whatsapp ") || command.startsWith("compose sms ") ||
            command.startsWith("text message ") || command.startsWith("email ")) {
            return JarvisActionRisk.EXTERNAL_TRANSMISSION
        }
        if (listOf("delete", "remove", "purchase", "buy ", "pay ", "transfer ",
                "call ", "submit", "post ", "security", "password", "permission").any { command.contains(it) }) {
            return JarvisActionRisk.IRREVERSIBLE_OR_FINANCIAL
        }
        if (command in setOf("back", "go back", "home", "go home", "scroll up", "scroll down")) {
            return JarvisActionRisk.ROUTINE_REVERSIBLE
        }
        if (command in setOf("settings", "open settings", "wifi", "wi-fi", "bluetooth",
                "camera", "open camera", "alarm", "show alarms", "notification settings",
                "open notification settings", "open phone", "open dialer", "dialer")) {
            return JarvisActionRisk.ROUTINE_REVERSIBLE
        }
        if (command.startsWith("open ") && command.removePrefix("open ").isNotBlank()) {
            return JarvisActionRisk.ROUTINE_REVERSIBLE
        }
        if ((command.startsWith("search ") || command.startsWith("google ")) &&
            command.substringAfter(' ').isNotBlank()) {
            return JarvisActionRisk.EXTERNAL_NAVIGATION
        }
        if ((command.startsWith("navigate to ") || command.startsWith("map ")) &&
            command.substringAfter(" to ", command.removePrefix("map ")).isNotBlank()) {
            return JarvisActionRisk.EXTERNAL_NAVIGATION
        }
        return JarvisActionRisk.UNKNOWN
    }

    fun canAutoExecute(rawCommand: String): Boolean = when (classify(rawCommand)) {
        JarvisActionRisk.ROUTINE_REVERSIBLE, JarvisActionRisk.EXTERNAL_NAVIGATION -> true
        JarvisActionRisk.PRIVATE_DATA_ACCESS, JarvisActionRisk.USER_INPUT,
        JarvisActionRisk.EXTERNAL_TRANSMISSION, JarvisActionRisk.IRREVERSIBLE_OR_FINANCIAL,
        JarvisActionRisk.UNKNOWN -> false
    }
}
