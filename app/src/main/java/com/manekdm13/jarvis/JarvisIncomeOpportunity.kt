package com.manekdm13.jarvis

/**
 * Normalized, auditable opportunity model for JARVIS's side-income agent.
 * Values are evidence supplied by a platform/account check; the model never
 * assumes Pakistan eligibility or payout support from a user's available methods.
 */
internal enum class JarvisIncomeCategory {
    SURVEY, PAID_RESEARCH, APP_WEBSITE_TESTING, AI_DATA_TASK, TRANSCRIPTION,
    WEB_RESEARCH, DATA_ENTRY, REMOTE_CONTRACT, TUTORING, DIGITAL_PRODUCT,
    CREATOR_MONETIZATION, AFFILIATE, E_COMMERCE, OTHER
}

internal enum class JarvisPayoutRoute {
    JAZZCASH, EASYPAISA, PAYONEER, PAKISTANI_BANK
}

internal enum class JarvisOpportunityStatus {
    DISCOVERED, ELIGIBILITY_UNVERIFIED, ELIGIBLE, QUALIFICATION_REQUIRED,
    ACTIVE, PAUSED, COMPLETED, REJECTED, PAID
}

internal enum class JarvisAutomationPermission {
    EXPLICITLY_ALLOWED, LIMITED_OR_REVIEW_REQUIRED, PROHIBITED, UNKNOWN
}

internal data class JarvisIncomeOpportunity(
    val id: String,
    val platform: String,
    val title: String,
    val officialUrl: String,
    val category: JarvisIncomeCategory,
    val status: JarvisOpportunityStatus,
    val pakistanEligibilityVerified: Boolean,
    val automationPermission: JarvisAutomationPermission,
    val payoutRoutesVerified: Set<JarvisPayoutRoute>,
    val grossRewardUsd: Double?,
    val estimatedMinutes: Int?,
    val feesUsd: Double = 0.0,
    val otherCostsUsd: Double = 0.0,
    val withdrawalMinimumUsd: Double? = null,
    val evidenceUrl: String? = null,
    val lastVerifiedEpochMillis: Long? = null,
    val requiresPersonalOpinion: Boolean = false,
    val requiresIdentityOrPhysicalPresence: Boolean = false
) {
    fun validate(): String? {
        if (id.isBlank() || platform.isBlank() || title.isBlank()) return "Opportunity identity is incomplete."
        if (!officialUrl.startsWith("https://")) return "An official HTTPS URL is required."
        if (grossRewardUsd != null && (!grossRewardUsd.isFinite() || grossRewardUsd < 0.0)) {
            return "Reward must be finite and cannot be negative."
        }
        if (estimatedMinutes != null && estimatedMinutes <= 0) return "Estimated time must be positive."
        if (!feesUsd.isFinite() || !otherCostsUsd.isFinite() || feesUsd < 0.0 || otherCostsUsd < 0.0) {
            return "Costs must be finite and cannot be negative."
        }
        if (withdrawalMinimumUsd != null &&
            (!withdrawalMinimumUsd.isFinite() || withdrawalMinimumUsd < 0.0)) {
            return "Withdrawal minimum must be finite and cannot be negative."
        }
        return null
    }

    /** Estimated net USD/hour; null when reward or duration is not evidenced. */
    fun estimatedNetHourlyUsd(): Double? {
        val reward = grossRewardUsd ?: return null
        val minutes = estimatedMinutes?.takeIf { it > 0 } ?: return null
        return ((reward - feesUsd - otherCostsUsd) / minutes) * 60.0
    }

    /** Discovery/ranking can be autonomous; task submission may not be. */
    fun canPerformTaskAutonomously(): Boolean =
        pakistanEligibilityVerified &&
            automationPermission == JarvisAutomationPermission.EXPLICITLY_ALLOWED &&
            !requiresPersonalOpinion &&
            !requiresIdentityOrPhysicalPresence &&
            status in setOf(JarvisOpportunityStatus.ELIGIBLE, JarvisOpportunityStatus.ACTIVE) &&
            payoutRoutesVerified.isNotEmpty()

    fun isReadyToRecommend(): Boolean =
        validate() == null &&
            pakistanEligibilityVerified &&
            status !in setOf(JarvisOpportunityStatus.REJECTED, JarvisOpportunityStatus.PAUSED)
}

/** Conservative ordering: verified eligibility and payout before estimated return. */
internal object JarvisIncomeOpportunityRanking {
    fun rank(items: List<JarvisIncomeOpportunity>): List<JarvisIncomeOpportunity> =
        items.sortedWith(
            compareByDescending<JarvisIncomeOpportunity> { it.isReadyToRecommend() }
                .thenByDescending { it.payoutRoutesVerified.isNotEmpty() }
                .thenByDescending { it.estimatedNetHourlyUsd() ?: Double.NEGATIVE_INFINITY }
                .thenBy { it.platform.lowercase() }
        )
}
