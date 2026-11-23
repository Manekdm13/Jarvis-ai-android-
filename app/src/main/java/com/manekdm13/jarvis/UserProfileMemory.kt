package com.manekdm13.jarvis

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted, editable user context stored only in app-private preferences.
 * The profile is included in AI requests only as conversation context.
 */
internal class UserProfileMemory(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): String {
        val encoded = preferences.getString(CIPHERTEXT_KEY, null)
        if (encoded == null) {
            return if (preferences.getBoolean(INITIALIZED_KEY, false)) "" else DEFAULT_PROFILE
        }
        return runCatching {
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            require(packed.size > IV_LENGTH) { "Stored profile is incomplete" }
            val iv = packed.copyOfRange(0, IV_LENGTH)
            val ciphertext = packed.copyOfRange(IV_LENGTH, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
        }.getOrElse {
            preferences.edit().remove(CIPHERTEXT_KEY).putBoolean(INITIALIZED_KEY, true).apply()
            ""
        }
    }

    fun save(profile: String) {
        require(profile.length <= MAX_PROFILE_CHARS) { "Profile is too long (maximum $MAX_PROFILE_CHARS characters)." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = requireNotNull(cipher.iv)
        val ciphertext = cipher.doFinal(profile.toByteArray(StandardCharsets.UTF_8))
        val packed = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, packed, 0, iv.size)
        System.arraycopy(ciphertext, 0, packed, iv.size, ciphertext.size)
        preferences.edit()
            .putString(CIPHERTEXT_KEY, Base64.encodeToString(packed, Base64.NO_WRAP))
            .putBoolean(INITIALIZED_KEY, true)
            .apply()
    }

    fun clear() {
        preferences.edit().remove(CIPHERTEXT_KEY).putBoolean(INITIALIZED_KEY, true).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
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
        const val PREFERENCES_NAME = "jarvis_private_profile"
        const val CIPHERTEXT_KEY = "profile_ciphertext"
        const val INITIALIZED_KEY = "profile_initialized"
        const val KEY_ALIAS = "jarvis_user_profile_v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
        const val MAX_PROFILE_CHARS = 4000

        const val DEFAULT_PROFILE = """Country: Pakistan
Education: BS (Hons) Food Science and Technology, University of Agriculture Faisalabad (UAF); started 2023; expected graduation 2027; CGPA 3.11/4.00.
Academic interests: food science and technology, food safety and quality, biotechnology, microbiology, nutrition, agricultural sciences, sustainable food systems, and scientific research.
Practical experience: HACCP, CCP, food analysis, dairy, spectroscopy, food quality and acceptability, and food safety.
Skills: MS Word, Excel, PowerPoint, and food laboratory skills. Languages: Urdu and English.
Activities: Food Fest 2025 and Entrepreneur Competition 2025 at UAF; HP LIFE Project Manager and Business Email certificates; athletics, cricket, and football tournament awards.
Goals: fully funded international Master's opportunities for the 2027 cycle, scientific research, and building JARVIS AI.
Other interests: AI engineering, programming, global stock markets, financial literacy, entrepreneurship, online income opportunities, and food product research and development."""
    }
}
