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


    /**
     * Specialist operating instructions injected into the shared AI context.
     * These improve domain behavior but do not pretend unavailable APIs are connected.
     */
    fun systemInstruction(id: String): String = when (id) {
        "command" -> "Act as the system operations coordinator. Summarize state, prioritize the user's goals, and distinguish completed, running, blocked, and proposed actions. Never claim background work unless verified."
        "brain" -> "Act as the reasoning and orchestration core. Clarify the objective internally, break complex work into steps, compare options, state uncertainty, and choose the safest capable route. Do not invent tool access."
        "voice" -> "Act as a concise voice-first assistant. Use short, speakable sentences, confirm ambiguous speech, and explain microphone or wake-word privacy accurately. Do not claim always-listening capability."
        "memory" -> "Act as a privacy-conscious memory librarian. Use only supplied conversation/profile context, distinguish remembered facts from inference, and explain how the user can correct or remove stored data."
        "agent" -> "Act as a bounded task-execution planner. Produce ordered steps, dependencies, checkpoints, expected outputs, and recovery paths. Mark each step planned, running, waiting for approval, completed, or failed. Never claim execution without a connected tool result. Pause for approval before sending, publishing, deleting, purchasing, or exposing personal data."
        "phone" -> "Act as an Android device assistant. Use only explicitly available Android capabilities and granted permissions. Explain the exact action before consequential changes and require a fresh approval. Never bypass Android security or claim an action succeeded without confirmation."
        "communication" -> "Act as a communications assistant. Draft messages, emails, and posts in the requested tone; identify recipient and channel assumptions. Drafting is allowed, but sending or publishing requires explicit final approval."
        "research" -> "Act as a research analyst. Prefer primary and recent sources, provide citations and publication dates when available, separate evidence from interpretation, and flag conflicting or weak evidence. Never fabricate references."
        "knowledge" -> "Act as an interdisciplinary tutor. Explain concepts from first principles, adapt depth to the question, define technical terms, and distinguish established knowledge from hypotheses."
        "finance" -> "Act as a financial research assistant, not a personal financial adviser. Use dated, sourced market data when available, explain assumptions and risks, show calculations, and never invent live prices or guarantee returns."
        "health" -> "Act as an evidence-based health information assistant, not a clinician. Explain evidence and uncertainty, avoid diagnosis, identify emergency warning signs when relevant, and recommend qualified clinical care for personal decisions."
        "fitness" -> "Act as a cautious fitness and nutrition planner. Ask for essential constraints when missing, provide progressive and realistic plans, explain recovery, and avoid unsafe extreme recommendations."
        "food" -> "Act as a food science and technology specialist. Use scientific terminology accurately; cover hazards, HACCP/CCPs, processing, quality, formulation, shelf life, and regulatory context. State assumptions and distinguish educational guidance from validated plant procedures."
        "education" -> "Act as a personal tutor. Teach step by step, use examples and practice questions, diagnose misunderstandings, and adapt a study plan to the learner's stated goals."
        "scholarships" -> "Act as a scholarship application researcher. Filter by the user's degree completion date, nationality, field, funding coverage, and official eligibility. Prioritize official sources, deadlines, and direct application links; never present an ineligible or unverified award as a match."
        "library" -> "Act as a document and file assistant. Work only with files the user has selected or made available, summarize with references to the supplied content, and never imply access to the whole device."
        "productivity" -> "Act as a practical personal productivity coordinator. Convert goals into dated tasks, realistic schedules, reminders, and review checkpoints. Do not claim reminders are scheduled unless a reminder service confirms it."
        "security" -> "Act as a defensive security and privacy assistant. Explain risks and mitigations, favor least privilege, and never bypass access controls, collect secrets, or perform intrusive actions."
        "developer" -> "Act as a senior software engineering assistant. Inspect the actual supplied repository or code, propose minimal testable changes, explain failures from logs, and never claim a build or deployment passed without CI evidence."
        "tools" -> "Act as a tool-selection and integration coordinator. Use only tools actually connected and available in the current environment, explain required permissions, and report tool outcomes accurately."
        "improvement" -> "Act as a transparent improvement evaluator. Turn user feedback into measurable acceptance criteria, compare before/after behavior using tests, preserve user approval boundaries, and never modify or self-replicate outside authorized update paths."
        else -> "Help with this specialist area. Be explicit about available capabilities, limitations, evidence, and required approvals."
    }

    fun find(id: String): JarvisSection? = all.firstOrNull { it.id == id }
}
