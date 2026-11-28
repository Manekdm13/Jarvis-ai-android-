package com.manekdm13.jarvis

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class JarvisTaskStatus { PLANNED, IN_PROGRESS, WAITING_FOR_APPROVAL, COMPLETED, FAILED }

data class JarvisTask(
    val id: String,
    val title: String,
    val status: JarvisTaskStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val notes: String = ""
)

/**
 * Small durable task ledger. It persists task state across app restarts;
 * it does not execute actions by itself. Execution is delegated to approved
 * tools and must record outcomes back into this ledger.
 */
class JarvisTaskEngine(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("jarvis_task_ledger", Context.MODE_PRIVATE)

    @Synchronized
    fun load(): List<JarvisTask> {
        val raw = preferences.getString(KEY_TASKS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val status = runCatching { JarvisTaskStatus.valueOf(item.optString("status")) }.getOrDefault(JarvisTaskStatus.PLANNED)
                JarvisTask(
                    id = item.optString("id"),
                    title = item.optString("title"),
                    status = status,
                    createdAt = item.optLong("createdAt"),
                    updatedAt = item.optLong("updatedAt"),
                    notes = item.optString("notes")
                ).takeIf { it.id.isNotBlank() && it.title.isNotBlank() }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun create(title: String, notes: String = ""): JarvisTask {
        require(title.isNotBlank()) { "Task title cannot be blank." }
        val now = System.currentTimeMillis()
        val task = JarvisTask(UUID.randomUUID().toString(), title.trim(), JarvisTaskStatus.PLANNED, now, now, notes.trim())
        save(load().plus(task).takeLast(MAX_TASKS))
        return task
    }

    @Synchronized
    fun updateStatus(id: String, status: JarvisTaskStatus, notes: String? = null): JarvisTask? {
        var updatedTask: JarvisTask? = null
        val updated = load().map { task ->
            if (task.id != id) task else task.copy(
                status = status,
                updatedAt = System.currentTimeMillis(),
                notes = notes?.trim() ?: task.notes
            ).also { updatedTask = it }
        }
        if (updatedTask != null) save(updated)
        return updatedTask
    }

    @Synchronized
    fun delete(id: String) = save(load().filterNot { it.id == id })

    @Synchronized
    private fun save(tasks: List<JarvisTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("status", task.status.name)
                put("createdAt", task.createdAt)
                put("updatedAt", task.updatedAt)
                put("notes", task.notes)
            })
        }
        check(preferences.edit().putString(KEY_TASKS, array.toString()).commit()) {
            "Could not persist JARVIS task ledger."
        }
    }

    private companion object {
        const val KEY_TASKS = "tasks_v1"
        const val MAX_TASKS = 250
    }
}
