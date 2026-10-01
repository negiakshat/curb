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
import com.example.util.SemanticConsistencyValidator
import com.example.util.TimerSemanticMode
import com.example.util.ParkingTimeEvidenceBuilder
import com.example.util.ParkingTimeEvidenceType
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

    @Test
    fun testRegressionActive1MinuteLimit() {
        val ocrText = "1 MINUTE PARKING 5:30 PM TO 10:00 PM ALL DAYS"
        val mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val crop = LocalSignCrop(
            id = "crop_1m",
            normalizedBox = SignBoundingBox("crop_1m", 0.1f, 0.1f, 0.9f, 0.9f, "1 MINUTE PARKING", ocrText),
            ocrText = ocrText,
            fileUri = "file://test/crop_1m.png",
            bitmap = mockBitmap,
            isDemo = false,
            isUncertain = false
        )

        val geminiSign = DetectedSign(
            id = "crop_1m",
            title = "1 Minute Parking",
            subtitle = "5:30 PM - 10:00 PM • All Days",
            applicableDaysHours = "5:30 PM - 10:00 PM • All Days",
            restrictions = "1 minute parking limit",
            ruleText = "1 minute parking limit",
            isRestrictingNow = true,
            isUncertain = false,
            rawText = ocrText
        )

        val rawScanResult = ScanResult(
            verdict = ScanVerdict.ALLOWED,
            locationName = "123 Test St",
            allowedUntilTime = "1 Minute Parking",
            timeRemaining = "1m remaining",
            parkingRules = listOf("1 minute parking limit"),
            explanation = "1 minute parking limit is active",
            detectedSigns = listOf(geminiSign),
            isDemo = false
        )

        // Run through anchoring validator
        val anchoredResult = EvidenceAnchoringValidator.sanitizeAndAnchorResult(rawScanResult, listOf(crop))

        // Deterministic test time at 5:45 PM (17:45)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 17)
            set(Calendar.MINUTE, 45)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val testTimeMillis = cal.timeInMillis

        // Calculate config
        val config = ParkingTimerCalculator.calculateConfig(anchoredResult, testTimeMillis)

        // Expectations
        // - Sign/rule is considered active
        assertEquals(ScanVerdict.ALLOWED, anchoredResult.verdict)
        assertEquals(TimerSemanticMode.TIMED_LIMIT, config.mode)
        assertTrue(config.canStart)
        
        // - Timer calculation returns 1 minute
        assertEquals(1, config.calculatedMinutes)
        assertTrue(config.timerBasis.lowercase().contains("1m") || config.timerBasis.lowercase().contains("1 min"))

        // - Expiry should be approximately 5:46 PM
        val calExpiry = Calendar.getInstance().apply { timeInMillis = config.endTime }
        assertEquals(17, calExpiry.get(Calendar.HOUR_OF_DAY))
        assertEquals(46, calExpiry.get(Calendar.MINUTE))

        // - Timer authorization is allowed when all other evidence/authority checks pass
        val canAuthorize = ParkingAuthority.canAuthorizeTimer(anchoredResult)
        assertTrue(canAuthorize)

        // - It must NOT return "Verify physical signage" merely because the duration is only 1 minute
        assertFalse(anchoredResult.allowedUntilTime.contains("Verify physical signage"))

        // - It must NOT classify the 1-minute duration as UNKNOWN
        val evidence = ParkingTimeEvidenceBuilder.buildParkingTimeEvidence(anchoredResult, testTimeMillis)
        assertTrue(evidence.type != ParkingTimeEvidenceType.UNKNOWN)

        // - Reminder presets are separate: a 1-minute session must have NO early-reminder preset
        val presets = getPresetsForDuration(1)
        assertTrue(presets.isEmpty())
    }

    @Test
    fun testRegression1MinuteLimitActiveStartsAsRestricted() {
        // Test that 1-minute parking starting as RESTRICTED is successfully elevated/healed to ALLOWED inside active schedule
        val ocrText = "1 MINUTE PARKING 5:30 PM TO 10:00 PM ALL DAYS"
        val crop = LocalSignCrop(
            id = "crop_1m",
            normalizedBox = SignBoundingBox("crop_1m", 0.1f, 0.1f, 0.9f, 0.9f, "1 MINUTE PARKING", ocrText),
            ocrText = ocrText,
            fileUri = "file://test/crop_1m.png",
            bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888),
            isDemo = false,
            isUncertain = false
        )
        val geminiSign = DetectedSign(
            id = "crop_1m",
            title = "1 MINUTE PARKING",
            subtitle = "5:30 PM - 10:00 PM • All Days",
            applicableDaysHours = "5:30 PM - 10:00 PM • All Days",
            restrictions = "1 minute parking limit",
            ruleText = "1 minute parking limit",
            isRestrictingNow = true,
            isUncertain = false,
            rawText = ocrText
        )
        val rawResult = ScanResult(
            verdict = ScanVerdict.RESTRICTED, // Starts as RESTRICTED from Gemini
            locationName = "123 Test St",
            allowedUntilTime = "1 Minute Parking",
            timeRemaining = "--",
            parkingRules = listOf("1 minute parking limit"),
            explanation = "Extended parking is prohibited",
            detectedSigns = listOf(geminiSign),
            isDemo = false
        )

        // Deterministic test time at 5:45 PM (17:45) - inside schedule
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 17)
            set(Calendar.MINUTE, 45)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val testTimeMillis = cal.timeInMillis

        // Run through consistency gate passing custom current time
        val normalizedResult = SemanticConsistencyValidator.enforceSemanticConsistency(rawResult, listOf(crop), testTimeMillis)

        // It must be healed to ALLOWED
        assertEquals(ScanVerdict.ALLOWED, normalizedResult.verdict)
        assertEquals("Parking allowed", normalizedResult.statusChipText)

        val config = ParkingTimerCalculator.calculateConfig(normalizedResult, testTimeMillis)
        assertTrue(config.canStart)
        assertEquals(1, config.calculatedMinutes)
    }

    @Test
    fun testRegression5MinuteLimitActive() {
        val ocrText = "5 MINUTE PARKING 8:00 AM TO 6:00 PM MON-FRI"
        val crop = LocalSignCrop(
            id = "crop_5m",
            normalizedBox = SignBoundingBox("crop_5m", 0.1f, 0.1f, 0.9f, 0.9f, "5 MINUTE PARKING", ocrText),
            ocrText = ocrText,
            fileUri = "file://test/crop_5m.png",
            bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888),
            isDemo = false,
            isUncertain = false
        )
        val geminiSign = DetectedSign(
            id = "crop_5m",
            title = "5 MINUTE PARKING",
            subtitle = "8:00 AM - 6:00 PM • Mon-Fri",
            applicableDaysHours = "8:00 AM - 6:00 PM • Mon-Fri",
            restrictions = "5 minute parking limit",
            ruleText = "5 minute parking limit",
            isRestrictingNow = true,
            isUncertain = false,
            rawText = ocrText
        )
        val rawResult = ScanResult(
            verdict = ScanVerdict.RESTRICTED, // Starts as RESTRICTED
            locationName = "123 Test St",
            allowedUntilTime = "5 Minute Parking",
            timeRemaining = "--",
            parkingRules = listOf("5 minute parking limit"),
            explanation = "Extended parking is prohibited",
            detectedSigns = listOf(geminiSign),
            isDemo = false
        )

        // Deterministic test time at Wednesday 10:00 AM - inside schedule
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val testTimeMillis = cal.timeInMillis

        val normalizedResult = SemanticConsistencyValidator.enforceSemanticConsistency(rawResult, listOf(crop), testTimeMillis)
        assertEquals(ScanVerdict.ALLOWED, normalizedResult.verdict)

        val config = ParkingTimerCalculator.calculateConfig(normalizedResult, testTimeMillis)
        assertTrue(config.canStart)
        assertEquals(5, config.calculatedMinutes)
    }

    @Test
    fun testRegression2HourLimitActive() {
        val ocrText = "2 HOUR PARKING 8:00 AM TO 6:00 PM MON-FRI"
        val crop = LocalSignCrop(
            id = "crop_2h",
            normalizedBox = SignBoundingBox("crop_2h", 0.1f, 0.1f, 0.9f, 0.9f, "2 HOUR PARKING", ocrText),
            ocrText = ocrText,
            fileUri = "file://test/crop_2h.png",
            bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888),
            isDemo = false,
            isUncertain = false
        )
        val geminiSign = DetectedSign(
            id = "crop_2h",
            title = "2 HOUR PARKING",
            subtitle = "8:00 AM - 6:00 PM • Mon-Fri",
            applicableDaysHours = "8:00 AM - 6:00 PM • Mon-Fri",
            restrictions = "2 hour parking limit",
            ruleText = "2 hour parking limit",
            isRestrictingNow = true,
            isUncertain = false,
            rawText = ocrText
        )
        val rawResult = ScanResult(
            verdict = ScanVerdict.RESTRICTED, // Starts as RESTRICTED
            locationName = "123 Test St",
            allowedUntilTime = "2 Hour Parking",
            timeRemaining = "--",
            parkingRules = listOf("2 hour parking limit"),
            explanation = "Extended parking is prohibited",
            detectedSigns = listOf(geminiSign),
            isDemo = false
        )

        // Deterministic test time at Wednesday 10:00 AM - inside schedule
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val testTimeMillis = cal.timeInMillis

        val normalizedResult = SemanticConsistencyValidator.enforceSemanticConsistency(rawResult, listOf(crop), testTimeMillis)
        assertEquals(ScanVerdict.ALLOWED, normalizedResult.verdict)

        val config = ParkingTimerCalculator.calculateConfig(normalizedResult, testTimeMillis)
        assertTrue(config.canStart)
        assertEquals(120, config.calculatedMinutes)
    }

    @Test
    fun testRegressionNoParkingRemainsRestricted() {
        val ocrText = "NO PARKING 8:00 AM TO 10:00 AM TUE"
        val crop = LocalSignCrop(
            id = "crop_nopark",
            normalizedBox = SignBoundingBox("crop_nopark", 0.1f, 0.1f, 0.9f, 0.9f, "NO PARKING", ocrText),
            ocrText = ocrText,
            fileUri = "file://test/crop_nopark.png",
            bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888),
            isDemo = false,
            isUncertain = false
        )
        val geminiSign = DetectedSign(
            id = "crop_nopark",
            title = "NO PARKING",
            subtitle = "8:00 AM - 10:00 AM • Tue",
            applicableDaysHours = "8:00 AM - 10:00 AM • Tue",
            restrictions = "No parking for street cleaning",
            ruleText = "No parking for street cleaning",
            isRestrictingNow = true,
            isUncertain = false,
            rawText = ocrText
        )
        val rawResult = ScanResult(
            verdict = ScanVerdict.RESTRICTED,
            locationName = "123 Test St",
            allowedUntilTime = "No parking permitted",
            timeRemaining = "--",
            parkingRules = listOf("No parking for street cleaning"),
            explanation = "Street cleaning is active now",
            detectedSigns = listOf(geminiSign),
            isDemo = false
        )

        // Deterministic test time at Tuesday 9:00 AM - inside schedule
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val testTimeMillis = cal.timeInMillis

        val normalizedResult = SemanticConsistencyValidator.enforceSemanticConsistency(rawResult, listOf(crop), testTimeMillis)
        assertEquals(ScanVerdict.RESTRICTED, normalizedResult.verdict)
    }

    @Test
    fun testIsTimeWithinSchedule() {
        val validator = SemanticConsistencyValidator
        // Inside schedule
        val calInside = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 17)
            set(Calendar.MINUTE, 45)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertTrue(validator.isTimeWithinSchedule("5:30 PM - 10:00 PM • All Days", calInside.timeInMillis))

        // Outside schedule
        val calOutside = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertFalse(validator.isTimeWithinSchedule("5:30 PM - 10:00 PM • All Days", calOutside.timeInMillis))
    }

    @Test
    fun testRegressionTimedParkingOutsideScheduleIsRestricted() {
        val ocrText = "1 MINUTE PARKING 5:30 PM TO 10:00 PM ALL DAYS"
        val crop = LocalSignCrop(
            id = "crop_1m",
            normalizedBox = SignBoundingBox("crop_1m", 0.1f, 0.1f, 0.9f, 0.9f, "1 MINUTE PARKING", ocrText),
            ocrText = ocrText,
            fileUri = "file://test/crop_1m.png",
            bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888),
            isDemo = false,
            isUncertain = false
        )
        val geminiSign = DetectedSign(
            id = "crop_1m",
            title = "1 MINUTE PARKING",
            subtitle = "5:30 PM - 10:00 PM • All Days",
            applicableDaysHours = "5:30 PM - 10:00 PM • All Days",
            restrictions = "1 minute parking limit",
            ruleText = "1 minute parking limit",
            isRestrictingNow = true,
            isUncertain = false,
            rawText = ocrText
        )
        val rawResult = ScanResult(
            verdict = ScanVerdict.RESTRICTED,
            locationName = "123 Test St",
            allowedUntilTime = "No parking permitted",
            timeRemaining = "--",
            parkingRules = listOf("1 minute parking limit"),
            explanation = "Extended parking is prohibited",
            detectedSigns = listOf(geminiSign),
            isDemo = false
        )

        // Deterministic test time at 11:00 PM (23:00) - outside active schedule
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val testTimeMillis = cal.timeInMillis

        val normalizedResult = SemanticConsistencyValidator.enforceSemanticConsistency(rawResult, listOf(crop), testTimeMillis)
        assertEquals(ScanVerdict.RESTRICTED, normalizedResult.verdict)
    }

    private fun getPresetsForDuration(durationMins: Int): List<Int> {
        val standardPresets = listOf(5, 10, 15, 30)
        return standardPresets.filter { it < durationMins }
    }
}
