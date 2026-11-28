package com.manekdm13.jarvis

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class JarvisTaskStatus { PLANNED, IN_PROGRESS, WAITING_FOR_APPROVAL, COMPLETED, FAILED, CANCELLED }

data class JarvisTask(
    val id: String,
    val title: String,
    val status: JarvisTaskStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val notes: String = "",
    val planId: String? = null
)

/**
 * Keystore-encrypted durable task ledger.
 * WorkManager stores only an opaque task ID; goals and results remain encrypted here.
 */
class JarvisTaskEngine(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun load(): List<JarvisTask> {
        val encrypted = preferences.getString(KEY_CIPHERTEXT, null)
        if (!encrypted.isNullOrBlank()) {
            return runCatching { decode(decrypt(encrypted)) }
                .getOrElse {
                    preferences.edit().remove(KEY_CIPHERTEXT).commit()
                    emptyList()
                }
        }

        // One-time migration from the original plaintext v1 ledger.
        val legacy = preferences.getString(KEY_LEGACY_TASKS, null) ?: return emptyList()
        val migrated = runCatching { decode(legacy) }.getOrDefault(emptyList())
        if (migrated.isNotEmpty()) save(migrated)
        preferences.edit().remove(KEY_LEGACY_TASKS).commit()
        return migrated
    }

    @Synchronized
    fun create(title: String, notes: String = ""): JarvisTask {
        require(title.isNotBlank()) { "Task title cannot be blank." }
        val now = System.currentTimeMillis()
        val task = JarvisTask(UUID.randomUUID().toString(), title.trim(), JarvisTaskStatus.PLANNED, now, now, notes.trim())
        save((load() + task).takeLast(MAX_TASKS))
        return task
    }

    @Synchronized
    fun createPlan(title: String, stepLines: List<String>): List<JarvisTask> {
        require(title.isNotBlank()) { "Plan title cannot be blank." }
        val steps = stepLines.map { line ->
            line.trim().replace(Regex("^([0-9]+[.)]|[-*•])\\s*"), "").trim()
        }.filter { it.length >= 3 }.distinct().take(20)
        require(steps.isNotEmpty()) { "The plan must contain at least one actionable step." }
        val planId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val created = steps.mapIndexed { index, step ->
            JarvisTask(
                id = UUID.randomUUID().toString(),
                title = step,
                status = JarvisTaskStatus.PLANNED,
                createdAt = now + index,
                updatedAt = now + index,
                notes = "Plan: ${title.trim()}",
                planId = planId
            )
        }
        save((load() + created).takeLast(MAX_TASKS))
        return created
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

    private fun decode(raw: String): List<JarvisTask> {
        val array = JSONArray(raw)
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val status = runCatching { JarvisTaskStatus.valueOf(item.optString("status")) }
                .getOrDefault(JarvisTaskStatus.PLANNED)
            JarvisTask(
                id = item.optString("id"),
                title = item.optString("title"),
                status = status,
                createdAt = item.optLong("createdAt"),
                updatedAt = item.optLong("updatedAt"),
                notes = item.optString("notes"),
                planId = item.optString("planId").takeIf { it.isNotBlank() }
            ).takeIf { it.id.isNotBlank() && it.title.isNotBlank() }
        }
    }

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
                put("planId", task.planId ?: "")
            })
        }
        val encrypted = encrypt(array.toString())
        check(preferences.edit().putString(KEY_CIPHERTEXT, encrypted).remove(KEY_LEGACY_TASKS).commit()) {
            "Could not persist encrypted JARVIS task ledger."
        }
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = requireNotNull(cipher.iv)
        val ciphertext = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val packed = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, packed, 0, iv.size)
        System.arraycopy(ciphertext, 0, packed, iv.size, ciphertext.size)
        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val packed = Base64.decode(encoded, Base64.NO_WRAP)
        require(packed.size > IV_LENGTH) { "Stored task ledger is incomplete." }
        val iv = packed.copyOfRange(0, IV_LENGTH)
        val ciphertext = packed.copyOfRange(IV_LENGTH, packed.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES_NAME = "jarvis_task_ledger"
        const val KEY_CIPHERTEXT = "tasks_ciphertext_v2"
        const val KEY_LEGACY_TASKS = "tasks_v1"
        const val KEY_ALIAS = "jarvis_task_ledger_v2"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
        const val MAX_TASKS = 250
    }
}
