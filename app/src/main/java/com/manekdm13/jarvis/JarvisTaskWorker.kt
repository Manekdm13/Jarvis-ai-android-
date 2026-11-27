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
 * This worker safely runs a user-requested task prompt through the configured
 * JARVIS gateway when the app is no longer in the foreground. It is not yet a
 * multi-step tool-using agent; that orchestration layer will be added separately.
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
            val taskContext = if (savedProfile.isBlank()) "" else
                "USER PROFILE (explicitly saved by the user):\n$savedProfile"

            val answer = AiGatewayClient.reply(
                baseUrl = gatewayUrl,
                token = token,
                message = "Act as JARVIS task executor. Work on this user goal now: $goal. " +
                    "Make useful progress immediately. Be truthful about what you completed, " +
                    "what you could not do, and the exact next blocker. Do not claim external " +
                    "actions were performed unless verified. Ask for approval before irreversible actions.",
                history = taskContext
            )

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
