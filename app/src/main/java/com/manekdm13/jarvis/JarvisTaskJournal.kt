package com.manekdm13.jarvis

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted durable task journal. WorkManager owns scheduling; this journal preserves
 * the user's objective, last checkpoint, result/error and timestamps across process death.
 * It is not a substitute for WorkManager's own execution state.
 */
internal class JarvisTaskJournal(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class Entry(
        val id: String,
        val goal: String,
        val status: String,
        val progress: Int,
        val result: String = "",
        val error: String = "",
        val updatedAt: Long = System.currentTimeMillis(),
        val attempt: Int = 0
    )

    @Synchronized
    fun list(): List<Entry> = runCatching {
        val packed = Base64.decode(prefs.getString(DATA, null) ?: return emptyList(), Base64.NO_WRAP)
        require(packed.size > IV_SIZE)
        val iv = packed.copyOfRange(0, IV_SIZE)
        val cipherText = packed.copyOfRange(IV_SIZE, packed.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        val array = JSONArray(String(cipher.doFinal(cipherText), StandardCharsets.UTF_8))
        (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            Entry(
                id = o.optString("id"),
                goal = o.optString("goal"),
                status = o.optString("status"),
                progress = o.optInt("progress").coerceIn(0, 100),
                result = o.optString("result"),
                error = o.optString("error"),
                updatedAt = o.optLong("updatedAt"),
                attempt = o.optInt("attempt")
            ).takeIf { it.id.isNotBlank() }
        }.sortedByDescending { it.updatedAt }
    }.getOrElse { emptyList() }

    @Synchronized
    fun get(taskId: String): Entry? = list().firstOrNull { it.id == taskId }

    @Synchronized
    fun checkpoint(entry: Entry) {
        require(entry.id.isNotBlank() && entry.goal.isNotBlank())
        val entries = list().filterNot { it.id == entry.id }.toMutableList()
        entries += entry.copy(
            goal = entry.goal.take(MAX_GOAL),
            status = entry.status.take(120),
            result = entry.result.take(MAX_RESULT),
            error = entry.error.take(MAX_ERROR),
            progress = entry.progress.coerceIn(0, 100),
            updatedAt = System.currentTimeMillis()
        )
        save(entries.sortedByDescending { it.updatedAt }.take(MAX_TASKS))
    }

    @Synchronized
    fun remove(taskId: String): Boolean {
        val old = list()
        val kept = old.filterNot { it.id == taskId }
        if (kept.size == old.size) return false
        save(kept)
        return true
    }

    private fun save(entries: List<Entry>) {
        val array = JSONArray()
        entries.take(MAX_TASKS).forEach { e ->
            array.put(JSONObject().put("id", e.id).put("goal", e.goal)
                .put("status", e.status).put("progress", e.progress)
                .put("result", e.result).put("error", e.error)
                .put("updatedAt", e.updatedAt).put("attempt", e.attempt))
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(array.toString().toByteArray(StandardCharsets.UTF_8))
        val packed = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, packed, 0, iv.size)
        System.arraycopy(encrypted, 0, packed, iv.size, encrypted.size)
        prefs.edit().putString(DATA, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(KeyGenParameterSpec.Builder(
            KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }

    private companion object {
        const val PREFS = "jarvis_task_journal"
        const val DATA = "encrypted_task_history"
        const val KEY_ALIAS = "jarvis_task_journal_v1"
        const val KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
        const val MAX_TASKS = 30
        const val MAX_GOAL = 2_000
        const val MAX_RESULT = 12_000
        const val MAX_ERROR = 1_000
    }
}
