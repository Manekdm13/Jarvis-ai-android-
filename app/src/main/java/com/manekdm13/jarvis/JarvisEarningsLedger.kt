package com.manekdm13.jarvis

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

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

/** Small encrypted-at-rest boundary can be added without changing the ledger schema.
 * Account credentials and payment secrets are deliberately never stored here.
 */
internal class JarvisEarningsLedgerStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun read(): List<JarvisEarningEntry> = runCatching {
        val array = JSONArray(preferences.getString(KEY_ENTRIES, "[]"))
        buildList {
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
    }.getOrDefault(emptyList())

    @Synchronized
    fun save(entry: JarvisEarningEntry) {
        val updated = JarvisEarningsLedger.upsert(read(), entry)
        val array = JSONArray()
        updated.forEach { item ->
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
        check(preferences.edit().putString(KEY_ENTRIES, array.toString()).commit()) {
            "Could not persist the JARVIS earnings ledger."
        }
    }

    companion object {
        private const val PREFS = "jarvis_earnings_ledger"
        private const val KEY_ENTRIES = "entries_v1"
    }
}
