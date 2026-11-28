package com.manekdm13.jarvis

import org.junit.Assert.assertTrue
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
}
