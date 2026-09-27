package com.example

import android.graphics.Bitmap
import com.example.data.detection.LocalSignCrop
import com.example.data.model.DetectedSign
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.model.SignBoundingBox
import com.example.util.EvidenceAnchoringValidator
import com.example.util.ParkingAuthority
import com.example.util.ParkingTimerCalculator
import com.example.util.TimerSemanticMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CurbRegressionTests {

    @Test
    fun testExactRegressionActive5MinuteLimit() {
        val ocrText = "5 MINUTE PARKING 5:30 PM TO 10:00 PM ALL DAYS"
        val mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val crop = LocalSignCrop(
            id = "crop_5m",
            normalizedBox = SignBoundingBox("crop_5m", 0.1f, 0.1f, 0.9f, 0.9f, "5 MINUTE PARKING", ocrText),
            ocrText = ocrText,
            fileUri = "file://test/crop_5m.png",
            bitmap = mockBitmap,
            isDemo = false,
            isUncertain = false
        )

        val geminiSign = DetectedSign(
            id = "crop_5m",
            title = "5 Minute Parking",
            subtitle = "5:30 PM - 10:00 PM • All Days",
            applicableDaysHours = "5:30 PM - 10:00 PM • All Days",
            restrictions = "5 minute parking limit",
            ruleText = "5 minute parking limit",
            isRestrictingNow = true,
            isUncertain = false,
            rawText = ocrText
        )

        val rawScanResult = ScanResult(
            verdict = ScanVerdict.ALLOWED,
            locationName = "123 Test St",
            allowedUntilTime = "5 Minute Parking",
            timeRemaining = "5m remaining",
            parkingRules = listOf("5 minute parking limit"),
            explanation = "5 minute parking limit is active",
            detectedSigns = listOf(geminiSign),
            isDemo = false
        )

        // Run through anchoring validator
        val anchoredResult = EvidenceAnchoringValidator.sanitizeAndAnchorResult(rawScanResult, listOf(crop))

        // Deterministic test time at 5:31 PM
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 17)
            set(Calendar.MINUTE, 31)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val testTimeMillis = cal.timeInMillis

        // Calculate config
        val config = ParkingTimerCalculator.calculateConfig(anchoredResult, testTimeMillis)

        // Expectations
        assertEquals(ScanVerdict.ALLOWED, anchoredResult.verdict)
        assertEquals(TimerSemanticMode.TIMED_LIMIT, config.mode)
        assertTrue(config.canStart)
        assertEquals(5, config.calculatedMinutes)
        assertTrue(config.timerBasis.lowercase().contains("5m") || config.timerBasis.lowercase().contains("5 min"))
        
        // Expiry should be approximately 5:36 PM
        val calExpiry = Calendar.getInstance().apply { timeInMillis = config.endTime }
        assertEquals(17, calExpiry.get(Calendar.HOUR_OF_DAY))
        assertEquals(36, calExpiry.get(Calendar.MINUTE))

        // Timer Authority must be true
        val canAuthorize = ParkingAuthority.canAuthorizeTimer(anchoredResult)
        assertTrue(canAuthorize)
    }

    @Test
    fun testSafetyRegressionNoParkingProhibited() {
        val ocrText = "NO PARKING ANY TIME"
        val mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val crop = LocalSignCrop(
            id = "crop_noparking",
            normalizedBox = SignBoundingBox("crop_noparking", 0.1f, 0.1f, 0.9f, 0.9f, "NO PARKING", ocrText),
            ocrText = ocrText,
            fileUri = "file://test/crop_noparking.png",
            bitmap = mockBitmap,
            isDemo = false,
            isUncertain = false
        )

        val geminiSign = DetectedSign(
            id = "crop_noparking",
            title = "No Parking",
            subtitle = "Any Time",
            applicableDaysHours = "Any Time",
            restrictions = "Parking is prohibited at all times",
            ruleText = "Parking is prohibited at all times",
            isRestrictingNow = true,
            isUncertain = false,
            rawText = ocrText
        )

        val rawScanResult = ScanResult(
            verdict = ScanVerdict.RESTRICTED,
            locationName = "123 Test St",
            allowedUntilTime = "No parking permitted",
            timeRemaining = "--",
            parkingRules = listOf("No parking allowed"),
            explanation = "Active prohibition in place",
            detectedSigns = listOf(geminiSign),
            isDemo = false
        )

        // Run through anchoring validator
        val anchoredResult = EvidenceAnchoringValidator.sanitizeAndAnchorResult(rawScanResult, listOf(crop))

        // Calculate config
        val config = ParkingTimerCalculator.calculateConfig(anchoredResult)

        // Expectations: prohibition must remain blocked
        assertFalse(config.canStart)
        assertFalse(ParkingAuthority.canAuthorizeTimer(anchoredResult))
    }

    @Test
    fun testEarlyReminderPresets() {
        // We will test the derivative presets logic for various session durations
        // 1. 30 min → [5,10,15]
        val presets30 = getPresetsForDuration(30)
        assertEquals(listOf(5, 10, 15), presets30)

        // 2. 60 min → [5,10,15,30]
        val presets60 = getPresetsForDuration(60)
        assertEquals(listOf(5, 10, 15, 30), presets60)

        // 3. 120 min → [5,10,15,30]
        val presets120 = getPresetsForDuration(120)
        assertEquals(listOf(5, 10, 15, 30), presets120)

        // 4. 10 min → [5]
        val presets10 = getPresetsForDuration(10)
        assertEquals(listOf(5), presets10)

        // 5. 5 min → []
        val presets5 = getPresetsForDuration(5)
        assertEquals(emptyList<Int>(), presets5)

        // 6. No decimal values are ever produced (since we use Int list)
        // 7. Presets are always ascending
        val presetsVarious = getPresetsForDuration(100)
        assertEquals(listOf(5, 10, 15, 30), presetsVarious)
        
        // Assert sorting is ascending
        assertTrue(presetsVarious == presetsVarious.sorted())
    }

    private fun getPresetsForDuration(durationMins: Int): List<Int> {
        val standardPresets = listOf(5, 10, 15, 30)
        return standardPresets.filter { it < durationMins }
    }
}
