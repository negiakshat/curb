package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.UserProfile

class SessionPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "curb_session_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_PERMISSIONS_COMPLETED = "key_permissions_completed"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_IS_GUEST = "key_is_guest"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_GENDER = "key_user_gender"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_IS_PRO = "key_is_pro"
        private const val KEY_PUSH_NOTIFICATIONS = "key_push_notifications"
        private const val KEY_JUDGE_PRO_ACCESS = "key_judge_pro_access"
        private const val KEY_LAST_NOTIFICATION_READ_TIME = "key_last_notification_read_time"
    }

    var lastNotificationReadTime: Long
        get() = try { prefs.getLong(KEY_LAST_NOTIFICATION_READ_TIME, 0L) } catch (_: Throwable) { 0L }
        set(value) { try { prefs.edit().putLong(KEY_LAST_NOTIFICATION_READ_TIME, value).apply() } catch (_: Throwable) {} }

    var isJudgeProActive: Boolean
        get() = try { prefs.getBoolean(KEY_JUDGE_PRO_ACCESS, false) } catch (_: Throwable) { false }
        set(value) { try { prefs.edit().putBoolean(KEY_JUDGE_PRO_ACCESS, value).apply() } catch (_: Throwable) {} }

    var isOnboardingCompleted: Boolean
        get() = try { prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false) } catch (_: Throwable) { false }
        set(value) { try { prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply() } catch (_: Throwable) {} }

    var isPermissionsCompleted: Boolean
        get() = try { prefs.getBoolean(KEY_PERMISSIONS_COMPLETED, false) } catch (_: Throwable) { false }
        set(value) { try { prefs.edit().putBoolean(KEY_PERMISSIONS_COMPLETED, value).apply() } catch (_: Throwable) {} }

    var isLoggedIn: Boolean
        get() = try { prefs.getBoolean(KEY_IS_LOGGED_IN, false) } catch (_: Throwable) { false }
        set(value) { try { prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply() } catch (_: Throwable) {} }

    var isGuest: Boolean
        get() = try { prefs.getBoolean(KEY_IS_GUEST, false) } catch (_: Throwable) { false }
        set(value) { try { prefs.edit().putBoolean(KEY_IS_GUEST, value).apply() } catch (_: Throwable) {} }

    fun isOnboardingAndPermissionsCompleted(): Boolean {
        return try {
            isOnboardingCompleted && isPermissionsCompleted && isLoggedIn
        } catch (_: Throwable) {
            false
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        try {
            prefs.edit()
                .putString(KEY_USER_NAME, profile.name)
                .putString(KEY_USER_GENDER, profile.gender)
                .putString(KEY_USER_EMAIL, profile.email)
                .putBoolean(KEY_IS_PRO, profile.isPro)
                .putBoolean(KEY_PUSH_NOTIFICATIONS, profile.pushNotificationsEnabled)
                .apply()
        } catch (_: Throwable) {}
    }

    fun getUserProfile(): UserProfile {
        return try {
            val defaultName = if (isGuest) "Guest" else "Alex"
            UserProfile(
                name = prefs.getString(KEY_USER_NAME, defaultName) ?: defaultName,
                gender = prefs.getString(KEY_USER_GENDER, "Not specified") ?: "Not specified",
                email = prefs.getString(KEY_USER_EMAIL, "") ?: "",
                isPro = prefs.getBoolean(KEY_IS_PRO, false),
                pushNotificationsEnabled = prefs.getBoolean(KEY_PUSH_NOTIFICATIONS, true)
            )
        } catch (_: Throwable) {
            UserProfile(name = "Alex", gender = "Not specified", email = "", isPro = false, pushNotificationsEnabled = true)
        }
    }

    fun logout() {
        try {
            prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .putBoolean(KEY_ONBOARDING_COMPLETED, false)
                .putBoolean(KEY_IS_GUEST, false)
                .apply()
        } catch (_: Throwable) {}
    }

    fun clearSession() {
        try {
            prefs.edit().clear().apply()
        } catch (_: Throwable) {}
    }
}
