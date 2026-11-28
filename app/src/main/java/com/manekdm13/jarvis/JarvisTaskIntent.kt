package com.manekdm13.jarvis

/**
 * Recognizes requests that describe an objective rather than a conversational question.
 * Deliberately conservative: plain questions and phone commands stay in the chat/action path.
 */
internal object JarvisTaskIntent {
    private val objectivePhrases = listOf(
        "research ", "find me ", "find the ", "look for ", "search for ",
        "compare ", "prepare ", "create ", "write ", "build ", "solve ",
        "analyze ", "analyse ", "organize ", "organise ", "plan ",
        "complete ", "handle ", "investigate ", "work on ", "figure out ",
        "draft ", "summarize ", "summarise ", "evaluate ", "identify ",
        "collect ", "compile ", "calculate ", "design "
    )

    private val explicitPrefixes = listOf(
        "do this:", "do this ", "handle this:", "handle this ",
        "task:", "objective:", "take care of ", "please handle ",
        "i want you to ", "i need you to ", "please handle ", "your task is to ", "get this done:", "take care of "
    )

    fun isAutonomousTaskRequest(raw: String): Boolean {
        val text = raw.trim().lowercase()
        if (text.length < 12) return false
        if (text in setOf("what is research", "how do i research", "what is a plan")) return false
        if (explicitPrefixes.any(text::startsWith)) return true
        return objectivePhrases.any { phrase ->
            text.startsWith(phrase) || text.contains("please $phrase") ||
                text.contains("can you $phrase") || text.contains("could you $phrase")
        }
    }
}
