package com.manekdm13.jarvis

/**
 * Structured, auditable task model for JARVIS autonomy.
 *
 * Plans are data only: creating a plan never executes a tool or grants permission.
 * Execution must go through a permission-scoped tool runner and approval broker.
 */
internal enum class JarvisStepStatus {
    PENDING,
    RUNNING,
    WAITING_FOR_APPROVAL,
    SUCCEEDED,
    FAILED,
    CANCELLED
}

internal enum class JarvisApprovalLevel {
    NONE,
    USER_CONFIRMATION,
    SENSITIVE_CONFIRMATION
}

internal data class JarvisPlanStep(
    val id: String,
    val title: String,
    val instruction: String,
    val dependsOn: List<String> = emptyList(),
    val requiredTool: String? = null,
    val approvalLevel: JarvisApprovalLevel = JarvisApprovalLevel.NONE,
    val status: JarvisStepStatus = JarvisStepStatus.PENDING,
    val result: String? = null,
    val error: String? = null
)

internal data class JarvisTaskPlan(
    val id: String,
    val goal: String,
    val createdAtEpochMillis: Long,
    val steps: List<JarvisPlanStep>,
    val status: JarvisStepStatus = JarvisStepStatus.PENDING
) {
    /**
     * Reject malformed plans before any execution. Dependencies must exist,
     * cannot refer to the step itself, and the graph must be acyclic.
     */
    fun validate(): PlanValidation {
        if (id.isBlank()) return PlanValidation.Invalid("Task ID is empty.")
        if (goal.isBlank()) return PlanValidation.Invalid("Task goal is empty.")
        if (steps.isEmpty()) return PlanValidation.Invalid("A task plan must contain at least one step.")

        val ids = steps.map { it.id }
        if (ids.any { it.isBlank() }) return PlanValidation.Invalid("A step ID is empty.")
        if (ids.size != ids.toSet().size) return PlanValidation.Invalid("Step IDs must be unique.")

        val knownIds = ids.toSet()
        for (step in steps) {
            if (step.title.isBlank() || step.instruction.isBlank()) {
                return PlanValidation.Invalid("Every step needs a title and instruction.")
            }
            if (step.id in step.dependsOn) {
                return PlanValidation.Invalid("A step cannot depend on itself: ${step.id}.")
            }
            val missing = step.dependsOn.firstOrNull { it !in knownIds }
            if (missing != null) {
                return PlanValidation.Invalid("Step ${step.id} depends on unknown step $missing.")
            }
        }

        val dependencies = steps.associate { it.id to it.dependsOn }
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()

        fun hasCycle(stepId: String): Boolean {
            if (stepId in visiting) return true
            if (stepId in visited) return false
            visiting += stepId
            for (dependency in dependencies[stepId].orEmpty()) {
                if (hasCycle(dependency)) return true
            }
            visiting -= stepId
            visited += stepId
            return false
        }

        if (ids.any(::hasCycle)) return PlanValidation.Invalid("Task plan contains a dependency cycle.")
        return PlanValidation.Valid
    }

    /** Only steps whose prerequisites succeeded may be considered runnable. */
    fun readySteps(): List<JarvisPlanStep> {
        val completed = steps.filter { it.status == JarvisStepStatus.SUCCEEDED }
            .map { it.id }
            .toSet()
        return steps.filter { step ->
            step.status == JarvisStepStatus.PENDING &&
                step.dependsOn.all { it in completed }
        }
    }
}

internal sealed class PlanValidation {
    data object Valid : PlanValidation()
    data class Invalid(val reason: String) : PlanValidation()
}
