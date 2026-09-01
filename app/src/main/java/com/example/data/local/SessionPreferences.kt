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
    }

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var isPermissionsCompleted: Boolean
        get() = prefs.getBoolean(KEY_PERMISSIONS_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_PERMISSIONS_COMPLETED, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var isGuest: Boolean
        get() = prefs.getBoolean(KEY_IS_GUEST, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_GUEST, value).apply()

    fun isOnboardingAndPermissionsCompleted(): Boolean {
        return isOnboardingCompleted && isPermissionsCompleted && isLoggedIn
    }

    fun saveUserProfile(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_USER_NAME, profile.name)
            .putString(KEY_USER_GENDER, profile.gender)
            .putString(KEY_USER_EMAIL, profile.email)
            .putBoolean(KEY_IS_PRO, profile.isPro)
            .putBoolean(KEY_PUSH_NOTIFICATIONS, profile.pushNotificationsEnabled)
            .apply()
    }

    fun getUserProfile(): UserProfile {
        val defaultName = if (isGuest) "Guest" else "Alex"
        val defaultEmail = if (isGuest) "guest@curbapp.com" else "alex@curbapp.com"
        return UserProfile(
            name = prefs.getString(KEY_USER_NAME, defaultName) ?: defaultName,
            gender = prefs.getString(KEY_USER_GENDER, "Not specified") ?: "Not specified",
            email = prefs.getString(KEY_USER_EMAIL, defaultEmail) ?: defaultEmail,
            isPro = prefs.getBoolean(KEY_IS_PRO, false),
            pushNotificationsEnabled = prefs.getBoolean(KEY_PUSH_NOTIFICATIONS, true)
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
