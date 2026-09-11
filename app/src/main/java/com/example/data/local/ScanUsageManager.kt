package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScanUsageInfo(
    val scansUsed: Int,
    val monthlyLimit: Int = 10,
    val currentMonthFormatted: String = "",
    val isPro: Boolean = false
) {
    val scansRemaining: Int
        get() = if (isPro) Int.MAX_VALUE else (monthlyLimit - scansUsed).coerceAtLeast(0)

    val isLimitReached: Boolean
        get() = !isPro && scansUsed >= monthlyLimit

    val displayText: String
        get() = if (isPro) {
            "Unlimited AI scans"
        } else {
            val remaining = scansRemaining
            when (remaining) {
                1 -> "1 scan remaining"
                else -> "$remaining scans remaining"
            }
        }

    val fractionUsed: Float
        get() = if (isPro) 0f else (scansUsed.toFloat() / monthlyLimit.toFloat()).coerceIn(0f, 1f)
}

class ScanUsageManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "curb_scan_usage_prefs"
        private const val KEY_SCANS_USED = "key_scans_used"
        private const val KEY_MONTH_KEY = "key_month_key"
        const val MONTHLY_FREE_LIMIT = 10
    }

    private fun getCurrentMonthKey(): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    }

    private fun getHumanReadableMonth(): String {
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }

    private fun ensureCurrentMonth() {
        val currentMonth = getCurrentMonthKey()
        val savedMonth = prefs.getString(KEY_MONTH_KEY, null)
        if (savedMonth != currentMonth) {
            // New calendar month: reset free scan usage
            prefs.edit()
                .putString(KEY_MONTH_KEY, currentMonth)
                .putInt(KEY_SCANS_USED, 0)
                .apply()
        }
    }

    @Synchronized
    fun getUsageInfo(isPro: Boolean): ScanUsageInfo {
        ensureCurrentMonth()
        val scansUsed = prefs.getInt(KEY_SCANS_USED, 0)
        return ScanUsageInfo(
            scansUsed = scansUsed,
            monthlyLimit = MONTHLY_FREE_LIMIT,
            currentMonthFormatted = getHumanReadableMonth(),
            isPro = isPro
        )
    }

    @Synchronized
    fun canPerformScan(isPro: Boolean): Boolean {
        if (isPro) return true
        ensureCurrentMonth()
        val scansUsed = prefs.getInt(KEY_SCANS_USED, 0)
        return scansUsed < MONTHLY_FREE_LIMIT
    }

    @Synchronized
    fun consumeScan(isPro: Boolean): ScanUsageInfo {
        if (isPro) return getUsageInfo(true)
        ensureCurrentMonth()
        val current = prefs.getInt(KEY_SCANS_USED, 0)
        val updated = (current + 1).coerceAtMost(MONTHLY_FREE_LIMIT)
        prefs.edit().putInt(KEY_SCANS_USED, updated).apply()
        return getUsageInfo(false)
    }

    @Synchronized
    fun resetUsage() {
        prefs.edit()
            .putString(KEY_MONTH_KEY, getCurrentMonthKey())
            .putInt(KEY_SCANS_USED, 0)
            .apply()
    }
}
