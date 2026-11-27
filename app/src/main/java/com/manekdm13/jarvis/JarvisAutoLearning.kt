package com.manekdm13.jarvis

/**
 * Conservative, local-only preference extraction.
 *
 * It learns only when the user phrases a statement as a durable instruction or
 * preference. Ordinary conversation is never silently promoted into memory.
 */
internal object JarvisAutoLearning {
    private val durableMarkers = listOf(
        "from now on ",
        "i prefer ",
        "i want you to ",
        "i want jarvis to ",
        "please always ",
        "please never ",
        "always ",
        "never ",
        "when i say "
    )

    private val sensitiveMarkers = listOf(
        "password", "passcode", "one-time code", "otp", "api key",
        "access token", "private key", "secret key", "credit card",
        "bank account", "recovery code"
    )

    fun extractExplicitLesson(message: String): String? {
        val clean = message.trim().replace(Regex("\\s+"), " ")
        if (clean.length !in 12..500) return null
        val lower = clean.lowercase()
        if (sensitiveMarkers.any { it in lower }) return null
        if (Regex("""(?i)\\b\\d{6,}\\b""").containsMatchIn(clean)) return null

        val marker = durableMarkers.firstOrNull { lower.startsWith(it) } ?: return null
        val preference = clean.substring(marker.length).trim().trimEnd('.', '!', '?')
        if (preference.length < 8) return null
        if (preference.equals("that", ignoreCase = true)) return null
        return "User preference: $preference".take(500)
    }
}
