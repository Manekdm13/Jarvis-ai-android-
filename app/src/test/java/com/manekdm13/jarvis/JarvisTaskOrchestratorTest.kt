package com.manekdm13.jarvis

import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class JarvisTaskOrchestratorTest {
    @Test
    fun plannerPromptRequiresBoundedStructuredPlan() {
        val prompt = JarvisTaskOrchestrator.planningPrompt("Research food safety scholarships")
        assertTrue(prompt.contains("\"steps\""))
        assertTrue(prompt.contains("at most ${JarvisTaskOrchestrator.MAX_STEPS}"))
        assertTrue(prompt.contains("Do not include chain-of-thought"))
        assertTrue(prompt.contains("Do not request or imply access to private"))
    }

    @Test
    fun stepPromptCarriesEarlierResultsWithoutRequestingPrivateReasoning() {
        val step = JarvisPlanStep(
            id = "step-2",
            title = "Compare options",
            instruction = "Compare the verified options",
            dependsOn = listOf("step-1")
        )
        val prompt = JarvisTaskOrchestrator.stepPrompt(
            goal = "Find a scholarship",
            step = step,
            completed = listOf("Find options" to "Option A is fully funded.")
        )
        assertTrue(prompt.contains("Option A is fully funded."))
        assertTrue(prompt.contains("Compare the verified options"))
        assertTrue(prompt.contains("Do not reveal private chain-of-thought"))
        assertTrue(prompt.contains("not independently verified"))
        assertTrue(prompt.contains("verify important claims"))
    }
    @Test
    fun reviewPromptRequiresIndependentCheckingAndSafeRecovery() {
        val prompt = JarvisTaskOrchestrator.reviewPrompt(
            goal = "Prepare a comparison and recommendation",
            completed = listOf("Compare options" to "Option A costs $20.")
        )
        assertTrue(prompt.contains("Independently check"))
        assertTrue(prompt.contains("repair it now"))
        assertTrue(prompt.contains("smallest"))
        assertTrue(prompt.contains("Do not claim external actions"))
        assertTrue(prompt.contains("Option A costs $20."))
        assertTrue(prompt.contains("Do not reveal chain-of-thought"))
    }

    @Test
    fun reviewCanRequestExactlyOneBoundedRepair() {
        val incomplete = "[JARVIS_REVIEW: NEEDS_REPAIR] Missing verified deadlines."
        assertTrue(JarvisTaskOrchestrator.needsRepair(incomplete))
        assertTrue(JarvisTaskOrchestrator.repairPrompt("Find scholarships", incomplete).contains("Missing verified deadlines."))
        assertTrue(JarvisTaskOrchestrator.needsRepair("[JARVIS_REVIEW: NEEDS_REPAIR]"))
        org.junit.Assert.assertFalse(JarvisTaskOrchestrator.needsRepair("[JARVIS_REVIEW: COMPLETE] All requirements met."))
    }
}
