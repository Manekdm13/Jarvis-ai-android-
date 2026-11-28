package com.manekdm13.jarvis

/** Consequential intents pause at a visible, one-time approval checkpoint. */
internal object JarvisTaskApprovalPolicy {
    private val sensitiveIntent = Regex(
        """\b(send|submit|publish|post|delete|remove|purchase|buy|pay|transfer|call|message|email|whatsapp|modify|change password|change security|share private)\b""",
        RegexOption.IGNORE_CASE
    )

    fun requiresApproval(taskTitle: String): Boolean = sensitiveIntent.containsMatchIn(taskTitle)
}
