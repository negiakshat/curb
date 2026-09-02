package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatUsageInfo
import com.example.data.local.ChatUsageManager
import com.example.data.local.ScanUsageInfo
import com.example.data.local.ScanUsageManager
import com.example.data.local.SessionPreferences
import com.example.data.location.LocationService
import com.example.data.location.UserLocationResult
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

sealed class PromoCodeResult {
    data object Success : PromoCodeResult()
    data class Error(val message: String) : PromoCodeResult()
}

class CurbViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CurbRepository(application)
    private val sessionPreferences = SessionPreferences(application)
    private val scanUsageManager = ScanUsageManager(application)
    private val chatUsageManager = ChatUsageManager(application)
    val subscriptionService = SubscriptionService(application)
    val locationService = LocationService(application)

    private val _userLocationState = MutableStateFlow<UserLocationResult>(
        if (locationService.hasLocationPermission()) UserLocationResult.Unavailable("Checking location…")
        else UserLocationResult.PermissionRequired()
    )
    val userLocationState: StateFlow<UserLocationResult> = _userLocationState.asStateFlow()

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

    private val _isJudgeProActive = MutableStateFlow(sessionPreferences.isJudgeProActive)
    val isJudgeProActive: StateFlow<Boolean> = _isJudgeProActive.asStateFlow()

    val subscriptionState: StateFlow<SubscriptionUiState> = subscriptionService.subscriptionState

    private val _scanUsageInfo = MutableStateFlow(
        scanUsageManager.getUsageInfo(
            sessionPreferences.getUserProfile().isPro || sessionPreferences.isJudgeProActive
        )
    )
    val scanUsageInfo: StateFlow<ScanUsageInfo> = _scanUsageInfo.asStateFlow()

    private val _chatUsageInfo = MutableStateFlow(
        chatUsageManager.getUsageInfo(
            sessionPreferences.getUserProfile().isPro || sessionPreferences.isJudgeProActive
        )
    )
    val chatUsageInfo: StateFlow<ChatUsageInfo> = _chatUsageInfo.asStateFlow()

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
            val updated = _userProfile.value.copy(isPro = isProActive)
            _userProfile.value = updated
            sessionPreferences.saveUserProfile(updated)
            _scanUsageInfo.value = scanUsageManager.getUsageInfo(isUserPro())
            _chatUsageInfo.value = chatUsageManager.getUsageInfo(isUserPro())
        }

        // Fetch location if permission is already granted
        if (locationService.hasLocationPermission()) {
            refreshLocation()
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            if (!locationService.hasLocationPermission()) {
                _userLocationState.value = UserLocationResult.PermissionRequired()
                return@launch
            }
            val result = locationService.fetchCurrentLocation()
            _userLocationState.value = result
        }
    }

    fun isUserPro(): Boolean {
        return _userProfile.value.isPro || subscriptionState.value.isPro || _isJudgeProActive.value
    }

    fun applyPromoCode(code: String): PromoCodeResult {
        val trimmed = code.trim().uppercase()
        return if (trimmed == "CURB26X") {
            _isJudgeProActive.value = true
            sessionPreferences.isJudgeProActive = true
            _scanUsageInfo.value = scanUsageManager.getUsageInfo(true)
            _chatUsageInfo.value = chatUsageManager.getUsageInfo(true)
            PromoCodeResult.Success
        } else {
            PromoCodeResult.Error("Invalid promo code.")
        }
    }

    fun canPerformScan(): Boolean {
        return scanUsageManager.canPerformScan(isUserPro())
    }

    fun canSendChatMessage(): Boolean {
        return chatUsageManager.canSendMessage(isUserPro())
    }

    fun refreshUsageInfo() {
        val pro = isUserPro()
        _scanUsageInfo.value = scanUsageManager.getUsageInfo(pro)
        _chatUsageInfo.value = chatUsageManager.getUsageInfo(pro)
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
            // Logout semantics: end the current session ONLY. Local Curb data
            // (Saved Places, Scan History, Notes, Parking Sessions, User Profile)
            // is intentionally preserved across logout/login so the user keeps
            // their Curb content. We only flip the auth flags and reset
            // ephemeral in-memory UI state.

            // 1. Reset monthly usage counters (not destructive to user data)
            scanUsageManager.resetUsage()
            chatUsageManager.resetUsage()

            // 2. Set session/auth flags for guest mode
            sessionPreferences.isOnboardingCompleted = true
            sessionPreferences.isPermissionsCompleted = true
            sessionPreferences.isLoggedIn = true
            sessionPreferences.isGuest = true

            // 3. Mark session as logged-out for the gating flow
            _onboardingCompleted.value = true

            // 4. Reset ephemeral in-memory UI state only
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

            onComplete()
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            // Curb is local-first. Logging out MUST end the auth/session only.
            // It must NEVER delete legitimate user content (Saved Places,
            // Scan History, Notes, Parking Sessions, the saved UserProfile).
            //
            // What logout DOES do:
            //   - Flip auth/session flags so the splash flow routes to
            //     WELCOME / NAME_SETUP next time.
            //   - Reset ephemeral in-memory UI state (current scan, chat
            //     thread, processing indicators, notification dialog).
            //
            // What logout does NOT do (anymore):
            //   - repository.clearAllData()    // was wiping Saved Places + History
            //   - sessionPreferences.clearSession()  // was wiping user profile

            sessionPreferences.isLoggedIn = false
            sessionPreferences.isOnboardingCompleted = false
            sessionPreferences.isPermissionsCompleted = false

            _onboardingCompleted.value = false
            _currentScanResult.value = null
            _isProcessingScan.value = false
            _processingStatusText.value = "Reading parking signs…"
            _chatMessages.value = listOf(
                ChatMessage(
                    text = "Hello. I'm Curb AI, your dedicated parking assistant. Ask me anything about parking signs, curb colors, street cleaning schedules, or meter rules.",
                    isUser = false
                )
            )
            _isChatLoading.value = false
            _showNotificationDialog.value = false

            onComplete()
        }
    }

    fun processCapturedImage(
        bitmap: Bitmap?,
        explicitLocationName: String? = null,
        explicitCityState: String? = null,
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
            delay(800)
            _processingStatusText.value = "Understanding the rules…"
            delay(800)

            // Resolve location context
            val (resolvedLocName, resolvedCityState, isKnown) = if (!explicitLocationName.isNullOrBlank()) {
                // User explicitly selected a saved place or custom spot
                Triple(explicitLocationName, explicitCityState ?: "", true)
            } else {
                // Use actual device location if available, otherwise attempt fresh fetch
                val currentLoc = if (_userLocationState.value !is UserLocationResult.Success && locationService.hasLocationPermission()) {
                    locationService.fetchCurrentLocation().also { _userLocationState.value = it }
                } else {
                    _userLocationState.value
                }

                when (currentLoc) {
                    is UserLocationResult.Success -> {
                        Triple(currentLoc.locationName, currentLoc.cityState, true)
                    }
                    is UserLocationResult.PermissionRequired -> {
                        Triple("Location access needed", "", false)
                    }
                    is UserLocationResult.Unavailable -> {
                        Triple("Location unavailable", "", false)
                    }
                }
            }

            val result = GeminiService.analyzeParkingSigns(
                bitmap = bitmap,
                locationName = resolvedLocName,
                cityState = resolvedCityState,
                isLocationKnown = isKnown
            )
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
            _processingStatusText.value = "Reading sample sign…"
            delay(600)
            _processingStatusText.value = "Understanding the rules…"
            delay(600)

            val scanResult = ScanResult(
                locationName = preset.locationName,
                cityState = "",
                verdict = preset.simulatedVerdict,
                statusChipText = if (preset.simulatedVerdict == ScanVerdict.ALLOWED) "Updated just now" else if (preset.simulatedVerdict == ScanVerdict.RESTRICTED) "Enforced now" else "Rule unclear",
                allowedUntilTime = preset.allowedUntil,
                timeRemaining = if (preset.simulatedVerdict == ScanVerdict.ALLOWED) "2h 00m remaining" else "0m",
                parkingRules = preset.rules,
                explanation = preset.explanation,
                detectedSigns = preset.detectedSigns,
                zoneType = "Sample sign zone",
                paymentInfo = ""
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
                _chatUsageInfo.value = chatUsageManager.getUsageInfo(true)
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
                    _chatUsageInfo.value = chatUsageManager.getUsageInfo(true)
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
        locationName: String = "Parked Spot",
        durationMinutes: Int = 120,
        allowedUntilTime: String = "",
        notes: String = ""
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

    fun sendChatMessage(
        query: String,
        onLimitReached: () -> Unit = {}
    ) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        if (_isChatLoading.value) return

        val userIsPro = isUserPro()
        if (!chatUsageManager.canSendMessage(userIsPro)) {
            _chatUsageInfo.value = chatUsageManager.getUsageInfo(userIsPro)
            onLimitReached()
            return
        }

        val userMsg = ChatMessage(text = trimmed, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        // Increment usage allowance counter once request is submitted
        _chatUsageInfo.value = chatUsageManager.incrementUsage(userIsPro)

        viewModelScope.launch {
            try {
                val responseText = GeminiService.askParkingAssistant(trimmed, _chatMessages.value)
                val aiMsg = ChatMessage(text = responseText, isUser = false)
                _chatMessages.value = _chatMessages.value + aiMsg
            } catch (e: Exception) {
                val errMsg = ChatMessage(
                    text = "I'm having trouble connecting right now. Please try again in a moment.",
                    isUser = false
                )
                _chatMessages.value = _chatMessages.value + errMsg
            } finally {
                _isChatLoading.value = false
            }
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
