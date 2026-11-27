package com.manekdm13.jarvis

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * User-directed, encrypted long-term learning store.
 *
 * JARVIS never silently promotes chat text into permanent memory. The user must
 * issue an explicit "remember that ..." instruction. Entries are bounded,
 * deduplicated, editable by removal, and stored locally with an Android Keystore key.
 */
internal class JarvisLearningMemory(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    @Synchronized
    fun load(): List<String> = runCatching {
        val encoded = preferences.getString(DATA_KEY, null) ?: return emptyList()
        val packed = Base64.decode(encoded, Base64.NO_WRAP)
        require(packed.size > IV_BYTES)
        val iv = packed.copyOfRange(0, IV_BYTES)
        val ciphertext = packed.copyOfRange(IV_BYTES, packed.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        val array = JSONArray(String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8))
        (0 until array.length()).mapNotNull { i ->
            array.optString(i).trim().takeIf { it.isNotBlank() }
        }.takeLast(MAX_ENTRIES)
    }.getOrElse { emptyList() }

    @Synchronized
    fun remember(lesson: String): Boolean {
        val clean = lesson.trim().take(MAX_ENTRY_CHARS)
        if (clean.isBlank()) return false
        val entries = load().filterNot { it.equals(clean, ignoreCase = true) }.toMutableList()
        entries.add(clean)
        save(entries.takeLast(MAX_ENTRIES))
        return true
    }

    @Synchronized
    fun forget(matchingText: String): Int {
        val query = matchingText.trim()
        if (query.isBlank()) return 0
        val entries = load()
        val kept = entries.filterNot { it.contains(query, ignoreCase = true) }
        val removed = entries.size - kept.size
        if (removed > 0) save(kept)
        return removed
    }

    @Synchronized
    fun clear() = save(emptyList())

    private fun save(entries: List<String>) {
        val array = JSONArray()
        entries.takeLast(MAX_ENTRIES).forEach(array::put)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = requireNotNull(cipher.iv)
        val ciphertext = cipher.doFinal(array.toString().toByteArray(StandardCharsets.UTF_8))
        val packed = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, packed, 0, iv.size)
        System.arraycopy(ciphertext, 0, packed, iv.size, ciphertext.size)
        preferences.edit().putString(DATA_KEY, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
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
        const val PREFERENCES = "jarvis_learning_memory"
        const val DATA_KEY = "encrypted_user_directed_lessons"
        const val KEY_ALIAS = "jarvis_learning_memory_v1"
        const val KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val MAX_ENTRIES = 100
        const val MAX_ENTRY_CHARS = 500
    }
}
