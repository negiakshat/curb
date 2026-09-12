package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.detection.LocalSignCrop
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
import com.example.data.model.InAppNotification
import com.example.data.model.ParkingSpot
import com.example.data.model.SampleSignPreset
import com.example.data.model.SavedPlace
import com.example.data.model.ScanResult
import com.example.data.model.SignBoundingBox
import com.example.data.model.UserProfile
import com.example.data.remote.SubscriptionPackageInfo
import com.example.data.remote.SubscriptionService
import com.example.data.remote.SubscriptionUiState
import com.example.data.remote.WalkingRoute
import com.example.data.repository.CurbRepository
import com.example.viewmodel.coordinators.ChatCoordinator
import com.example.viewmodel.coordinators.LocationSpotCoordinator
import com.example.viewmodel.coordinators.NotificationCoordinator
import com.example.viewmodel.coordinators.ProfileCoordinator
import com.example.viewmodel.coordinators.ScanCoordinator
import com.example.viewmodel.coordinators.UsageCoordinator
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

    // Extracted Coordinators
    val profileCoordinator = ProfileCoordinator(
        repository = repository,
        sessionPreferences = sessionPreferences,
        scanUsageManager = scanUsageManager,
        chatUsageManager = chatUsageManager,
        coroutineScope = viewModelScope
    )

    val locationSpotCoordinator = LocationSpotCoordinator(
        repository = repository,
        locationService = locationService,
        coroutineScope = viewModelScope
    )

    val allScans: StateFlow<List<ScanResult>> = repository.allScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession: StateFlow<ActiveParkingSession?> = repository.activeSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSessions: StateFlow<List<ActiveParkingSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationCoordinator = NotificationCoordinator(
        sessionPreferences = sessionPreferences,
        activeSession = activeSession,
        allSessions = allSessions,
        coroutineScope = viewModelScope
    )

    val scanCoordinator = ScanCoordinator(
        application = application,
        repository = repository,
        scanUsageManager = scanUsageManager,
        locationService = locationService,
        userLocationState = locationSpotCoordinator.userLocationState,
        coroutineScope = viewModelScope
    )

    val chatCoordinator = ChatCoordinator(
        chatUsageManager = chatUsageManager,
        sessionPreferences = sessionPreferences,
        coroutineScope = viewModelScope
    )

    val usageCoordinator = UsageCoordinator(
        scanUsageManager = scanUsageManager,
        chatUsageManager = chatUsageManager,
        subscriptionService = subscriptionService,
        sessionPreferences = sessionPreferences,
        isJudgeProActiveFlow = profileCoordinator.isJudgeProActive,
        userProfileFlow = profileCoordinator.userProfile,
        coroutineScope = viewModelScope
    )

    // Exposed State Delegates
    val userLocationState: StateFlow<UserLocationResult> = locationSpotCoordinator.userLocationState
    val currentUserLocation: StateFlow<UserLocationResult?> = locationSpotCoordinator.currentUserLocation
    val inAppNotifications: StateFlow<List<InAppNotification>> = notificationCoordinator.inAppNotifications
    val hasUnreadNotifications: StateFlow<Boolean> = notificationCoordinator.hasUnreadNotifications
    val showNotificationDialog: StateFlow<Boolean> = notificationCoordinator.showNotificationDialog
    val isSavingParkingSpot: StateFlow<Boolean> = locationSpotCoordinator.isSavingParkingSpot
    val parkingSpotSaveError: StateFlow<String?> = locationSpotCoordinator.parkingSpotSaveError
    val walkingRouteState: StateFlow<WalkingRoute?> = locationSpotCoordinator.walkingRouteState

    val userProfile: StateFlow<UserProfile> = profileCoordinator.userProfile
    val isJudgeProActive: StateFlow<Boolean> = profileCoordinator.isJudgeProActive
    val onboardingCompleted: StateFlow<Boolean> = profileCoordinator.onboardingCompleted

    val subscriptionState: StateFlow<SubscriptionUiState> = usageCoordinator.subscriptionState
    val isUserPro: StateFlow<Boolean> = usageCoordinator.isUserPro
    val scanUsageInfo: StateFlow<ScanUsageInfo> = usageCoordinator.scanUsageInfo
    val chatUsageInfo: StateFlow<ChatUsageInfo> = usageCoordinator.chatUsageInfo

    val currentScanResult: StateFlow<ScanResult?> = scanCoordinator.currentScanResult
    val isProcessingScan: StateFlow<Boolean> = scanCoordinator.isProcessingScan
    val processingStatusText: StateFlow<String> = scanCoordinator.processingStatusText

    val chatMessages: StateFlow<List<ChatMessage>> = chatCoordinator.chatMessages
    val isChatLoading: StateFlow<Boolean> = chatCoordinator.isChatLoading

    val savedParkingSpot: StateFlow<ParkingSpot?> = repository.savedParkingSpot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val demoSavedParkingSpot: StateFlow<ParkingSpot?> = repository.demoSavedParkingSpot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val savedPlaces: StateFlow<List<SavedPlace>> = repository.savedPlaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<CurbNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _targetSession = MutableStateFlow<ActiveParkingSession?>(null)
    val targetSession: StateFlow<ActiveParkingSession?> = _targetSession.asStateFlow()

    init {
        // Initialize RevenueCat with anonymous app user ID
        subscriptionService.initialize { isProActive ->
            val updated = profileCoordinator.userProfile.value.copy(isPro = isProActive)
            profileCoordinator.updateUserProfile(updated)
            usageCoordinator.refreshUsageInfo()
        }
    }

    // Target Session Methods
    fun loadSessionById(sessionId: Long) {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            if (session != null) {
                _targetSession.value = session
            }
        }
    }

    fun clearTargetSession() {
        _targetSession.value = null
    }

    // Notification Methods
    fun markNotificationsAsRead() = notificationCoordinator.markNotificationsAsRead()
    fun setShowNotificationDialog(show: Boolean) = notificationCoordinator.setShowNotificationDialog(show)

    // Location & Spot Methods
    fun refreshLocation() = locationSpotCoordinator.refreshLocation()
    fun refreshCurrentLocation(onResult: (UserLocationResult) -> Unit = {}) = locationSpotCoordinator.refreshCurrentLocation(onResult)
    fun startLiveLocationUpdates() = locationSpotCoordinator.startLiveLocationUpdates()
    fun stopLiveLocationUpdates() = locationSpotCoordinator.stopLiveLocationUpdates()
    fun updateWalkingRouteIfNeeded(userLat: Double, userLng: Double, carLat: Double, carLng: Double) =
        locationSpotCoordinator.updateWalkingRouteIfNeeded(userLat, userLng, carLat, carLng)
    fun saveCurrentParkingSpot(sessionId: Long? = null, isDemo: Boolean? = null, onResult: (Boolean, String?) -> Unit = { _, _ -> }) =
        locationSpotCoordinator.saveCurrentParkingSpot(sessionId, isDemo, onResult)
    fun clearParkingSpotSaveError() = locationSpotCoordinator.clearParkingSpotSaveError()
    fun clearSavedParkingSpot(isDemo: Boolean = false) = locationSpotCoordinator.clearSavedParkingSpot(isDemo)

    // Profile & Account Methods
    fun setUserName(name: String) = profileCoordinator.setUserName(name)
    fun updateAccount(name: String, gender: String, email: String) = profileCoordinator.updateAccount(name, gender, email)
    fun completeOnboarding(name: String? = null, isGuest: Boolean = false) = profileCoordinator.completeOnboarding(name, isGuest)
    fun togglePushNotifications(enabled: Boolean) = profileCoordinator.togglePushNotifications(enabled)
    fun isOnboardingAndPermissionsCompleted(): Boolean = sessionPreferences.isOnboardingAndPermissionsCompleted()

    fun applyPromoCode(code: String): PromoCodeResult {
        return profileCoordinator.applyPromoCode(code) {
            usageCoordinator.refreshUsageInfo()
        }
    }

    fun startGuestSession(onComplete: () -> Unit = {}) {
        profileCoordinator.startGuestSession {
            scanCoordinator.resetScanState()
            chatCoordinator.resetChatMessages()
            notificationCoordinator.resetEphemeralState()
            onComplete()
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        profileCoordinator.logout {
            scanCoordinator.resetScanState()
            chatCoordinator.resetChatMessages()
            notificationCoordinator.resetEphemeralState()
            onComplete()
        }
    }

    fun deleteAccount(onComplete: () -> Unit = {}) {
        profileCoordinator.deleteAccount {
            scanCoordinator.resetScanState()
            chatCoordinator.resetChatMessages()
            notificationCoordinator.resetEphemeralState()
            usageCoordinator.updateUsageForReset(false)
            onComplete()
        }
    }

    // Usage & Pro Methods
    fun isUserPro(): Boolean = usageCoordinator.checkIsUserPro()
    fun canPerformScan(): Boolean = usageCoordinator.canPerformScan()
    fun canSendChatMessage(): Boolean = usageCoordinator.canSendChatMessage()
    fun refreshUsageInfo() = usageCoordinator.refreshUsageInfo()

    // Subscription Service Delegation
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
                refreshUsageInfo()
                onSuccess()
            },
            onError = onError
        )
    }

    fun restoreSubscriptionPurchases(onResult: (Boolean, String) -> Unit) {
        subscriptionService.restorePurchases(
            onSuccess = { isProRestored ->
                val updated = profileCoordinator.userProfile.value.copy(isPro = isProRestored)
                profileCoordinator.updateUserProfile(updated)
                refreshUsageInfo()
                if (isProRestored) {
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

    // Scan Methods
    fun setCurrentScan(scanResult: ScanResult) = scanCoordinator.setCurrentScan(scanResult)

    fun processCapturedImage(
        bitmap: Bitmap?,
        explicitLocationName: String? = null,
        explicitCityState: String? = null,
        detectionBoxes: List<SignBoundingBox> = emptyList(),
        localDetections: List<LocalSignCrop> = emptyList(),
        onPaywallRequired: () -> Unit,
        onComplete: () -> Unit
    ) {
        scanCoordinator.processCapturedImage(
            bitmap = bitmap,
            explicitLocationName = explicitLocationName,
            explicitCityState = explicitCityState,
            detectionBoxes = detectionBoxes,
            localDetections = localDetections,
            isUserPro = isUserPro(),
            onPaywallRequired = onPaywallRequired,
            onComplete = {
                usageCoordinator.refreshUsageInfo()
                onComplete()
            }
        )
    }

    fun processPresetSign(
        preset: SampleSignPreset,
        onPaywallRequired: () -> Unit = {},
        onComplete: () -> Unit
    ) {
        scanCoordinator.processPresetSign(
            preset = preset,
            isUserPro = isUserPro(),
            onPaywallRequired = onPaywallRequired,
            onComplete = {
                usageCoordinator.refreshUsageInfo()
                onComplete()
            }
        )
    }

    // Chat Methods
    fun sendChatMessage(query: String, onLimitReached: () -> Unit = {}) {
        chatCoordinator.sendChatMessage(
            query = query,
            isUserPro = isUserPro(),
            currentScan = scanCoordinator.currentScanResult.value,
            onLimitReached = onLimitReached
        )
        usageCoordinator.refreshUsageInfo()
    }

    // Parking Session Methods (delegating to CurbRepository)
    fun startParkingSession(
        scanResultId: Long = 0,
        locationName: String = "Parked Spot",
        durationMinutes: Int = 0,
        allowedUntilTime: String = "",
        notes: String = "",
        timerBasis: String = "",
        parkingRuleSummary: String = "",
        scanResult: ScanResult? = null,
        maxAllowedEndTimeMillis: Long? = null
    ) {
        viewModelScope.launch {
            repository.startParkingSession(
                scanResultId = scanResultId,
                locationName = locationName,
                durationMinutes = durationMinutes,
                allowedUntilTime = allowedUntilTime,
                notes = notes,
                timerBasis = timerBasis,
                parkingRuleSummary = parkingRuleSummary,
                scanResult = scanResult,
                maxAllowedEndTimeMillis = maxAllowedEndTimeMillis
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

    // Saved Places & Notes Methods (delegating to CurbRepository)
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
