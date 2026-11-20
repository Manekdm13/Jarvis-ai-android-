package com.manekdm13.jarvis

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

/**
 * Local safety budget. This is a user-experience guardrail, not a provider quota guarantee.
 * The gateway/provider can impose stricter limits at any time.
 */
internal object UsageBudget {
    private const val PREFS = "jarvis_usage_budget"
    private const val DAY_KEY = "day"
    private const val COUNT_KEY = "requests"
    const val DAILY_FREE_REQUEST_LIMIT = 40

    @Synchronized
    fun tryConsume(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now(ZoneId.systemDefault()).toString()
        if (prefs.getString(DAY_KEY, null) != today) {
            prefs.edit().putString(DAY_KEY, today).putInt(COUNT_KEY, 0).commit()
        }
        val count = prefs.getInt(COUNT_KEY, 0)
        if (count >= DAILY_FREE_REQUEST_LIMIT) return false
        return prefs.edit().putInt(COUNT_KEY, count + 1).commit()
    }

    fun remaining(context: Context): Int {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now(ZoneId.systemDefault()).toString()
        if (prefs.getString(DAY_KEY, null) != today) return DAILY_FREE_REQUEST_LIMIT
        return (DAILY_FREE_REQUEST_LIMIT - prefs.getInt(COUNT_KEY, 0)).coerceAtLeast(0)
    }
}
