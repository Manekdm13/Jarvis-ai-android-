package com.manekdm13.jarvis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisTaskPlanTest {
    private fun step(
        id: String,
        dependsOn: List<String> = emptyList(),
        status: JarvisStepStatus = JarvisStepStatus.PENDING
    ) = JarvisPlanStep(
        id = id,
        title = "Step $id",
        instruction = "Complete step $id",
        dependsOn = dependsOn,
        status = status
    )

    private fun plan(vararg steps: JarvisPlanStep) = JarvisTaskPlan(
        id = "plan-1",
        goal = "Test a dependency graph",
        createdAtEpochMillis = 1L,
        steps = steps.toList()
    )

    @Test
    fun validAcyclicPlanIsAccepted() {
        assertEquals(PlanValidation.Valid, plan(step("a"), step("b", listOf("a"))).validate())
    }

    @Test
    fun missingDependencyIsRejected() {
        val result = plan(step("a", listOf("missing"))).validate()
        assertTrue(result is PlanValidation.Invalid)
        assertTrue((result as PlanValidation.Invalid).reason.contains("unknown step"))
    }

    @Test
    fun dependencyCycleIsRejected() {
        val result = plan(step("a", listOf("b")), step("b", listOf("a"))).validate()
        assertTrue(result is PlanValidation.Invalid)
        assertTrue((result as PlanValidation.Invalid).reason.contains("cycle"))
    }

    @Test
    fun duplicateStepIdsAreRejected() {
        assertTrue(plan(step("same"), step("same")).validate() is PlanValidation.Invalid)
    }

    @Test
    fun onlyStepsWithSuccessfulPrerequisitesAreReady() {
        val task = plan(
            step("first", status = JarvisStepStatus.SUCCEEDED),
            step("second", dependsOn = listOf("first")),
            step("third", dependsOn = listOf("second")),
            step("blocked", dependsOn = listOf("not-present"))
        )
        assertEquals(listOf("second"), task.readySteps().map { it.id })
    }
}
