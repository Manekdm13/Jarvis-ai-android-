package com.manekdm13.jarvis

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Short in-memory notification digest. Notification text is not persisted by this service. */
class JarvisNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val item = sbn ?: return
        val extras = item.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence("android.bigText")?.toString()?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence("android.text")?.toString()?.trim().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        synchronized(lock) {
            recent.removeAll { it.key == item.key }
            recent.add(0, Notice(item.key, item.packageName, title, text, item.postTime))
            while (recent.size > 30) recent.removeAt(recent.lastIndex)
        }
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        synchronized(lock) { recent.removeAll { it.key == sbn.key } }
    }
    companion object {
        private data class Notice(val key: String, val packageName: String, val title: String, val text: String, val time: Long)
        private val lock = Any()
        private val recent = mutableListOf<Notice>()
        fun digest(): String = synchronized(lock) {
            if (recent.isEmpty()) return@synchronized "No notifications observed since notification access was enabled."
            val clock = SimpleDateFormat("HH:mm", Locale.getDefault())
            recent.take(20).joinToString("\n\n") {
                val app = it.packageName.substringAfterLast('.')
                val heading = it.title.ifBlank { app }
                "[$app • ${clock.format(Date(it.time))}] $heading\n${it.text}".take(900)
            }
        }
    }
}
