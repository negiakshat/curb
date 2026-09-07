package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.detection.LocalSignCrop
import com.example.data.local.CurbDatabase
import com.example.data.local.ParkingSessionEntity
import com.example.data.local.ScanResultEntity
import com.example.data.model.SampleSignPreset
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.model.SignBoundingBox
import com.example.data.remote.GeminiService
import com.example.util.CandidateValidation
import com.example.util.SignCandidateValidator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DemoIsolationTest {

    private lateinit var database: CurbDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            CurbDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testPresetSignsHaveIsDemoTrue() {
        val presets = GeminiService.PRESET_SIGNS
        assertTrue("PRESET_SIGNS should not be empty", presets.isNotEmpty())
        for (preset in presets) {
            for (sign in preset.detectedSigns) {
                assertTrue("DetectedSign in preset ${preset.id} must have isDemo = true", sign.isDemo)
            }
        }

        val preparedPresets = GeminiService.getPreparedPresets(context)
        assertTrue("getPreparedPresets should not be empty", preparedPresets.isNotEmpty())
        for (preset in preparedPresets) {
            for (sign in preset.detectedSigns) {
                assertTrue("DetectedSign in prepared preset ${preset.id} must have isDemo = true", sign.isDemo)
            }
        }
    }

    @Test
    fun testSampleCropRejectedAsRealPhysicalCandidate() {
        val validationResult = SignCandidateValidator.validatePhysicalCandidateGeometry(
            rectLeft = 10, rectTop = 10, rectRight = 200, rectBottom = 200,
            imageWidth = 1000, imageHeight = 1000,
            ocrText = "2 HOUR PARKING 8AM TO 6PM",
            isDemo = true,
            fileUri = "/cache/sample_sign_plates/sample_2hr.jpg",
            candidateId = "preset_1"
        )
        assertFalse("Sample or demo candidate crop must be rejected", validationResult.isValid)
        assertTrue(
            "Validation result must indicate sample/demo rejection",
            (validationResult as CandidateValidation.Invalid).reason.contains("Sample or demo preset crop")
        )
    }

    @Test
    fun testGeminiServiceRejectsSampleCropAsRealEvidence() {
        val dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val sampleCrop = LocalSignCrop(
            id = "preset_crop_1",
            bitmap = dummyBitmap,
            normalizedBox = SignBoundingBox(
                id = "box_1",
                left = 0.1f, top = 0.1f, right = 0.9f, bottom = 0.9f,
                label = "2 HOUR PARKING",
                ocrText = "2 HOUR PARKING 8AM TO 6PM",
                confidence = 0.99f
            ),
            ocrText = "2 HOUR PARKING 8AM TO 6PM",
            fileUri = "/cache/sample_sign_plates/sample_2hr_metered.jpg",
            isDemo = true
        )

        val hasRealEvidence = GeminiService.hasVerifiedPhysicalSignEvidence(listOf(sampleCrop))
        assertFalse("GeminiService must reject sample/demo crop as verified physical evidence", hasRealEvidence)
    }

    @Test
    fun testDatabaseScanResultsDemoIsolation() = runBlocking {
        val scanDao = database.scanDao()

        val realScan = ScanResultEntity(
            timestamp = System.currentTimeMillis(),
            locationName = "Real Spot 123",
            cityState = "San Francisco, CA",
            verdict = "ALLOWED",
            statusChipText = "Updated just now",
            allowedUntilTime = "6:00 PM",
            timeRemaining = "2h 00m remaining",
            parkingRulesJson = "[\"2 Hour Parking\"]",
            explanation = "Real scan explanation",
            detectedSignsJson = "[]",
            zoneType = "Parking zone",
            paymentInfo = "",
            vehicleApplicability = "",
            imageUri = "",
            isDemo = false
        )

        val demoScan = ScanResultEntity(
            timestamp = System.currentTimeMillis(),
            locationName = "Demo Metered Spot",
            cityState = "",
            verdict = "ALLOWED",
            statusChipText = "Updated just now",
            allowedUntilTime = "6:00 PM",
            timeRemaining = "2h 00m remaining",
            parkingRulesJson = "[\"2 Hour Parking Preset\"]",
            explanation = "Preset demo explanation",
            detectedSignsJson = "[]",
            zoneType = "Parking zone",
            paymentInfo = "",
            vehicleApplicability = "",
            imageUri = "",
            isDemo = true
        )

        scanDao.insertScan(realScan)
        scanDao.insertScan(demoScan)

        val realScansOnly = scanDao.getAllScans().first()
        assertEquals("getAllScans should return exactly 1 real scan", 1, realScansOnly.size)
        assertEquals("Real Spot 123", realScansOnly[0].locationName)
        assertFalse("Real scan entity must have isDemo = false", realScansOnly[0].isDemo)

        val demoScansOnly = scanDao.getDemoScans().first()
        assertEquals("getDemoScans should return exactly 1 demo scan", 1, demoScansOnly.size)
        assertEquals("Demo Metered Spot", demoScansOnly[0].locationName)
        assertTrue("Demo scan entity must have isDemo = true", demoScansOnly[0].isDemo)

        scanDao.clearAllScans()
        val scansAfterClear = scanDao.getAllScans().first()
        assertTrue("Scans should be empty after clearAllScans", scansAfterClear.isEmpty())
    }

    @Test
    fun testDatabaseParkingSessionDemoIsolation() = runBlocking {
        val sessionDao = database.parkingSessionDao()

        val demoSession = ParkingSessionEntity(
            scanResultId = 100,
            locationName = "Demo Parked Spot",
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis() + 3600000,
            allowedUntilTime = "6:00 PM",
            reminderMinutesBefore = 15,
            notes = "",
            timerBasis = "[Demo] 2 Hour Limit",
            parkingRuleSummary = "Preset demo rule",
            isActive = true,
            maxAllowedEndTimeMillis = System.currentTimeMillis() + 3600000,
            isDemo = true
        )

        val realSession = ParkingSessionEntity(
            scanResultId = 101,
            locationName = "Real Parked Spot",
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis() + 7200000,
            allowedUntilTime = "7:00 PM",
            reminderMinutesBefore = 15,
            notes = "",
            timerBasis = "2 Hour Limit",
            parkingRuleSummary = "Real rule",
            isActive = true,
            maxAllowedEndTimeMillis = System.currentTimeMillis() + 7200000,
            isDemo = false
        )

        sessionDao.insertSession(demoSession)

        // getActiveSession queries WHERE isActive = 1 AND isDemo = 0
        val activeRealBefore = sessionDao.getActiveSession().first()
        assertNull("Active real session authority should be null when only demo session exists", activeRealBefore)

        val activeDemoBefore = sessionDao.getDemoActiveSession().first()
        assertNotNull("Active demo session should be returned by getDemoActiveSession", activeDemoBefore)
        assertEquals("Demo Parked Spot", activeDemoBefore?.locationName)

        // Insert real session
        sessionDao.insertSession(realSession)

        val activeRealAfter = sessionDao.getActiveSession().first()
        assertNotNull("Active real session should be returned by getActiveSession", activeRealAfter)
        assertEquals("Real Parked Spot", activeRealAfter?.locationName)
        assertFalse("Active real session must have isDemo = false", activeRealAfter!!.isDemo)

        // Ending all demo sessions must NOT touch the real active session
        sessionDao.endAllDemoSessions()

        val activeRealFinal = sessionDao.getActiveSession().first()
        assertNotNull("Real active session must remain active after endAllDemoSessions", activeRealFinal)
        assertEquals("Real Parked Spot", activeRealFinal?.locationName)

        val activeDemoFinal = sessionDao.getDemoActiveSession().first()
        assertNull("Active demo session should be ended after endAllDemoSessions", activeDemoFinal)
    }
}
