package com.manekdm13.jarvis

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.UUID

internal object BackgroundTaskScheduler {
    fun enqueue(context: Context, gatewayUrl: String, task: String): UUID {
        require(gatewayUrl.trim().startsWith("https://")) { "Configure a secure HTTPS AI gateway first." }
        require(task.isNotBlank()) { "Enter a task first." }
        val request = OneTimeWorkRequestBuilder<JarvisBackgroundTaskWorker>()
            .setInputData(
                Data.Builder()
                    .putString(JarvisBackgroundTaskWorker.KEY_TASK, task.take(4000))
                    .putString(JarvisBackgroundTaskWorker.KEY_GATEWAY, gatewayUrl.trim())
                    .build()
            )
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, java.time.Duration.ofSeconds(30))
            .build()
        WorkManager.getInstance(context.applicationContext).enqueue(request)
        return request.id
    }
}
