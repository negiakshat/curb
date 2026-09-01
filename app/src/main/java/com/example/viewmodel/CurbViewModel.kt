package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ScanUsageInfo
import com.example.data.local.ScanUsageManager
import com.example.data.local.SessionPreferences
import com.example.data.model.ActiveParkingSession
import com.example.data.model.ChatMessage
import com.example.data.model.CurbNote
import com.example.data.model.SampleSignPreset
import com.example.data.model.SavedPlace
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.model.UserProfile
import com.example.data.remote.GeminiService
import com.example.data.remote.SubscriptionPackageInfo
import com.example.data.remote.SubscriptionService
import com.example.data.remote.SubscriptionUiState
import com.example.data.repository.CurbRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CurbViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CurbRepository(application)
    private val sessionPreferences = SessionPreferences(application)
    private val scanUsageManager = ScanUsageManager(application)
    val subscriptionService = SubscriptionService(application)

    val allScans: StateFlow<List<ScanResult>> = repository.allScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession: StateFlow<ActiveParkingSession?> = repository.activeSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val savedPlaces: StateFlow<List<SavedPlace>> = repository.savedPlaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<CurbNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _userProfile = MutableStateFlow(sessionPreferences.getUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    val subscriptionState: StateFlow<SubscriptionUiState> = subscriptionService.subscriptionState

    private val _scanUsageInfo = MutableStateFlow(
        scanUsageManager.getUsageInfo(_userProfile.value.isPro || subscriptionState.value.isPro)
    )
    val scanUsageInfo: StateFlow<ScanUsageInfo> = _scanUsageInfo.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(sessionPreferences.isOnboardingAndPermissionsCompleted())
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _currentScanResult = MutableStateFlow<ScanResult?>(null)
    val currentScanResult: StateFlow<ScanResult?> = _currentScanResult.asStateFlow()

    private val _isProcessingScan = MutableStateFlow(false)
    val isProcessingScan: StateFlow<Boolean> = _isProcessingScan.asStateFlow()

    private val _processingStatusText = MutableStateFlow("Reading parking signs…")
    val processingStatusText: StateFlow<String> = _processingStatusText.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Hello ${sessionPreferences.getUserProfile().name}. I'm Curb AI, your dedicated parking assistant. Ask me anything about parking signs, curb colors, street cleaning schedules, or meter rules.",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _showNotificationDialog = MutableStateFlow(false)
    val showNotificationDialog: StateFlow<Boolean> = _showNotificationDialog.asStateFlow()

    init {
        // Initialize RevenueCat with anonymous app user ID
        subscriptionService.initialize { isProActive ->
            if (isProActive != _userProfile.value.isPro) {
                val updated = _userProfile.value.copy(isPro = isProActive)
                _userProfile.value = updated
                sessionPreferences.saveUserProfile(updated)
                _scanUsageInfo.value = scanUsageManager.getUsageInfo(isProActive)
            }
        }

        // Only seed sample data if first run and onboarding has not been started yet
        if (!sessionPreferences.isLoggedIn && !sessionPreferences.isOnboardingCompleted) {
            viewModelScope.launch {
                repository.seedInitialDataIfEmpty()
            }
        }
    }

    fun isUserPro(): Boolean {
        return _userProfile.value.isPro || subscriptionState.value.isPro
    }

    fun canPerformScan(): Boolean {
        return scanUsageManager.canPerformScan(isUserPro())
    }

    fun refreshUsageInfo() {
        _scanUsageInfo.value = scanUsageManager.getUsageInfo(isUserPro())
    }

    fun isOnboardingAndPermissionsCompleted(): Boolean {
        return sessionPreferences.isOnboardingAndPermissionsCompleted()
    }

    fun setUserName(name: String) {
        val trimmed = name.trim().ifEmpty { "Alex" }
        val updated = _userProfile.value.copy(name = trimmed)
        _userProfile.value = updated
        sessionPreferences.saveUserProfile(updated)
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

    fun startGuestSession(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            // 1. Wipe Room Database completely so no previous user/guest history persists
            repository.clearAllData()

            // 2. Reset SharedPreferences completely
            sessionPreferences.clearSession()
            scanUsageManager.resetUsage()

            // 3. Setup fresh guest state
            val guestProfile = UserProfile(
                name = "Guest",
                gender = "Not specified",
                email = "guest@curbapp.com",
                isPro = false,
                pushNotificationsEnabled = true
            )
            _userProfile.value = guestProfile
            sessionPreferences.isOnboardingCompleted = true
            sessionPreferences.isPermissionsCompleted = true
            sessionPreferences.isLoggedIn = true
            sessionPreferences.isGuest = true
            sessionPreferences.saveUserProfile(guestProfile)
            _scanUsageInfo.value = scanUsageManager.getUsageInfo(false)

            // 4. Reset in-memory View Model state
            _currentScanResult.value = null
            _isProcessingScan.value = false
            _processingStatusText.value = "Reading parking signs…"
            _chatMessages.value = listOf(
                ChatMessage(
                    text = "Hello! I'm Curb AI, your dedicated parking assistant. Ask me anything about parking signs, curb colors, street cleaning schedules, or meter rules.",
                    isUser = false
                )
            )
            _isChatLoading.value = false
            _showNotificationDialog.value = false
            _onboardingCompleted.value = true

            onComplete()
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            // 1. Wipe all local database tables (scans, active sessions, saved spots)
            repository.clearAllData()

            // 2. Clear all SharedPreferences session & onboarding states
            sessionPreferences.clearSession()
            scanUsageManager.resetUsage()

            // 3. Reset in-memory ViewModel states to pristine defaults
            val defaultProfile = UserProfile(name = "Alex", email = "alex@curbapp.com")
            _userProfile.value = defaultProfile
            _currentScanResult.value = null
            _isProcessingScan.value = false
            _processingStatusText.value = "Reading parking signs…"
            _chatMessages.value = listOf(
                ChatMessage(
                    text = "Hello Alex. I'm Curb AI, your dedicated parking assistant. Ask me anything about parking signs, curb colors, street cleaning schedules, or meter rules.",
                    isUser = false
                )
            )
            _isChatLoading.value = false
            _showNotificationDialog.value = false
            _onboardingCompleted.value = false
            _scanUsageInfo.value = scanUsageManager.getUsageInfo(false)

            onComplete()
        }
    }

    fun processCapturedImage(
        bitmap: Bitmap?,
        locationName: String = "Mission Street",
        onPaywallRequired: () -> Unit,
        onComplete: () -> Unit
    ) {
        val userIsPro = isUserPro()
        if (!scanUsageManager.canPerformScan(userIsPro)) {
            onPaywallRequired()
            return
        }

        viewModelScope.launch {
            _isProcessingScan.value = true
            _processingStatusText.value = "Reading parking signs…"
            delay(900)
            _processingStatusText.value = "Understanding the rules…"
            delay(900)

            val result = GeminiService.analyzeParkingSigns(bitmap, locationName)
            val scanId = repository.saveScan(result)
            val savedResult = result.copy(id = scanId)
            _currentScanResult.value = savedResult

            // Consume scan count only after successful analysis
            _scanUsageInfo.value = scanUsageManager.consumeScan(userIsPro)

            _isProcessingScan.value = false
            onComplete()
        }
    }

    fun processPresetSign(
        preset: SampleSignPreset,
        onPaywallRequired: () -> Unit = {},
        onComplete: () -> Unit
    ) {
        val userIsPro = isUserPro()
        if (!scanUsageManager.canPerformScan(userIsPro)) {
            onPaywallRequired()
            return
        }

        viewModelScope.launch {
            _isProcessingScan.value = true
            _processingStatusText.value = "Reading parking signs…"
            delay(700)
            _processingStatusText.value = "Understanding the rules…"
            delay(700)

            val scanResult = ScanResult(
                locationName = preset.locationName,
                cityState = "San Francisco, CA",
                verdict = preset.simulatedVerdict,
                statusChipText = if (preset.simulatedVerdict == ScanVerdict.ALLOWED) "Updated just now" else if (preset.simulatedVerdict == ScanVerdict.RESTRICTED) "Enforced now" else "Rule unclear",
                allowedUntilTime = preset.allowedUntil,
                timeRemaining = if (preset.simulatedVerdict == ScanVerdict.ALLOWED) "2h 15m remaining" else "0m",
                parkingRules = preset.rules,
                explanation = preset.explanation,
                detectedSigns = preset.detectedSigns,
                zoneType = "Metered parking zone",
                paymentInfo = "Pay at meter or via app"
            )
            val id = repository.saveScan(scanResult)
            val finalResult = scanResult.copy(id = id)
            _currentScanResult.value = finalResult

            _scanUsageInfo.value = scanUsageManager.consumeScan(userIsPro)

            _isProcessingScan.value = false
            onComplete()
        }
    }

    fun purchaseSubscription(
        activity: Activity,
        packageInfo: SubscriptionPackageInfo,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        subscriptionService.purchasePackage(
            activity = activity,
            packageInfo = packageInfo,
            onSuccess = {
                val updated = _userProfile.value.copy(isPro = true)
                _userProfile.value = updated
                sessionPreferences.saveUserProfile(updated)
                _scanUsageInfo.value = scanUsageManager.getUsageInfo(true)
                onSuccess()
            },
            onError = onError
        )
    }

    fun restoreSubscriptionPurchases(
        onResult: (Boolean, String) -> Unit
    ) {
        subscriptionService.restorePurchases(
            onSuccess = { isProRestored ->
                if (isProRestored) {
                    val updated = _userProfile.value.copy(isPro = true)
                    _userProfile.value = updated
                    sessionPreferences.saveUserProfile(updated)
                    _scanUsageInfo.value = scanUsageManager.getUsageInfo(true)
                    onResult(true, "Your Curb Pro subscription has been restored.")
                } else {
                    onResult(false, "No active Curb Pro purchase was found.")
                }
            },
            onError = { error ->
                onResult(false, error)
            }
        )
    }

    fun setCurrentScan(scanResult: ScanResult) {
        _currentScanResult.value = scanResult
    }

    fun startParkingSession(
        scanResultId: Long = 0,
        locationName: String = "Mission Street",
        durationMinutes: Int = 135, // 2h 15m
        allowedUntilTime: String = "11:00 AM",
        notes: String = "Mission Street spot"
    ) {
        viewModelScope.launch {
            repository.startParkingSession(
                scanResultId = scanResultId,
                locationName = locationName,
                durationMinutes = durationMinutes,
                allowedUntilTime = allowedUntilTime,
                notes = notes
            )
        }
    }

    fun endActiveParkingSession(sessionId: Long) {
        viewModelScope.launch {
            repository.endActiveSession(sessionId)
        }
    }

    fun extendParkingSession(sessionId: Long, additionalMinutes: Int, currentEndTime: Long) {
        viewModelScope.launch {
            repository.extendActiveSession(sessionId, additionalMinutes, currentEndTime)
        }
    }

    fun updateSessionReminder(sessionId: Long, reminderMinutes: Int) {
        viewModelScope.launch {
            repository.updateSessionReminder(sessionId, reminderMinutes)
        }
    }

    fun sendChatMessage(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val userMsg = ChatMessage(text = trimmed, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            val responseText = GeminiService.askParkingAssistant(trimmed, _chatMessages.value)
            val aiMsg = ChatMessage(text = responseText, isUser = false)
            _chatMessages.value = _chatMessages.value + aiMsg
            _isChatLoading.value = false
        }
    }

    fun togglePushNotifications(enabled: Boolean) {
        val updated = _userProfile.value.copy(pushNotificationsEnabled = enabled)
        _userProfile.value = updated
        sessionPreferences.saveUserProfile(updated)
    }

    fun setShowNotificationDialog(show: Boolean) {
        _showNotificationDialog.value = show
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

    fun addSavedPlace(name: String, address: String, note: String) {
        viewModelScope.launch {
            repository.addSavedPlace(
                SavedPlace(
                    name = name,
                    address = address,
                    parkingNote = note
                )
            )
        }
    }

    fun deleteSavedPlace(id: Long) {
        viewModelScope.launch {
            repository.deleteSavedPlace(id)
        }
    }

    fun getNoteFor(targetType: String, targetId: Long): CurbNote? {
        return allNotes.value.firstOrNull { it.targetType == targetType && it.targetId == targetId }
    }

    fun saveNote(targetType: String, targetId: Long, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.saveNote(targetType, targetId, trimmed)
        }
    }

    fun deleteNote(targetType: String, targetId: Long) {
        viewModelScope.launch {
            repository.deleteNote(targetType, targetId)
        }
    }

    fun deleteNoteById(id: Long) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
        }
    }
}
