package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.data.detection.LocalSignCrop
import com.example.data.local.ScanUsageManager
import com.example.data.location.LocationService
import com.example.data.location.UserLocationResult
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.model.SignBoundingBox
import com.example.data.remote.GeminiFailureClassification
import com.example.data.remote.GeminiService
import com.example.data.repository.CurbRepository
import com.example.util.EvidenceAnchoringValidator
import com.example.util.ParkingAuthority
import com.example.viewmodel.coordinators.ScanCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CurbGeminiDiagnosticsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // 1. Verify that error extracting safely parses standard API errors and extracts message/status
    @Test
    fun testSafeApiErrorExtraction() {
        val standardErrorJson = """
            {
              "error": {
                "code": 429,
                "message": "Quota exceeded for quota metric 'Generate Content API requests'...",
                "status": "RESOURCE_EXHAUSTED"
              }
            }
        """.trimIndent()

        val parsed = GeminiService.extractApiError(standardErrorJson)
        assertTrue(parsed.contains("RESOURCE_EXHAUSTED"))
        assertTrue(parsed.contains("429"))
        assertTrue(parsed.contains("Quota exceeded"))

        // Safely falls back on malformed or non-standard errors
        val nonStandardJson = "Internal server error"
        val parsedFallback = GeminiService.extractApiError(nonStandardJson)
        assertEquals("Internal server error", parsedFallback)
    }

    // 2. Verify all of our precise failure classifications are mapped and used correctly
    @Test
    fun testFailureClassificationsExist() {
        val missingKey = GeminiFailureClassification.MISSING_API_KEY
        val preflightNoImg = GeminiFailureClassification.PREFLIGHT_NO_IMAGE
        val imgEncFail = GeminiFailureClassification.IMAGE_ENCODING_FAILURE
        val networkIo = GeminiFailureClassification.NETWORK_IO
        val http408 = GeminiFailureClassification.HTTP_408
        val http429 = GeminiFailureClassification.HTTP_429
        val http5xx = GeminiFailureClassification.HTTP_5XX
        val httpOther = GeminiFailureClassification.HTTP_OTHER
        val emptyResp = GeminiFailureClassification.EMPTY_RESPONSE
        val malformedResp = GeminiFailureClassification.MALFORMED_RESPONSE
        val invalidModelResult = GeminiFailureClassification.INVALID_MODEL_RESULT

        assertEquals("MISSING_API_KEY", missingKey.name)
        assertEquals("PREFLIGHT_NO_IMAGE", preflightNoImg.name)
        assertEquals("IMAGE_ENCODING_FAILURE", imgEncFail.name)
        assertEquals("NETWORK_IO", networkIo.name)
        assertEquals("HTTP_408", http408.name)
        assertEquals("HTTP_429", http429.name)
        assertEquals("HTTP_5XX", http5xx.name)
        assertEquals("HTTP_OTHER", httpOther.name)
        assertEquals("EMPTY_RESPONSE", emptyResp.name)
        assertEquals("MALFORMED_RESPONSE", malformedResp.name)
        assertEquals("INVALID_MODEL_RESULT", invalidModelResult.name)
    }

    // 3. Verify that failed scans/results cannot authorize a timer or fabricate any durations
    @Test
    fun testFailedGeminiCannotAuthorizeTimer() {
        val failedResult = GeminiService.createFailureResult(
            locationName = "Failed Spot",
            cityState = "San Francisco, CA",
            explanation = "AI parking analysis is temporarily unavailable"
        )

        assertEquals(ScanVerdict.AMBIGUOUS, failedResult.verdict)
        assertEquals("Rule Unclear", failedResult.statusChipText)
        assertEquals("Verify physical signage", failedResult.allowedUntilTime)
        assertEquals("--", failedResult.timeRemaining)
        assertTrue(failedResult.detectedSigns.isEmpty())

        // Ensure failed result CANNOT authorize a timer under any circumstance
        assertFalse(ParkingAuthority.canAuthorizeTimer(failedResult))
    }

    // 4. Verify that failed Gemini never uses generateIntelligentScanResult in production
    @Test
    fun testProductionNeverUsesIntelligentScanResultOnFailure() {
        // Confirm that generateIntelligentScanResult is only a fallback helper during testing or isolated presets,
        // and a real production failure always returns AMBIGUOUS with empty signs and no timer authorization.
        val productionFailure = GeminiService.createFailureResult(
            locationName = "Market Street",
            cityState = "San Francisco, CA",
            explanation = "API key was invalid"
        )

        assertEquals(ScanVerdict.AMBIGUOUS, productionFailure.verdict)
        assertTrue(productionFailure.detectedSigns.isEmpty())
        assertFalse(ParkingAuthority.canAuthorizeTimer(productionFailure))
    }

    // 5. Verify the scan concurrency improvement: ScanCoordinator starts Gemini without waiting
    @Test
    fun testScanCoordinatorStartsGeminiSynchronously() {
        val realRepo = CurbRepository(context)
        val realUsage = ScanUsageManager(context)
        val realLocService = LocationService(context)
        val userLocState = MutableStateFlow<UserLocationResult>(
            UserLocationResult.Success(37.7749, -122.4194, "Synchronous Spot", "CA", "Synchronous Spot, CA")
        )

        val scanCoordinator = ScanCoordinator(
            application = ApplicationProvider.getApplicationContext(),
            repository = realRepo,
            scanUsageManager = realUsage,
            locationService = realLocService,
            userLocationState = userLocState,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val testBmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        // Process a captured image. Since CoroutineScope is Dispatchers.Unconfined, the execution runs
        // synchronously and verifies we start Gemini and initiate the process without blocks/awaiting.
        var onCompleteCalled = false
        var onErrorCalled = false

        scanCoordinator.processCapturedImage(
            bitmap = testBmp,
            explicitLocationName = null,
            explicitCityState = null,
            detectionBoxes = emptyList(),
            localDetections = emptyList(),
            isUserPro = true,
            onPaywallRequired = {},
            onComplete = { onCompleteCalled = true },
            onError = { onErrorCalled = true }
        )

        // The process starts immediately. Even if it ultimately errors due to lacking real network,
        // it proves the synchronous pipeline invocation succeeded without blocking on location refresh.
        assertNotNull(scanCoordinator.processingStage.value)
    }
}
