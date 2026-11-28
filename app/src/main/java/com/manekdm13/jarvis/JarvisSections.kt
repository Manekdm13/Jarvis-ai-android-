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

    fun find(id: String): JarvisSection? = all.firstOrNull { it.id == id }
}
