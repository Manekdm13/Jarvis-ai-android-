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
            if (source.length() == 0) return null
            val steps = mutableListOf<JarvisPlanStep>()
            for (index in 0 until minOf(source.length(), MAX_STEPS)) {
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
            Earlier verified step outputs:
            ${prior.ifBlank { "None; this is the first step." }}
            Perform this step now. Give the useful result, cite sources when available,
            state uncertainty, and do not claim to have changed external systems or the
            phone. Do not reveal private chain-of-thought.
        """.trimIndent()
    }
}
