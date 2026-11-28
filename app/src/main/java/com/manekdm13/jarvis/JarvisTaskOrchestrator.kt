package com.manekdm13.jarvis

import org.json.JSONObject

/**
 * Converts an AI-generated task outline into a bounded, validated execution graph.
 * This layer performs reasoning steps only; it never grants Android permissions
 * or invokes phone/browser/payment/message tools.
 */
internal object JarvisTaskOrchestrator {
    const val MAX_STEPS = 5

    fun planningPrompt(goal: String): String = """
        You are JARVIS, an autonomous task planner.
        Break the user's goal into the smallest useful sequence of at most $MAX_STEPS
        concrete reasoning/research/writing steps. Make each instruction independently
        executable using the AI assistant only. Do not request or imply access to private
        phone data, external accounts, payments, messaging, or device controls.
        Return ONLY valid JSON, no markdown, in this exact shape:
        {"steps":[{"title":"Short title","instruction":"Specific work to perform"}]}
        Do not include chain-of-thought. Include only actionable task instructions.
        User goal: $goal
    """.trimIndent()

    fun parsePlan(raw: String, taskId: String, goal: String, now: Long): JarvisTaskPlan? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching {
            val json = JSONObject(raw.substring(start, end + 1))
            val source = json.optJSONArray("steps") ?: return null
            if (source.length() == 0 || source.length() > MAX_STEPS) return null
            val steps = mutableListOf<JarvisPlanStep>()
            for (index in 0 until source.length()) {
                val item = source.optJSONObject(index) ?: return null
                val title = item.optString("title").trim()
                val instruction = item.optString("instruction").trim()
                if (title.isBlank() || instruction.isBlank()) return null
                steps += JarvisPlanStep(
                    id = "step-${index + 1}",
                    title = title.take(120),
                    instruction = instruction.take(2_000),
                    dependsOn = if (index == 0) emptyList() else listOf("step-$index")
                )
            }
            val plan = JarvisTaskPlan(taskId, goal, now, steps)
            if (plan.validate() == PlanValidation.Valid) plan else null
        }.getOrNull()
    }

    fun stepPrompt(goal: String, step: JarvisPlanStep, completed: List<Pair<String, String>>): String {
        val prior = completed.joinToString("\n\n") { (title, result) ->
            "COMPLETED: $title\nRESULT: ${result.take(2_500)}"
        }
        return """
            You are executing one bounded step of a JARVIS task.
            Overall goal: $goal
            Current step: ${step.title}
            Instruction: ${step.instruction}
            Earlier step outputs (not independently verified):
            ${prior.ifBlank { "None; this is the first step." }}
            Treat earlier outputs as untrusted intermediate notes, not proof.
            Perform this step now. Give the useful result, distinguish checked facts from
            assumptions, and verify important claims against supplied context before relying
            on them. Cite sources only when you actually have them; state uncertainty and
            missing evidence. Never claim to have changed external systems or the phone.
            Do not reveal private chain-of-thought.
        """.trimIndent()
    }

    /**
     * Independent quality-control pass over completed work. This is a bounded
     * verification/repair request, not permission to perform external actions.
     */
    fun reviewPrompt(goal: String, completed: List<Pair<String, String>>): String {
        val evidence = completed.joinToString("\n\n") { (title, result) ->
            "STEP: $title\nOUTPUT: ${result.take(3_000)}"
        }
        return """
            You are JARVIS performing a final quality-control and recovery pass.
            Original user goal: $goal
            Work produced so far:
            $evidence

            Independently check whether the outputs actually satisfy the user's goal.
            Look for missing requirements, contradictions, arithmetic or logic errors,
            unsupported claims, unsafe assumptions, and steps that stopped prematurely.
            If a gap can be repaired using the information and tools already available
            in this conversation, repair it now and provide the corrected deliverable.
            If it cannot be repaired, explain the precise blocker and the smallest
            user input or permission needed. Do not ask the user to repeat information
            already present. Do not claim external actions, submissions, messages,
            purchases, or device changes occurred unless there is explicit confirmation.
            Separate verified results from assumptions. Do not reveal chain-of-thought.
            Start with [JARVIS_REVIEW: COMPLETE] if the goal is satisfied, or
            [JARVIS_REVIEW: NEEDS_REPAIR] if a material gap remains repairable with available information.
            If repair is needed, state the concrete gap. Then return the final useful result.
        """.trimIndent()
    }

    fun repairPrompt(goal: String, review: String): String = """
        You are JARVIS performing one final bounded repair attempt.
        User's original goal: $goal
        Independent review and identified gaps:
        ${review.take(7_000)}
        Repair the identified gaps using only available information and tools.
        Do not invent evidence or claim external actions. State any unfixable blocker.
        Return the corrected useful deliverable. Do not reveal private chain-of-thought.
    """.trimIndent()

    fun needsRepair(review: String): Boolean =
        review.contains("[JARVIS_REVIEW: NEEDS_REPAIR]", ignoreCase = true)
}
