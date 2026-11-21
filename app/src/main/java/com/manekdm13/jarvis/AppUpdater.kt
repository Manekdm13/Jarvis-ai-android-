package com.manekdm13.jarvis

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class JarvisUpdate(val versionName: String, val apkUrl: String)

object AppUpdater {
    private const val RELEASE_API =
        "https://api.github.com/repos/Manekdm13/Jarvis-ai-android-/releases/latest"

    suspend fun check(): JarvisUpdate? = withContext(Dispatchers.IO) {
        val connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val version = release.optString("tag_name").removePrefix("v")
            val assets = release.optJSONArray("assets") ?: return@withContext null
            var apkUrl: String? = null
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    break
                }
            }
            if (apkUrl.isNullOrBlank() || compareVersions(version, BuildConfig.VERSION_NAME) <= 0) {
                null
            } else {
                JarvisUpdate(version, apkUrl)
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun downloadAndInstall(context: Context, update: JarvisUpdate) =
        withContext(Dispatchers.IO) {
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(update.apkUrl))
                .setTitle("JARVIS update ${update.versionName}")
                .setDescription("Downloading the latest JARVIS update")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(
                    context,
                    Environment.DIRECTORY_DOWNLOADS,
                    "jarvis-update-${update.versionName}.apk"
                )
            val downloadId = manager.enqueue(request)
            var completedUri: Uri? = null
            for (attempt in 0 until 600) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                manager.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                completedUri = manager.getUriForDownloadedFile(downloadId)
                            }
                            DownloadManager.STATUS_FAILED -> {
                                throw IllegalStateException("Android could not download the update.")
                            }
                        }
                    }
                }
                if (completedUri != null) break
                delay(500)
            }
            val apkUri = completedUri ?: throw IllegalStateException("The update download timed out.")
            withContext(Dispatchers.Main) {
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(installIntent)
            }
        }

    private fun compareVersions(first: String, second: String): Int {
        val a = first.split(".").map { it.toIntOrNull() ?: 0 }
        val b = second.split(".").map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(a.size, b.size)) {
            val difference = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
            if (difference != 0) return difference
        }
        return 0
    }
}
