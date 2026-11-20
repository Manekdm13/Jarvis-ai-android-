package com.manekdm13.jarvis

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Executes one explicitly queued AI task. WorkManager persists the request across app closure
 * and process death; Android still controls when background work is scheduled.
 */
internal class JarvisBackgroundTaskWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val task = inputData.getString(KEY_TASK)?.takeIf { it.isNotBlank() } ?: return Result.failure()
        val gateway = inputData.getString(KEY_GATEWAY)?.takeIf { it.startsWith("https://") }
            ?: return Result.failure()

        if (!UsageBudget.tryConsume(applicationContext)) {
            postStatus("Daily free request allowance reached", "JARVIS paused this task to protect your free quota.")
            return Result.failure()
        }

        return try {
            val answer = AiGatewayClient.reply(gateway, task, "This request was queued by the user for background completion. Keep the response actionable.")
            postStatus("JARVIS task completed", answer.take(180))
            Result.success()
        } catch (error: Exception) {
            if (runAttemptCount < 2) Result.retry()
            else {
                postStatus("JARVIS task needs attention", error.message ?: "The AI provider is unavailable.")
                Result.failure()
            }
        }
    }

    private fun postStatus(title: String, message: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "JARVIS task updates", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    companion object {
        const val KEY_TASK = "task"
        const val KEY_GATEWAY = "gateway"
        const val CHANNEL_ID = "jarvis_task_updates"
    }
}
