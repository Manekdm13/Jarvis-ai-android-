package com.manekdm13.jarvis

import org.junit.Assert.*
import org.junit.Test

class JarvisIncomeOpportunityTest {
    private fun opportunity(
        eligible: Boolean = true,
        routes: Set<JarvisPayoutRoute> = setOf(JarvisPayoutRoute.PAYONEER),
        permission: JarvisAutomationPermission = JarvisAutomationPermission.EXPLICITLY_ALLOWED,
        personalOpinion: Boolean = false
    ) = JarvisIncomeOpportunity(
        id = "task-1",
        platform = "Example",
        title = "Paid data task",
        officialUrl = "https://example.com/task",
        category = JarvisIncomeCategory.AI_DATA_TASK,
        status = JarvisOpportunityStatus.ELIGIBLE,
        pakistanEligibilityVerified = eligible,
        automationPermission = permission,
        payoutRoutesVerified = routes,
        grossRewardUsd = 5.0,
        estimatedMinutes = 30,
        requiresPersonalOpinion = personalOpinion
    )

    @Test fun estimatesNetHourlyReturnAfterCosts() {
        assertEquals(8.0, opportunity().copy(feesUsd = 1.0).estimatedNetHourlyUsd()!!, 0.001)
    }

    @Test fun neverAssumesPayoutSupportFromUserWallet() {
        assertFalse(opportunity(routes = emptySet()).canPerformTaskAutonomously())
    }

    @Test fun countryEligibilityMustBeVerified() {
        assertFalse(opportunity(eligible = false).canPerformTaskAutonomously())
    }

    @Test fun platformMustPermitAutomation() {
        assertFalse(opportunity(permission = JarvisAutomationPermission.UNKNOWN).canPerformTaskAutonomously())
        assertFalse(opportunity(permission = JarvisAutomationPermission.PROHIBITED).canPerformTaskAutonomously())
    }

    @Test fun personalOpinionTasksMustBeHandedToUser() {
        assertFalse(opportunity(personalOpinion = true).canPerformTaskAutonomously())
    }

    @Test fun rankingPutsVerifiedOpportunitiesFirst() {
        val unknown = opportunity(eligible = false).copy(id = "unknown")
        val verified = opportunity().copy(id = "verified")
        assertEquals("verified", JarvisIncomeOpportunityRanking.rank(listOf(unknown, verified)).first().id)
    }

    @Test fun rejectsNonHttpsAndInvalidTime() {
        assertNotNull(opportunity().copy(officialUrl = "http://example.com").validate())
        assertNotNull(opportunity().copy(estimatedMinutes = 0).validate())
    }
}
