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
 * Small, device-local conversation history.
 *
 * Messages are encrypted with an AES key held by Android Keystore. The key is
 * non-exportable, so the stored ciphertext cannot be read as plain text from
 * the app's private preferences.
 */
internal class ConversationMemory(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<String> {
        val encoded = preferences.getString(CIPHERTEXT_KEY, null) ?: return emptyList()

        return try {
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            require(packed.size > IV_LENGTH) { "Stored conversation is incomplete" }

            val iv = packed.copyOfRange(0, IV_LENGTH)
            val ciphertext = packed.copyOfRange(IV_LENGTH, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))

            val json = String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
            val array = JSONArray(json)
            (0 until array.length())
                .mapNotNull { index -> array.optString(index).takeIf(String::isNotBlank) }
                .takeLast(MAX_MESSAGES)
        } catch (_: Exception) {
            // A cleared/rotated Keystore key or damaged ciphertext must not crash startup.
            preferences.edit().remove(CIPHERTEXT_KEY).apply()
            emptyList()
        }
    }

    fun save(messages: List<String>) {
        try {
            val array = JSONArray()
            messages.takeLast(MAX_MESSAGES).forEach(array::put)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = requireNotNull(cipher.iv)
            val ciphertext = cipher.doFinal(array.toString().toByteArray(StandardCharsets.UTF_8))
            val packed = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, packed, 0, iv.size)
            System.arraycopy(ciphertext, 0, packed, iv.size, ciphertext.size)

            preferences.edit()
                .putString(CIPHERTEXT_KEY, Base64.encodeToString(packed, Base64.NO_WRAP))
                .apply()
        } catch (_: Exception) {
            // Persistence is best-effort; a Keystore/storage issue must not block chat.
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val specification = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(specification)
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES_NAME = "jarvis_private_memory"
        const val CIPHERTEXT_KEY = "conversation_ciphertext"
        const val KEY_ALIAS = "jarvis_conversation_memory_v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
        const val MAX_MESSAGES = 100
    }
}
