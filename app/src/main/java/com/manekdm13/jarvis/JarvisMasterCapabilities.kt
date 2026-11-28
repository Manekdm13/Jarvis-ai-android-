package com.manekdm13.jarvis

/**
 * Master capability map for JARVIS.
 *
 * This is a registry/catalog, not a claim that every capability is already implemented.
 * A capability becomes AVAILABLE only after its implementation and acceptance tests exist.
 */
internal enum class CapabilityState { PLANNED, FOUNDATION, AVAILABLE, BLOCKED }

internal data class JarvisCapability(
    val id: Int,
    val section: String,
    val features: List<String>,
    val state: CapabilityState = CapabilityState.PLANNED,
    val implementationNotes: String
)

internal object JarvisMasterCapabilities {
    val all: List<JarvisCapability> = listOf(
        JarvisCapability(1, "Independent JARVIS brain", listOf("JARVIS-owned orchestration", "goal planning", "step sequencing", "provider abstraction", "task state", "decision policies"), CapabilityState.FOUNDATION, "Cloud brain endpoint exists; durable orchestration and broad tool execution remain."),
        JarvisCapability(2, "Continuous self-learning", listOf("scheduled web discovery", "research feeds", "source extraction", "fact checking", "deduplication", "knowledge indexing", "knowledge refresh", "learning reports"), CapabilityState.FOUNDATION, "Initial D1 learning pipeline and schedule are in development; broad source coverage and corroboration remain."),
        JarvisCapability(3, "Skill acquisition and improvement", listOf("discover tools", "read documentation", "sandbox tests", "capability registration", "regression tests", "rollback"), implementationNotes = "Requires isolated execution and signed/approved capability installation."),
        JarvisCapability(4, "Autonomous task execution", listOf("break goals into tasks", "execute permitted steps", "track progress", "retry alternatives", "verify postconditions", "recover failures"), CapabilityState.FOUNDATION, "Bounded planning/review exists; general verified tool execution remains."),
        JarvisCapability(5, "Android phone control", listOf("launch apps", "visible UI actions", "notifications with consent", "files via SAF", "camera/gallery grants", "settings shortcuts", "per-action approval"), CapabilityState.FOUNDATION, "Limited accessibility and intent actions exist; Android sandbox and app restrictions apply."),
        JarvisCapability(6, "Voice and wake word", listOf("speech input", "natural TTS", "barge-in", "Hey JARVIS wake word", "mic indicator", "battery-aware listening", "privacy controls"), CapabilityState.FOUNDATION, "Speech/TTS foundations exist; always-on wake-word service is not complete."),
        JarvisCapability(7, "Online and offline intelligence", listOf("cloud model routing", "Ollama local model", "offline fallback", "provider health", "model selection", "local inference controls"), implementationNotes = "Ollama adapter is provided; local model hosting/device performance and fallback integration need testing."),
        JarvisCapability(8, "Persistent memory", listOf("conversation memory", "personal preferences", "task history", "knowledge memory", "skill memory", "encrypted storage", "cloud backup", "retrieval"), CapabilityState.FOUNDATION, "Encrypted local preference memory and D1 knowledge schema foundations exist; unified durable memory is incomplete."),
        JarvisCapability(9, "Cloud infrastructure", listOf("Cloudflare Worker", "D1 knowledge database", "scheduled jobs", "Supabase integration", "secure secrets", "health monitoring", "provider failover"), CapabilityState.FOUNDATION, "Worker and D1 foundations exist; live deployment and full service integration remain."),
        JarvisCapability(10, "Research and general knowledge", listOf("web search", "academic research", "citations", "source comparison", "science", "finance", "food science", "space", "marine science", "math"), implementationNotes = "Requires a complete search/source connector and citation pipeline."),
        JarvisCapability(11, "Browser and computer use", listOf("navigate websites", "read pages", "fill forms", "download documents", "browser automation", "verify results", "stop before sensitive final actions"), CapabilityState.FOUNDATION, "Browser-agent path exists but needs integrated tool orchestration and robust verification."),
        JarvisCapability(12, "Personal productivity", listOf("reminders", "calendar", "notes", "task lists", "email assistance", "scheduling", "daily briefings"), implementationNotes = "Requires durable task/calendar/email integrations and user-authorized connectors."),
        JarvisCapability(13, "Scholarships and education", listOf("eligibility checking", "official deadline tracking", "application documents", "university research", "reference planning", "submission checklist"), implementationNotes = "Needs dedicated verified scholarship research and document workflows."),
        JarvisCapability(14, "Career and job assistance", listOf("CV tailoring", "cover letters", "job discovery", "eligibility checks", "application tracking", "interview preparation"), implementationNotes = "Needs job-source integrations and persistent application tracking."),
        JarvisCapability(15, "Legitimate online earning", listOf("opportunity discovery", "Pakistan eligibility", "payout verification", "fee checks", "scam screening", "net hourly estimates", "earnings ledger"), CapabilityState.FOUNDATION, "Opportunity model and ledger foundations exist; live verified opportunity research remains."),
        JarvisCapability(16, "Security and privacy", listOf("encrypted secrets", "least privilege", "permission boundaries", "approval gates", "audit logs", "prompt-injection defense", "data minimization"), CapabilityState.FOUNDATION, "Action policy and encrypted local memory foundations exist; full security review remains."),
        JarvisCapability(17, "Futuristic user interface", listOf("dark navy theme", "cyan/teal accents", "status indicators", "chat interface", "collapsible settings", "copy reply", "task dashboard", "learning dashboard"), CapabilityState.FOUNDATION, "Existing Compose UI has partial elements; master visual redesign remains."),
        JarvisCapability(18, "Updates and maintenance", listOf("version checks", "in-app update flow", "signed update verification", "safe rollback", "release channels", "backup before migration"), CapabilityState.FOUNDATION, "Updater foundation exists; complete signed release/update pipeline remains."),
        JarvisCapability(19, "Diagnostics and self-repair", listOf("health checks", "error classification", "logs", "repair proposals", "sandboxed fixes", "regression tests", "rollback"), CapabilityState.FOUNDATION, "Review and one repair pass exist at reasoning level; code-level self-repair is not implemented."),
        JarvisCapability(20, "Extensibility and integrations", listOf("tool registry", "plugin adapters", "GitHub", "Notion", "Supabase", "Airtable", "messaging integrations", "capability permissions"), implementationNotes = "Requires explicit connector/tool adapters and per-capability authorization."),
        JarvisCapability(21, "Continuous intelligence engine", listOf("never-stop learning cycle", "topic discovery", "source provenance", "confidence tracking", "contradiction detection", "staleness detection", "knowledge graph", "offline knowledge cache"), CapabilityState.FOUNDATION, "Overlaps the self-learning section by design: this is the system-wide always-on knowledge lifecycle.")
    )

    fun byId(id: Int): JarvisCapability? = all.firstOrNull { it.id == id }
    fun summary(): Map<CapabilityState, Int> = all.groupingBy { it.state }.eachCount()
}
