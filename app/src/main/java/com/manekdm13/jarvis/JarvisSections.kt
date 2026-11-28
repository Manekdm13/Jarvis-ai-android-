package com.manekdm13.jarvis

/**
 * JARVIS 3.0's 21-section product map.
 *
 * A catalog entry describes a user-facing specialist area; it does not claim
 * that every integration or autonomous action is already implemented.
 * All sections share the central assistant conversation and existing
 * permission/approval boundaries.
 */
data class JarvisSection(
    val id: String,
    val name: String,
    val description: String,
    val status: SectionStatus
)

enum class SectionStatus { FOUNDATION, PLANNED }

object JarvisSections {
    val all = listOf(
        JarvisSection("command", "Command Centre", "System overview, status, quick actions and activity.", SectionStatus.FOUNDATION),
        JarvisSection("brain", "AI Brain", "Central reasoning, task classification and provider orchestration.", SectionStatus.FOUNDATION),
        JarvisSection("voice", "Voice Assistant", "Speech input, spoken replies and future wake-word controls.", SectionStatus.FOUNDATION),
        JarvisSection("memory", "Personal Memory", "Searchable conversation history and opt-in profile memory.", SectionStatus.FOUNDATION),
        JarvisSection("agent", "Autonomous Agent", "Plan multi-step work, track progress and recover from safe failures.", SectionStatus.PLANNED),
        JarvisSection("phone", "Phone Control", "Permission-gated Android actions with approval before consequential steps.", SectionStatus.FOUNDATION),
        JarvisSection("communication", "Communication", "Assist with messages, email and social workflows; confirm before sending.", SectionStatus.PLANNED),
        JarvisSection("research", "Research Lab", "Web and scientific research with citations, dates and evidence quality.", SectionStatus.FOUNDATION),
        JarvisSection("knowledge", "Knowledge Centre", "General knowledge and specialist explanations across disciplines.", SectionStatus.PLANNED),
        JarvisSection("finance", "Finance & Markets", "Market education, company analysis and sourced live financial data.", SectionStatus.PLANNED),
        JarvisSection("health", "Health & Medicine", "Evidence-based health information with appropriate clinical boundaries.", SectionStatus.PLANNED),
        JarvisSection("fitness", "Fitness & Nutrition", "Training, recovery and nutrition planning with user-stated constraints.", SectionStatus.PLANNED),
        JarvisSection("food", "Food Science", "Food safety, quality, processing, formulation and shelf-life support.", SectionStatus.PLANNED),
        JarvisSection("education", "Education", "Learning plans, tutoring, explanations and study support.", SectionStatus.PLANNED),
        JarvisSection("scholarships", "Scholarship Manager", "Eligibility-filtered opportunities, deadlines and application documents.", SectionStatus.PLANNED),
        JarvisSection("library", "Files & Gallery", "User-selected files, gallery indexing and document workflows.", SectionStatus.FOUNDATION),
        JarvisSection("productivity", "Productivity", "Tasks, reminders, schedules and personal workflows.", SectionStatus.PLANNED),
        JarvisSection("security", "Security Centre", "Permission visibility, privacy controls and authorized security guidance.", SectionStatus.PLANNED),
        JarvisSection("developer", "Developer Studio", "Code generation, repository inspection, debugging and tests.", SectionStatus.PLANNED),
        JarvisSection("tools", "Internet & Tools", "Approved external tools, calculations and connected services.", SectionStatus.PLANNED),
        JarvisSection("improvement", "Self-Improvement", "User-approved feedback, evaluations and transparent capability updates.", SectionStatus.PLANNED)
    )


    /** Section-specific starter actions; each opens the shared assistant with this specialist context. */
    fun actions(id: String): List<String> = when (id) {
        "command" -> listOf("Show my system status", "Summarize today's activity", "What can I do right now?")
        "brain" -> listOf("Explain how you choose an AI provider", "Run a reasoning task", "Check connection status")
        "voice" -> listOf("Start a voice conversation", "Help me configure voice", "Explain wake-word privacy")
        "memory" -> listOf("Search my saved conversation", "Review my profile memory", "Explain how to clear memory")
        "agent" -> listOf("Plan a multi-step task", "Review my active tasks", "Identify safe recovery steps")
        "phone" -> listOf("Open Android settings", "Check available phone actions", "Explain current permissions")
        "communication" -> listOf("Draft a WhatsApp message", "Draft an email", "Help organize a social post")
        "research" -> listOf("Research a topic with sources", "Find recent scientific papers", "Compare evidence quality")
        "knowledge" -> listOf("Explain a complex topic", "Teach me something new", "Build a learning roadmap")
        "finance" -> listOf("Analyze a company", "Explain a market concept", "Build a risk checklist")
        "health" -> listOf("Explain a health topic with evidence", "Prepare questions for a clinician", "Summarize medical research")
        "fitness" -> listOf("Create a workout plan", "Plan balanced meals", "Review recovery basics")
        "food" -> listOf("Analyze a food safety hazard", "Explain a food processing method", "Help with food quality testing")
        "education" -> listOf("Teach me a subject", "Create a study schedule", "Quiz me on a topic")
        "scholarships" -> listOf("Find eligible fully funded scholarships", "Build my application checklist", "Review scholarship deadlines")
        "library" -> listOf("Help me find a file", "Summarize a selected document", "Explain gallery access")
        "productivity" -> listOf("Create a task plan", "Organize my week", "Help set a reminder")
        "security" -> listOf("Review app permissions", "Explain privacy controls", "Check a security concern")
        "developer" -> listOf("Review code", "Help debug an error", "Plan a software feature")
        "tools" -> listOf("Find an approved tool for a task", "Run a calculation", "Explain connected services")
        "improvement" -> listOf("Review my feedback", "Suggest a measurable improvement", "Explain how updates are evaluated")
        else -> listOf("Help me with this section", "Explain available capabilities", "Plan my next step")
    }

    fun find(id: String): JarvisSection? = all.firstOrNull { it.id == id }
}
