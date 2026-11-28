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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resumable, network-constrained task worker.
 * WorkManager persists the request; only an opaque task ID is placed in WorkManager
 * input data. The task goal and result stay in the Keystore-encrypted task ledger.
 *
 * This worker performs research synthesis, planning and drafting only. It does not
 * send messages, publish, delete, purchase, call, or control other apps.
 */
internal class JarvisTaskWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return@withContext Result.failure()
        val engine = JarvisTaskEngine(applicationContext)
        val task = engine.load().firstOrNull { it.id == taskId } ?: return@withContext Result.failure()

        if (task.status == JarvisTaskStatus.WAITING_FOR_APPROVAL ||
            task.status == JarvisTaskStatus.CANCELLED ||
            task.status == JarvisTaskStatus.COMPLETED
        ) return@withContext Result.success()

        engine.updateStatus(taskId, JarvisTaskStatus.IN_PROGRESS)
        val settings = applicationContext.getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE)
        val gatewayUrl = settings.getString("ai_gateway_url", null).orEmpty()
        val token = SecureTokenStore.load(applicationContext)
        if (gatewayUrl.isBlank() || token.isBlank()) {
            engine.updateStatus(taskId, JarvisTaskStatus.FAILED, "Configure the AI gateway URL and private access token.")
            return@withContext Result.failure()
        }

        setProgress(Data.Builder()
            .putString(KEY_TASK_ID, taskId)
            .putString(KEY_STATUS, "Working in the background")
            .build())

        try {
            val prompt = buildString {
                append("You are JARVIS executing a user-approved background task.\n")
                append("Task: ").append(task.title).append("\n")
                if (task.notes.isNotBlank()) append("Context: ").append(task.notes).append("\n")
                append(
                    """
                    Produce a useful, evidence-aware result for this task. This execution scope is
                    limited to research synthesis, reasoning, planning and drafting. Do not send
                    messages, submit forms, publish posts, delete data, purchase items, place calls,
                    change settings, or claim to have changed any external system. If the goal
                    requests one of those actions, prepare a draft/checklist and clearly state that
                    the actual action requires a separate connected tool and fresh approval.
                    Distinguish verified facts from assumptions and report missing access honestly.
                    """.trimIndent()
                )
            }
            val result = AiGatewayClient.reply(gatewayUrl, token, prompt, "")
            engine.updateStatus(taskId, JarvisTaskStatus.COMPLETED, result.take(MAX_STORED_RESULT_CHARS))
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                engine.updateStatus(
                    taskId,
                    JarvisTaskStatus.FAILED,
                    (error.message ?: "Background task failed.").take(1000)
                )
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_TASK_ID = "jarvis_task_id"
        const val KEY_STATUS = "jarvis_task_status"
        private const val MAX_RETRIES = 2
        private const val MAX_STORED_RESULT_CHARS = 6000
    }
}

/** WorkManager unique work prevents duplicate runs and survives process death/reboot. */
internal object JarvisTaskScheduler {
    fun enqueue(context: Context, taskId: String) {
        require(taskId.isNotBlank()) { "Task ID cannot be blank." }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<JarvisTaskWorker>()
            .setInputData(Data.Builder().putString(JarvisTaskWorker.KEY_TASK_ID, taskId).build())
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

    private const val TAG = "jarvis-background-task"
}
