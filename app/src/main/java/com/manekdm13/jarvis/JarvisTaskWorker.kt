package com.manekdm13.jarvis

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * First durable execution primitive for JARVIS.
 *
 * This worker runs a bounded, validated multi-step AI plan through the configured
 * JARVIS gateway when the app is no longer in the foreground. Steps can reason,
 * research, and draft using prior results. They do not invoke Android tools or
 * access private app data; device actions remain in the foreground approval broker.
 */
internal class JarvisTaskWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return@withContext Result.failure()
        val goal = inputData.getString(KEY_GOAL)?.trim().orEmpty()
        if (goal.isBlank()) return@withContext Result.failure()

        setProgress(Data.Builder()
            .putString(KEY_TASK_ID, taskId)
            .putString(KEY_STATUS, "Connecting to JARVIS")
            .putInt(KEY_PROGRESS, 10)
            .build())

        val prefs = applicationContext.getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE)
        val gatewayUrl = prefs.getString("ai_gateway_url", null).orEmpty()
        val token = SecureTokenStore.load(applicationContext)
        if (gatewayUrl.isBlank() || token.isBlank()) {
            return@withContext Result.failure(
                Data.Builder()
                    .putString(KEY_TASK_ID, taskId)
                    .putString(KEY_ERROR, "Configure the AI gateway URL and access token in JARVIS settings.")
                    .build()
            )
        }

        return@withContext try {
            setProgress(Data.Builder()
                .putString(KEY_TASK_ID, taskId)
                .putString(KEY_STATUS, "Working on your task")
                .putInt(KEY_PROGRESS, 35)
                .build())

            // Only explicitly saved, encrypted profile context is shared with the online gateway.
            val savedProfile = UserProfileMemory(applicationContext).loadSaved()
            val learnedLessons = JarvisLearningMemory(applicationContext).load()
            val taskContext = buildString {
                if (savedProfile.isNotBlank()) {
                    append("USER PROFILE (explicitly saved by the user):\n")
                    append(savedProfile)
                    append("\n\n")
                }
                if (learnedLessons.isNotEmpty()) {
                    append("LONG-TERM USER-DIRECTED LESSONS (encrypted local memory):\n")
                    append(learnedLessons.joinToString("\n") { "- $it" })
                }
            }

            // Ask the model for a bounded plan, validate it locally, then execute
            // each reasoning step in order. If the provider cannot return valid JSON,
            // retain a safe single-pass fallback rather than losing the user's task.
            val planDraft = AiGatewayClient.reply(
                baseUrl = gatewayUrl,
                token = token,
                message = JarvisTaskOrchestrator.planningPrompt(goal),
                history = taskContext
            )
            val plan = JarvisTaskOrchestrator.parsePlan(
                raw = planDraft,
                taskId = taskId,
                goal = goal,
                now = System.currentTimeMillis()
            )

            val answer = if (plan == null) {
                AiGatewayClient.reply(
                    baseUrl = gatewayUrl,
                    token = token,
                    message = "Act as JARVIS task executor. Work on this user goal now: $goal. " +
                        "Make useful progress immediately. Be truthful about completed work and blockers. " +
                        "Do not claim external actions were performed unless verified.",
                    history = taskContext
                )
            } else {
                val completed = mutableListOf<Pair<String, String>>()
                plan.steps.forEachIndexed { index, step ->
                    val progress = 40 + ((index + 1) * 50 / plan.steps.size)
                    setProgress(Data.Builder()
                        .putString(KEY_TASK_ID, taskId)
                        .putString(KEY_STATUS, "Step ${index + 1}/${plan.steps.size}: ${step.title}")
                        .putInt(KEY_PROGRESS, progress)
                        .build())

                    val stepResult = AiGatewayClient.reply(
                        baseUrl = gatewayUrl,
                        token = token,
                        message = JarvisTaskOrchestrator.stepPrompt(goal, step, completed),
                        history = taskContext
                    )
                    completed += step.title to stepResult.take(4_000)
                }
                buildString {
                    append("JARVIS completed ${completed.size} planned steps.\n\n")
                    completed.forEachIndexed { index, item ->
                        append("${index + 1}. ${item.first}\n")
                        append(item.second)
                        if (index != completed.lastIndex) append("\n\n")
                    }
                }
            }

            Result.success(Data.Builder()
                .putString(KEY_TASK_ID, taskId)
                .putString(KEY_STATUS, "Completed")
                .putString(KEY_RESULT, answer)
                .putInt(KEY_PROGRESS, 100)
                .build())
        } catch (error: Exception) {
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Result.failure(Data.Builder()
                    .putString(KEY_TASK_ID, taskId)
                    .putString(KEY_ERROR, error.message ?: "JARVIS could not complete this task.")
                    .build())
            }
        }
    }

    companion object {
        const val KEY_TASK_ID = "jarvis_task_id"
        const val KEY_GOAL = "jarvis_task_goal"
        const val KEY_STATUS = "jarvis_task_status"
        const val KEY_PROGRESS = "jarvis_task_progress"
        const val KEY_RESULT = "jarvis_task_result"
        const val KEY_ERROR = "jarvis_task_error"
    }
}

/** Enqueues one uniquely named, network-aware task and exposes WorkManager's durable status. */
internal object JarvisTaskScheduler {
    fun enqueue(context: Context, taskId: String, goal: String) {
        require(taskId.isNotBlank()) { "Task ID cannot be blank." }
        require(goal.isNotBlank()) { "Task goal cannot be blank." }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<JarvisTaskWorker>()
            .setInputData(Data.Builder()
                .putString(JarvisTaskWorker.KEY_TASK_ID, taskId)
                .putString(JarvisTaskWorker.KEY_GOAL, goal)
                .build())
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork("jarvis-task-$taskId", ExistingWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context, taskId: String) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork("jarvis-task-$taskId")
    }

    fun observe(context: Context, taskId: String) =
        WorkManager.getInstance(context.applicationContext)
            .getWorkInfosForUniqueWorkLiveData("jarvis-task-$taskId")

    private const val TAG = "jarvis-user-task"
}
