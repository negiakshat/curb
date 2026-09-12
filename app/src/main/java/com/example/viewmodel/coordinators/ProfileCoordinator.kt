package com.example.viewmodel.coordinators

import com.example.data.local.ChatUsageManager
import com.example.data.local.ScanUsageManager
import com.example.data.local.SessionPreferences
import com.example.data.model.UserProfile
import com.example.data.repository.CurbRepository
import com.example.viewmodel.PromoCodeResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileCoordinator(
    private val repository: CurbRepository,
    private val sessionPreferences: SessionPreferences,
    private val scanUsageManager: ScanUsageManager,
    private val chatUsageManager: ChatUsageManager,
    private val coroutineScope: CoroutineScope
) {
    private val _userProfile = MutableStateFlow(sessionPreferences.getUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _isJudgeProActive = MutableStateFlow(sessionPreferences.isJudgeProActive)
    val isJudgeProActive: StateFlow<Boolean> = _isJudgeProActive.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(sessionPreferences.isOnboardingAndPermissionsCompleted())
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    fun setUserName(name: String) {
        val trimmed = name.trim().ifEmpty { "Alex" }
        val updated = _userProfile.value.copy(name = trimmed)
        _userProfile.value = updated
        sessionPreferences.saveUserProfile(updated)
    }

    fun updateAccount(name: String, gender: String, email: String) {
        val updated = _userProfile.value.copy(
            name = name.trim().ifEmpty { _userProfile.value.name },
            gender = gender,
            email = email.trim().ifEmpty { _userProfile.value.email }
        )
        _userProfile.value = updated
        sessionPreferences.saveUserProfile(updated)
    }

    fun updateUserProfile(profile: UserProfile) {
        _userProfile.value = profile
        sessionPreferences.saveUserProfile(profile)
    }

    fun completeOnboarding(name: String? = null, isGuest: Boolean = false) {
        if (!name.isNullOrBlank()) {
            setUserName(name)
        }
        sessionPreferences.isOnboardingCompleted = true
        sessionPreferences.isPermissionsCompleted = true
        sessionPreferences.isLoggedIn = true
        sessionPreferences.isGuest = isGuest
        sessionPreferences.saveUserProfile(_userProfile.value)
        _onboardingCompleted.value = true
    }

    fun togglePushNotifications(enabled: Boolean) {
        val updated = _userProfile.value.copy(pushNotificationsEnabled = enabled)
        _userProfile.value = updated
        sessionPreferences.saveUserProfile(updated)
    }

    fun applyPromoCode(code: String, onProActivated: () -> Unit = {}): PromoCodeResult {
        val trimmed = code.trim().uppercase()
        return if (trimmed == "CURB26X") {
            _isJudgeProActive.value = true
            sessionPreferences.isJudgeProActive = true
            onProActivated()
            PromoCodeResult.Success
        } else {
            PromoCodeResult.Error("Invalid promo code.")
        }
    }

    fun startGuestSession(onComplete: () -> Unit = {}) {
        coroutineScope.launch {
            sessionPreferences.isOnboardingCompleted = true
            sessionPreferences.isPermissionsCompleted = true
            sessionPreferences.isLoggedIn = true
            sessionPreferences.isGuest = true

            _onboardingCompleted.value = true
            onComplete()
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        coroutineScope.launch {
            sessionPreferences.isLoggedIn = false
            sessionPreferences.isOnboardingCompleted = false
            sessionPreferences.isPermissionsCompleted = false

            _onboardingCompleted.value = false
            onComplete()
        }
    }

    fun deleteAccount(onComplete: () -> Unit = {}) {
        coroutineScope.launch {
            repository.clearAllData()
            sessionPreferences.clearSession()
            scanUsageManager.resetUsage()
            chatUsageManager.resetUsage()

            val defaultProfile = UserProfile(name = "Alex", email = "")
            _userProfile.value = defaultProfile
            _isJudgeProActive.value = false
            _onboardingCompleted.value = false

            onComplete()
        }
    }
}
