package com.example.viewmodel.coordinators

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import com.example.data.detection.LocalSignCrop
import com.example.data.detection.SignDetectionService
import com.example.data.local.ScanUsageManager
import com.example.data.location.LocationService
import com.example.data.location.UserLocationResult
import com.example.data.model.SampleSignPreset
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.model.SignBoundingBox
import com.example.data.remote.GeminiService
import com.example.data.repository.CurbRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ScanCoordinator(
    private val application: Application,
    private val repository: CurbRepository,
    private val scanUsageManager: ScanUsageManager,
    private val locationService: LocationService,
    private val userLocationState: StateFlow<UserLocationResult>,
    private val coroutineScope: CoroutineScope
) {
    private val _currentScanResult = MutableStateFlow<ScanResult?>(null)
    val currentScanResult: StateFlow<ScanResult?> = _currentScanResult.asStateFlow()

    private val _isProcessingScan = MutableStateFlow(false)
    val isProcessingScan: StateFlow<Boolean> = _isProcessingScan.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private val _processingStatusText = MutableStateFlow("Reading your parking sign…")
    val processingStatusText: StateFlow<String> = _processingStatusText.asStateFlow()

    fun setCurrentScan(scanResult: ScanResult) {
        _currentScanResult.value = scanResult
    }

    fun clearScanError() {
        _scanError.value = null
    }

    fun canPerformScan(isUserPro: Boolean): Boolean {
        return scanUsageManager.canPerformScan(isUserPro)
    }

    fun resetScanState() {
        _currentScanResult.value = null
        _isProcessingScan.value = false
        _scanError.value = null
        _processingStatusText.value = "Reading your parking sign…"
    }

    fun processCapturedImage(
        bitmap: Bitmap?,
        explicitLocationName: String? = null,
        explicitCityState: String? = null,
        detectionBoxes: List<SignBoundingBox> = emptyList(),
        localDetections: List<LocalSignCrop> = emptyList(),
        isUserPro: Boolean,
        onPaywallRequired: () -> Unit,
        onComplete: () -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        if (_isProcessingScan.value) return

        // CRITICAL ISSUE 6: bitmap == null capture failure must not save scan or consume quota
        if (bitmap == null && localDetections.isEmpty()) {
            val errorMsg = "Camera capture failed. Please ensure the sign is visible and try again."
            _scanError.value = errorMsg
            onError?.invoke(errorMsg)
            return
        }

        if (!scanUsageManager.canPerformScan(isUserPro)) {
            onPaywallRequired()
            return
        }

        coroutineScope.launch {
            _isProcessingScan.value = true
            _scanError.value = null
            _processingStatusText.value = "Reading your parking sign…"
            val totalScanStart = System.currentTimeMillis()

            try {
                // Stage 1: Fast Crop Generation / Local Sign Detection
                // CRITICAL ISSUE 1+3: Always run fresh detection on the captured bitmap.
                // Live detection boxes are UI guidance only; they must NOT bypass
                // fresh full-image OCR on the actual captured pixels.
                // Pre-supplied localDetections (e.g. from gallery) are accepted as-is
                // because they come from the same image.
                val stage1Start = System.currentTimeMillis()
                val detectedCrops = if (localDetections.isNotEmpty()) {
                    Log.d("CurbPipeline", "Using pre-supplied local detections: ${localDetections.size}")
                    localDetections
                } else if (bitmap != null) {
                    // ISSUE 1+3 FIX: Always run full on-device detection on the captured bitmap
                    // instead of carrying stale live-frame boxes forward.
                    // Live boxes (detectionBoxes) are NOT used here — they are UI hints only.
                    Log.d("CurbTiming", "Running fresh on-device OCR on captured bitmap ${bitmap.width}x${bitmap.height} (ignoring ${detectionBoxes.size} live detection boxes)")
                    val detectionResult = SignDetectionService.detectAndCropSigns(
                        application,
                        bitmap
                    )
                    detectionResult.signs
                } else {
                    emptyList()
                }

                val stage1Time = System.currentTimeMillis() - stage1Start
                Log.d("CurbTiming", "Stage 1 (Detection & Cropping) finished in $stage1Time ms. Validated crops: ${detectedCrops.size}")

                _processingStatusText.value = "Reading your parking sign…"

                // Stage 2: Location Resolution
                val stage2Start = System.currentTimeMillis()
                val (resolvedLocName, resolvedCityState, isKnown) = if (!explicitLocationName.isNullOrBlank()) {
                    Triple(explicitLocationName, explicitCityState ?: "", true)
                } else {
                    val currentLoc = if (userLocationState.value !is UserLocationResult.Success && locationService.hasLocationPermission()) {
                        locationService.fetchCurrentLocation()
                    } else {
                        userLocationState.value
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
                val stage2Time = System.currentTimeMillis() - stage2Start
                Log.d("CurbTiming", "Stage 2 (Location Resolution) finished in $stage2Time ms: $resolvedLocName")

                // Stage 3: Gemini / Evidence-Anchored Analysis
                val stage3Start = System.currentTimeMillis()
                val result = GeminiService.analyzeParkingSigns(
                    bitmap = bitmap,
                    locationName = resolvedLocName,
                    cityState = resolvedCityState,
                    isLocationKnown = isKnown,
                    localDetections = detectedCrops,
                    context = application
                )
                val stage3Time = System.currentTimeMillis() - stage3Start
                Log.d("CurbTiming", "Stage 3 (Gemini & Evidence Anchoring) finished in $stage3Time ms. Verdict: ${result.verdict}")

                // Stage 4: Database Save & Usage
                val stage4Start = System.currentTimeMillis()
                val scanId = repository.saveScan(result)
                val savedResult = result.copy(id = scanId)
                _currentScanResult.value = savedResult
                scanUsageManager.consumeScan(isUserPro)
                val stage4Time = System.currentTimeMillis() - stage4Start
                Log.d("CurbTiming", "Stage 4 (DB Save & Usage) finished in $stage4Time ms. ScanId: $scanId")

                val totalDuration = System.currentTimeMillis() - totalScanStart
                Log.d("CurbTiming", "Total Scan Pipeline completed in $totalDuration ms")

                onComplete()
            } catch (e: Exception) {
                Log.e("CurbPipeline", "Error analyzing parking sign", e)
                val errorMsg = e.message ?: "Unable to analyze parking signs. Please ensure the sign is clear and try again."
                _scanError.value = errorMsg
                onError?.invoke(errorMsg)
            } finally {
                _isProcessingScan.value = false
            }
        }
    }

    fun processPresetSign(
        preset: SampleSignPreset,
        isUserPro: Boolean,
        onPaywallRequired: () -> Unit = {},
        onComplete: () -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        if (_isProcessingScan.value) return

        if (!scanUsageManager.canPerformScan(isUserPro)) {
            onPaywallRequired()
            return
        }

        coroutineScope.launch {
            _isProcessingScan.value = true
            _scanError.value = null
            _processingStatusText.value = "Reading your parking sign…"
            try {
                delay(1000)

                val enrichedSigns = preset.detectedSigns.map { sign ->
                    val cropPath = if (!sign.croppedImageUri.isNullOrBlank() && File(sign.croppedImageUri).exists()) {
                        sign.croppedImageUri
                    } else {
                        SignDetectionService.getOrCreateSampleSignCrop(
                            application,
                            sign.id,
                            sign.title,
                            sign.subtitle,
                            sign.isRestrictingNow
                        )
                    }
                    sign.copy(croppedImageUri = cropPath, isDemo = true)
                }

                val scanResult = ScanResult(
                    locationName = preset.locationName,
                    cityState = "",
                    verdict = preset.simulatedVerdict,
                    statusChipText = if (preset.simulatedVerdict == ScanVerdict.ALLOWED) "Updated just now" else if (preset.simulatedVerdict == ScanVerdict.RESTRICTED) "Enforced now" else "Rule unclear",
                    allowedUntilTime = preset.allowedUntil,
                    timeRemaining = if (preset.simulatedVerdict == ScanVerdict.ALLOWED) "2h 00m remaining" else "0m",
                    parkingRules = preset.rules,
                    explanation = preset.explanation,
                    detectedSigns = enrichedSigns,
                    zoneType = "Parking zone",
                    paymentInfo = "",
                    isDemo = true
                )
                val id = repository.saveScan(scanResult)
                val finalResult = scanResult.copy(id = id)
                _currentScanResult.value = finalResult

                scanUsageManager.consumeScan(isUserPro)
                onComplete()
            } catch (e: Exception) {
                Log.e("CurbPipeline", "Error processing preset sign", e)
                val errorMsg = e.message ?: "Failed to process preset sign."
                _scanError.value = errorMsg
                onError?.invoke(errorMsg)
            } finally {
                _isProcessingScan.value = false
            }
        }
    }
}
