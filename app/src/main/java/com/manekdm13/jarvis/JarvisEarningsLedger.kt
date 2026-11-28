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

internal enum class JarvisEarningStatus { SUBMITTED, PENDING, APPROVED, REJECTED, PAID }

internal data class JarvisEarningEntry(
    val id: String,
    val platform: String,
    val taskTitle: String,
    val occurredAtEpochMillis: Long,
    val minutesSpent: Int,
    val grossUsd: Double,
    val feesUsd: Double,
    val netUsd: Double,
    val status: JarvisEarningStatus,
    val payoutRoute: JarvisPayoutRoute? = null,
    val evidenceReference: String? = null
) {
    fun validate(): String? {
        if (id.isBlank() || platform.isBlank() || taskTitle.isBlank()) return "Entry identity is incomplete."
        if (occurredAtEpochMillis <= 0L) return "Entry timestamp is invalid."
        if (minutesSpent < 0) return "Time spent cannot be negative."
        if (!grossUsd.isFinite() || !feesUsd.isFinite() || !netUsd.isFinite()) return "Amounts must be finite."
        if (grossUsd < 0.0 || feesUsd < 0.0) return "Gross reward and fees cannot be negative."
        if (kotlin.math.abs(netUsd - (grossUsd - feesUsd)) > 0.01) return "Net amount must equal gross reward minus fees."
        return null
    }
}

/** Pure ledger operations keep status totals explicit and prevent duplicate task rows. */
internal object JarvisEarningsLedger {
    fun upsert(entries: List<JarvisEarningEntry>, entry: JarvisEarningEntry): List<JarvisEarningEntry> {
        require(entry.validate() == null) { entry.validate() ?: "Invalid ledger entry." }
        return (entries.filterNot { it.id == entry.id } + entry).sortedByDescending { it.occurredAtEpochMillis }
    }

    fun totalUsd(entries: List<JarvisEarningEntry>, status: JarvisEarningStatus): Double =
        entries.filter { it.status == status }.sumOf { it.netUsd }

    fun paidUsd(entries: List<JarvisEarningEntry>): Double =
        totalUsd(entries, JarvisEarningStatus.PAID)

    fun outstandingUsd(entries: List<JarvisEarningEntry>): Double =
        entries.filter { it.status in setOf(JarvisEarningStatus.SUBMITTED, JarvisEarningStatus.PENDING, JarvisEarningStatus.APPROVED) }
            .sumOf { it.netUsd }
}

/**
 * Durable earnings ledger encrypted with an Android Keystore AES-GCM key.
 * A one-time migration encrypts the previous plaintext entries_v1 value.
 * Credentials, OTPs, and payment secrets are never stored in this ledger.
 */
internal class JarvisEarningsLedgerStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun read(): List<JarvisEarningEntry> = runCatching {
        val encrypted = preferences.getString(KEY_ENTRIES_ENCRYPTED, null)
        if (encrypted != null) {
            decodeEntries(decrypt(Base64.decode(encrypted, Base64.NO_WRAP)))
        } else {
            // Migrate legacy plaintext data only after successfully encrypting and committing it.
            val legacy = preferences.getString(KEY_ENTRIES_LEGACY, null) ?: "[]"
            val entries = decodeEntries(legacy)
            persistEncrypted(entries)
            preferences.edit().remove(KEY_ENTRIES_LEGACY).commit()
            entries
        }
    }.getOrDefault(emptyList())

    @Synchronized
    fun save(entry: JarvisEarningEntry) {
        val updated = JarvisEarningsLedger.upsert(read(), entry)
        persistEncrypted(updated)
    }

    private fun decodeEntries(json: String): List<JarvisEarningEntry> {
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val route = item.optString("payoutRoute").takeIf { it.isNotBlank() }
                    ?.let { runCatching { JarvisPayoutRoute.valueOf(it) }.getOrNull() }
                val entry = JarvisEarningEntry(
                    id = item.optString("id"),
                    platform = item.optString("platform"),
                    taskTitle = item.optString("taskTitle"),
                    occurredAtEpochMillis = item.optLong("occurredAtEpochMillis"),
                    minutesSpent = item.optInt("minutesSpent"),
                    grossUsd = item.optDouble("grossUsd", Double.NaN),
                    feesUsd = item.optDouble("feesUsd", Double.NaN),
                    netUsd = item.optDouble("netUsd", Double.NaN),
                    status = JarvisEarningStatus.valueOf(item.optString("status")),
                    payoutRoute = route,
                    evidenceReference = item.optString("evidenceReference").takeIf { it.isNotBlank() }
                )
                if (entry.validate() == null) add(entry)
            }
        }
    }

    private fun persistEncrypted(entries: List<JarvisEarningEntry>) {
        val array = JSONArray()
        entries.forEach { item ->
            array.put(JSONObject()
                .put("id", item.id)
                .put("platform", item.platform)
                .put("taskTitle", item.taskTitle)
                .put("occurredAtEpochMillis", item.occurredAtEpochMillis)
                .put("minutesSpent", item.minutesSpent)
                .put("grossUsd", item.grossUsd)
                .put("feesUsd", item.feesUsd)
                .put("netUsd", item.netUsd)
                .put("status", item.status.name)
                .put("payoutRoute", item.payoutRoute?.name ?: "")
                .put("evidenceReference", item.evidenceReference ?: ""))
        }
        val encoded = Base64.encodeToString(encrypt(array.toString()), Base64.NO_WRAP)
        check(preferences.edit().putString(KEY_ENTRIES_ENCRYPTED, encoded).commit()) {
            "Could not persist the encrypted JARVIS earnings ledger."
        }
        // Remove any old plaintext only after the encrypted replacement is durable.
        preferences.edit().remove(KEY_ENTRIES_LEGACY).commit()
    }

    private fun encrypt(plainText: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = requireNotNull(cipher.iv)
        val ciphertext = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        return ByteArray(iv.size + ciphertext.size).also { packed ->
            System.arraycopy(iv, 0, packed, 0, iv.size)
            System.arraycopy(ciphertext, 0, packed, iv.size, ciphertext.size)
        }
    }

    private fun decrypt(packed: ByteArray): String {
        require(packed.size > IV_BYTES) { "Encrypted earnings ledger is truncated." }
        val iv = packed.copyOfRange(0, IV_BYTES)
        val ciphertext = packed.copyOfRange(IV_BYTES, packed.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
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
        const val PREFS = "jarvis_earnings_ledger"
        const val KEY_ENTRIES_LEGACY = "entries_v1"
        const val KEY_ENTRIES_ENCRYPTED = "entries_encrypted_v2"
        const val KEY_ALIAS = "jarvis_earnings_ledger_v2"
        const val KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
