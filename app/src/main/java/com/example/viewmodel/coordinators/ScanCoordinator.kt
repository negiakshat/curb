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

    private val _processingStatusText = MutableStateFlow("Reading your parking sign…")
    val processingStatusText: StateFlow<String> = _processingStatusText.asStateFlow()

    fun setCurrentScan(scanResult: ScanResult) {
        _currentScanResult.value = scanResult
    }

    fun canPerformScan(isUserPro: Boolean): Boolean {
        return scanUsageManager.canPerformScan(isUserPro)
    }

    fun resetScanState() {
        _currentScanResult.value = null
        _isProcessingScan.value = false
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
        onComplete: () -> Unit
    ) {
        if (_isProcessingScan.value) return

        if (!scanUsageManager.canPerformScan(isUserPro)) {
            onPaywallRequired()
            return
        }

        coroutineScope.launch {
            _isProcessingScan.value = true
            _processingStatusText.value = "Reading your parking sign…"

            try {
                val detectedCrops = if (localDetections.isNotEmpty()) {
                    Log.d("CurbPipeline", "Using pre-supplied local detections: ${localDetections.size}")
                    localDetections
                } else if (bitmap != null) {
                    Log.d("CurbPipeline", "Running on-device sign detection on captured bitmap ${bitmap.width}x${bitmap.height}")
                    val detectionResult = SignDetectionService.detectAndCropSigns(
                        application,
                        bitmap
                    )
                    if (detectionResult.signs.isNotEmpty()) {
                        detectionResult.signs
                    } else if (detectionBoxes.isNotEmpty()) {
                        SignDetectionService.cropSignsFromBoxes(
                            application,
                            bitmap,
                            detectionBoxes
                        )
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                Log.d(
                    "CurbPipeline",
                    "Final detected crops count: ${detectedCrops.size} (files: ${detectedCrops.map { it.fileUri }})"
                )

                _processingStatusText.value = "Reading your parking sign…"

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

                val result = GeminiService.analyzeParkingSigns(
                    bitmap = bitmap,
                    locationName = resolvedLocName,
                    cityState = resolvedCityState,
                    isLocationKnown = isKnown,
                    localDetections = detectedCrops,
                    context = application
                )
                val scanId = repository.saveScan(result)
                val savedResult = result.copy(id = scanId)
                _currentScanResult.value = savedResult

                scanUsageManager.consumeScan(isUserPro)
            } catch (e: Exception) {
                Log.e("CurbPipeline", "Error analyzing parking sign", e)
            } finally {
                _isProcessingScan.value = false
                onComplete()
            }
        }
    }

    fun processPresetSign(
        preset: SampleSignPreset,
        isUserPro: Boolean,
        onPaywallRequired: () -> Unit = {},
        onComplete: () -> Unit
    ) {
        if (_isProcessingScan.value) return

        if (!scanUsageManager.canPerformScan(isUserPro)) {
            onPaywallRequired()
            return
        }

        coroutineScope.launch {
            _isProcessingScan.value = true
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
            } catch (e: Exception) {
                Log.e("CurbPipeline", "Error processing preset sign", e)
            } finally {
                _isProcessingScan.value = false
                onComplete()
            }
        }
    }
}
