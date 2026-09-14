package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatUsageInfo(
    val messagesUsedToday: Int,
    val dailyLimit: Int = ChatUsageManager.CURB_AI_FREE_DAILY_LIMIT,
    val isPro: Boolean = false
) {
    val messagesRemaining: Int
        get() = if (isPro) Int.MAX_VALUE else (dailyLimit - messagesUsedToday).coerceAtLeast(0)

    val isLimitReached: Boolean
        get() = !isPro && messagesUsedToday >= dailyLimit

    val displayText: String
        get() = if (isPro) {
            "Unlimited AI questions"
        } else {
            val remaining = messagesRemaining
            when (remaining) {
                1 -> "1 AI question left today"
                else -> "$remaining AI questions left today"
            }
        }
}

class ChatUsageManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "curb_chat_usage_prefs"
        private const val KEY_MESSAGES_USED_TODAY = "key_messages_used_today"
        private const val KEY_USAGE_DATE_KEY = "key_usage_date_key"
        const val CURB_AI_FREE_DAILY_LIMIT = 15
    }

    private fun getCurrentDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun ensureCurrentDay() {
        try {
            val currentDay = getCurrentDateKey()
            val savedDay = prefs.getString(KEY_USAGE_DATE_KEY, null)
            if (savedDay != currentDay) {
                // New calendar day: reset daily message counter to 0
                prefs.edit()
                    .putString(KEY_USAGE_DATE_KEY, currentDay)
                    .putInt(KEY_MESSAGES_USED_TODAY, 0)
                    .apply()
            }
        } catch (_: Throwable) {}
    }

    @Synchronized
    fun getUsageInfo(isPro: Boolean): ChatUsageInfo {
        return try {
            ensureCurrentDay()
            val used = prefs.getInt(KEY_MESSAGES_USED_TODAY, 0)
            ChatUsageInfo(
                messagesUsedToday = used,
                dailyLimit = CURB_AI_FREE_DAILY_LIMIT,
                isPro = isPro
            )
        } catch (_: Throwable) {
            ChatUsageInfo(
                messagesUsedToday = 0,
                dailyLimit = CURB_AI_FREE_DAILY_LIMIT,
                isPro = isPro
            )
        }
    }

    @Synchronized
    fun canSendMessage(isPro: Boolean): Boolean {
        if (isPro) return true
        return try {
            ensureCurrentDay()
            val used = prefs.getInt(KEY_MESSAGES_USED_TODAY, 0)
            used < CURB_AI_FREE_DAILY_LIMIT
        } catch (_: Throwable) {
            true
        }
    }

    @Synchronized
    fun incrementUsage(isPro: Boolean): ChatUsageInfo {
        if (isPro) return getUsageInfo(true)
        return try {
            ensureCurrentDay()
            val current = prefs.getInt(KEY_MESSAGES_USED_TODAY, 0)
            val updated = (current + 1).coerceAtMost(CURB_AI_FREE_DAILY_LIMIT)
            prefs.edit().putInt(KEY_MESSAGES_USED_TODAY, updated).apply()
            getUsageInfo(false)
        } catch (_: Throwable) {
            getUsageInfo(false)
        }
    }

    @Synchronized
    fun resetUsage() {
        try {
            prefs.edit()
                .putString(KEY_USAGE_DATE_KEY, getCurrentDateKey())
                .putInt(KEY_MESSAGES_USED_TODAY, 0)
                .apply()
        } catch (_: Throwable) {}
    }
}
