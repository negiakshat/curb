package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActiveParkingSession
import com.example.data.model.ChatMessage
import com.example.data.model.SampleSignPreset
import com.example.data.model.SavedPlace
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.model.UserProfile
import com.example.data.remote.GeminiService
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

    val allScans: StateFlow<List<ScanResult>> = repository.allScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession: StateFlow<ActiveParkingSession?> = repository.activeSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val savedPlaces: StateFlow<List<SavedPlace>> = repository.savedPlaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _userProfile = MutableStateFlow(UserProfile(name = "Alex"))
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(false)
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
                text = "Hello Alex. I'm Curb AI, your dedicated parking assistant. Ask me anything about parking signs, curb colors, street cleaning schedules, or meter rules.",
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
        seedInitialSampleData()
    }

    private fun seedInitialSampleData() {
        viewModelScope.launch {
            // Seed sample places if none
            repository.addSavedPlace(SavedPlace(name = "Home", address = "742 Evergreen Terrace", parkingNote = "Residential permit required after 6 PM"))
            repository.addSavedPlace(SavedPlace(name = "Work / Office", address = "500 Howard Street", parkingNote = "2-hour metered parking 8 AM - 6 PM"))
            repository.addSavedPlace(SavedPlace(name = "Downtown", address = "Market & 4th St", parkingNote = "Tow-away zone 4 PM - 6 PM weekdays"))

            // Seed initial sample recent scans to match the product prompt specification
            val sampleScan1 = ScanResult(
                locationName = "Mission Street",
                cityState = "San Francisco, CA",
                verdict = ScanVerdict.ALLOWED,
                statusChipText = "Updated just now",
                allowedUntilTime = "6:00 PM",
                timeRemaining = "2h 15m remaining",
                parkingRules = listOf(
                    "2 Hour Parking: 8:00 AM – 6:00 PM, Mon – Fri",
                    "Street Cleaning: Tuesday & Thursday, 8:00 AM – 10:00 AM",
                    "No restrictions on weekends and city holidays"
                ),
                explanation = "Based on the signs you scanned, 2-hour parking is permitted between 8:00 AM and 6:00 PM on weekdays. Street sweeping is not active today."
            )
            repository.saveScan(sampleScan1)

            val sampleScan2 = ScanResult(
                locationName = "Broadway",
                cityState = "San Francisco, CA",
                verdict = ScanVerdict.RESTRICTED,
                statusChipText = "Restricted now",
                allowedUntilTime = "No parking permitted",
                timeRemaining = "0m",
                parkingRules = listOf(
                    "TOW-AWAY NO STOPPING: 4:00 PM – 6:00 PM, Mon – Fri",
                    "Commercial Loading Only: 9:00 AM – 4:00 PM"
                ),
                explanation = "Parking is restricted. This spot is in an active commute tow-away lane from 4:00 PM to 6:00 PM."
            )
            repository.saveScan(sampleScan2)
        }
    }

    fun setUserName(name: String) {
        val trimmed = name.trim().ifEmpty { "Alex" }
        _userProfile.value = _userProfile.value.copy(name = trimmed)
    }

    fun completeOnboarding() {
        _onboardingCompleted.value = true
    }

    fun processCapturedImage(bitmap: Bitmap?, locationName: String = "Mission Street", onComplete: () -> Unit) {
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
            _isProcessingScan.value = false
            onComplete()
        }
    }

    fun processPresetSign(preset: SampleSignPreset, onComplete: () -> Unit) {
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
            _isProcessingScan.value = false
            onComplete()
        }
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
        _userProfile.value = _userProfile.value.copy(pushNotificationsEnabled = enabled)
    }

    fun setShowNotificationDialog(show: Boolean) {
        _showNotificationDialog.value = show
    }

    fun updateAccount(name: String, gender: String, email: String) {
        _userProfile.value = _userProfile.value.copy(
            name = name.trim().ifEmpty { _userProfile.value.name },
            gender = gender,
            email = email.trim().ifEmpty { _userProfile.value.email }
        )
    }

    fun upgradeToPro() {
        _userProfile.value = _userProfile.value.copy(isPro = true)
    }

    fun restorePurchases() {
        _userProfile.value = _userProfile.value.copy(isPro = true)
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
}
