package com.example

import com.example.data.model.DetectedSign
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.util.ParkingTimerCalculator
import com.example.util.TimerSemanticMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ParkingTimerCalculatorTest {

    private fun createBaseScanResult(
        verdict: ScanVerdict = ScanVerdict.ALLOWED,
        allowedUntilTime: String = "6:00 PM",
        timeRemaining: String = "2h 00m remaining",
        parkingRules: List<String> = listOf("2 Hour Parking 8 AM - 6 PM"),
        detectedSigns: List<DetectedSign> = listOf(
            DetectedSign(id = "sign_1", title = "Parking Sign", croppedImageUri = "data:image/jpeg;base64,valid_crop", confidence = 0.95f)
        ),
        isDemo: Boolean = true
    ): ScanResult {
        return ScanResult(
            verdict = verdict,
            statusChipText = "Updated just now",
            allowedUntilTime = allowedUntilTime,
            timeRemaining = timeRemaining,
            parkingRules = parkingRules,
            explanation = "Testing parking rules",
            zoneType = "Standard Zone",
            paymentInfo = "Free",
            vehicleApplicability = "Passenger",
            detectedSigns = detectedSigns,
            isDemo = isDemo
        )
    }

    @Test
    fun testTwoHourParking() {
        val result = createBaseScanResult(
            allowedUntilTime = "2 Hour Parking",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM")
        )
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 6, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val config = ParkingTimerCalculator.calculateConfig(result, cal.timeInMillis)

        assertTrue(config.isValidAllowed)
        assertFalse(config.isUnrestricted)
        assertEquals(120, config.calculatedMinutes)
        assertEquals("2h 00m", config.formattedDuration)
        assertEquals("2h 00m limit", config.timerBasis)
        assertEquals("12:00 PM", config.allowedUntilTimeFormatted)
    }

    @Test
    fun testOneHourParking() {
        val result = createBaseScanResult(
            allowedUntilTime = "1 Hour Parking",
            timeRemaining = "1h 00m remaining",
            parkingRules = listOf("1 Hour Parking Mon-Fri 9 AM - 5 PM")
        )
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 6, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val config = ParkingTimerCalculator.calculateConfig(result, cal.timeInMillis)

        assertTrue(config.isValidAllowed)
        assertEquals(60, config.calculatedMinutes)
        assertEquals("1h 00m", config.formattedDuration)
    }

    @Test
    fun testUnrestrictedParking() {
        val result = createBaseScanResult(
            allowedUntilTime = "No time limit",
            timeRemaining = "Unlimited",
            parkingRules = listOf("Unrestricted parking anytime")
        )
        val config = ParkingTimerCalculator.calculateConfig(result)

        assertFalse(config.canStart)
        assertTrue(config.isUnrestricted)
        assertEquals(TimerSemanticMode.UNRESTRICTED_OR_NO_VERIFIED_LIMIT, config.mode)
        assertEquals(0, config.calculatedMinutes)
        assertEquals("No fixed time limit", config.formattedDuration)
    }

    @Test
    fun testScenarioA_Verified2HourSign_TimedLimitMode() {
        val result = createBaseScanResult(
            allowedUntilTime = "2 Hour Parking",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM")
        )
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 6, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val config = ParkingTimerCalculator.calculateConfig(result, cal.timeInMillis)

        assertTrue(config.canStart)
        assertEquals(TimerSemanticMode.TIMED_LIMIT, config.mode)
        assertEquals(120, config.calculatedMinutes)
        assertEquals("2h 00m limit", config.timerBasis)
    }

    @Test
    fun testScenarioB_Verified30MinuteSign_TimedLimitMode() {
        val result = createBaseScanResult(
            allowedUntilTime = "30 Min Parking",
            timeRemaining = "30m remaining",
            parkingRules = listOf("30 Minute Parking 9 AM - 6 PM")
        )
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 6, 11, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val config = ParkingTimerCalculator.calculateConfig(result, cal.timeInMillis)

        assertTrue(config.canStart)
        assertEquals(TimerSemanticMode.TIMED_LIMIT, config.mode)
        assertEquals(30, config.calculatedMinutes)
    }

    @Test
    fun testScenarioC_ClockCutoff_ClockCutoffMode() {
        val result = createBaseScanResult(
            allowedUntilTime = "6:00 PM",
            timeRemaining = "1h 30m remaining",
            parkingRules = listOf("Parking allowed until 6:00 PM")
        )
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 7, 16, 30, 0) // Monday 4:30 PM
            set(Calendar.MILLISECOND, 0)
        }
        val config = ParkingTimerCalculator.calculateConfig(result, cal.timeInMillis)

        assertTrue(config.canStart)
        assertEquals(TimerSemanticMode.CLOCK_CUTOFF, config.mode)
        assertEquals(90, config.calculatedMinutes)
    }

    @Test
    fun testScenarioD_MeteredWithoutVerifiedDuration_MeteredMode() {
        val result = createBaseScanResult(
            allowedUntilTime = "Metered",
            timeRemaining = "--",
            parkingRules = listOf("Pay at station - meter active")
        ).copy(paymentInfo = "Pay meter")
        val config = ParkingTimerCalculator.calculateConfig(result)

        assertFalse(config.canStart)
        assertEquals(TimerSemanticMode.METERED_WITHOUT_VERIFIED_TIME_LIMIT, config.mode)
        assertEquals(0, config.calculatedMinutes)
    }

    @Test
    fun testScenarioE_UnrestrictedAllowed_UnrestrictedMode() {
        val result = createBaseScanResult(
            allowedUntilTime = "Unrestricted",
            timeRemaining = "--",
            parkingRules = listOf("No time limit")
        )
        val config = ParkingTimerCalculator.calculateConfig(result)

        assertFalse(config.canStart)
        assertEquals(TimerSemanticMode.UNRESTRICTED_OR_NO_VERIFIED_LIMIT, config.mode)
    }

    @Test
    fun testScenarioF_AmbiguousScan_AmbiguousRestrictedMode() {
        val result = createBaseScanResult(
            verdict = ScanVerdict.AMBIGUOUS,
            allowedUntilTime = "Unclear",
            parkingRules = listOf("Obstructed Signage")
        )
        val config = ParkingTimerCalculator.calculateConfig(result)

        assertFalse(config.canStart)
        assertEquals(TimerSemanticMode.AMBIGUOUS_OR_RESTRICTED, config.mode)
    }

    @Test
    fun testScenarioG_RestrictedScan_AmbiguousRestrictedMode() {
        val result = createBaseScanResult(
            verdict = ScanVerdict.RESTRICTED,
            allowedUntilTime = "No Parking",
            parkingRules = listOf("No Parking Any Time")
        )
        val config = ParkingTimerCalculator.calculateConfig(result)

        assertFalse(config.canStart)
        assertEquals(TimerSemanticMode.AMBIGUOUS_OR_RESTRICTED, config.mode)
    }

    @Test
    fun testScenarioH_StricterCutoffOverridesPostedLimit() {
        // Posted limit is 2h (120m), but clock cutoff is 4 PM (30m away from 3:30 PM)
        val result = createBaseScanResult(
            allowedUntilTime = "4:00 PM",
            timeRemaining = "30m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 4 PM", "No Parking 4 PM - 6 PM")
        )
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 6, 15, 30, 0) // 3:30 PM
            set(Calendar.MILLISECOND, 0)
        }
        val config = ParkingTimerCalculator.calculateConfig(result, cal.timeInMillis)

        assertTrue(config.canStart)
        assertEquals(TimerSemanticMode.CLOCK_CUTOFF, config.mode)
        assertEquals(30, config.calculatedMinutes)
    }

    @Test
    fun testMeteredParking() {
        val result = createBaseScanResult(
            allowedUntilTime = "6:00 PM",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Metered Parking 9 AM - 6 PM")
        )
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 6, 16, 0, 0) // 4:00 PM
            set(Calendar.MILLISECOND, 0)
        }
        val config = ParkingTimerCalculator.calculateConfig(result, cal.timeInMillis)

        assertTrue(config.isValidAllowed)
        assertEquals(120, config.calculatedMinutes) // 4 PM to 6 PM = 120 mins
        assertEquals("6:00 PM", config.allowedUntilTimeFormatted)
    }

    @Test
    fun testPermitExemptParking() {
        val result = createBaseScanResult(
            allowedUntilTime = "2 Hour Parking",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM Except Area G Permit Holders")
        )
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 6, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val config = ParkingTimerCalculator.calculateConfig(result, cal.timeInMillis)

        assertTrue(config.isValidAllowed)
        assertEquals(120, config.calculatedMinutes)
        assertEquals("2h 00m", config.formattedDuration)
    }

    @Test
    fun testRestrictionBeginningEndingDuringSession() {
        val result = createBaseScanResult(
            allowedUntilTime = "4:00 PM",
            timeRemaining = "30m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 4 PM", "No Parking 4 PM - 6 PM")
        )
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 6, 15, 30, 0) // 3:30 PM
            set(Calendar.MILLISECOND, 0)
        }
        val config = ParkingTimerCalculator.calculateConfig(result, cal.timeInMillis)

        assertTrue(config.isValidAllowed)
        assertEquals(30, config.calculatedMinutes)
        assertEquals("30m", config.formattedDuration)
        assertEquals("4:00 PM", config.allowedUntilTimeFormatted)
    }

    @Test
    fun testOvernightMidnightCase() {
        val result = createBaseScanResult(
            allowedUntilTime = "8:00 AM",
            timeRemaining = "9h 00m remaining",
            parkingRules = listOf("Overnight Parking Allowed Until 8:00 AM")
        )
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 6, 23, 0, 0) // 11:00 PM
            set(Calendar.MILLISECOND, 0)
        }
        val config = ParkingTimerCalculator.calculateConfig(result, cal.timeInMillis)

        assertTrue(config.isValidAllowed)
        assertEquals(540, config.calculatedMinutes) // 9 hours
        assertEquals("8:00 AM", config.allowedUntilTimeFormatted)
    }

    @Test
    fun testUnknownAmbiguousRule() {
        val result = createBaseScanResult(
            verdict = ScanVerdict.AMBIGUOUS,
            allowedUntilTime = "Unclear",
            parkingRules = listOf("Obstructed Signage")
        )
        val config = ParkingTimerCalculator.calculateConfig(result)

        assertFalse(config.isValidAllowed)
        assertTrue(config.isRestrictedOrAmbiguous)
        assertEquals(0, config.calculatedMinutes)
        assertEquals("Rule unclear", config.formattedDuration)
    }

    @Test
    fun testCaseA_ExplicitDurationLimitAt435PM() {
        // "1 HOUR PARKING, 8 AM–6 PM, ALL DAYS"
        // Current time: 4:35 PM
        val result = createBaseScanResult(
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "6:00 PM",
            timeRemaining = "1h 00m remaining",
            parkingRules = listOf("1 Hour Parking 8 AM - 6 PM All Days"),
            detectedSigns = listOf(
                DetectedSign(
                    id = "sign_1",
                    title = "1 Hour Parking",
                    croppedImageUri = "data:image/jpeg;base64,valid_crop",
                    confidence = 0.95f,
                    rawText = "1 HOUR PARKING 8 AM - 6 PM ALL DAYS"
                )
            ),
            isDemo = false // Real scan (not demo) to run full authority check!
        )

        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 6, 16, 35, 0) // 4:35 PM
            set(Calendar.MILLISECOND, 0)
        }
        val config = ParkingTimerCalculator.calculateConfig(result, cal.timeInMillis)

        assertTrue("Timer should be authorized since physical evidence is validated", config.canStart)
        assertEquals(TimerSemanticMode.TIMED_LIMIT, config.mode)
        assertEquals(60, config.calculatedMinutes)
        assertEquals("5:35 PM", config.allowedUntilTimeFormatted)
    }

    @Test
    fun testCaseB_ClockWindowNoDuration() {
        // "8 AM–6 PM" with no duration
        val result = createBaseScanResult(
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "6:00 PM",
            timeRemaining = "1h 25m remaining",
            parkingRules = listOf("Parking allowed until 6:00 PM"),
            detectedSigns = listOf(
                DetectedSign(
                    id = "sign_1",
                    title = "Parking sign",
                    croppedImageUri = "data:image/jpeg;base64,valid_crop",
                    confidence = 0.95f,
                    rawText = "8 AM - 6 PM"
                )
            ),
            isDemo = false
        )

        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 6, 16, 35, 0) // 4:35 PM
            set(Calendar.MILLISECOND, 0)
        }
        val config = ParkingTimerCalculator.calculateConfig(result, cal.timeInMillis)

        assertTrue(config.canStart)
        assertEquals(TimerSemanticMode.CLOCK_CUTOFF, config.mode)
        assertEquals(85, config.calculatedMinutes) // 4:35 PM to 6:00 PM = 85 mins
        assertEquals("6:00 PM", config.allowedUntilTimeFormatted)
    }

    @Test
    fun testCaseC_GeminiInferredDurationWithoutPhysicalDuration() {
        // Gemini says "you can probably park for an hour" but validated physical evidence contains no duration limit
        val result = createBaseScanResult(
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "1 Hour Parking",
            timeRemaining = "1h 00m remaining",
            parkingRules = listOf("Assumed standard parking rule"),
            detectedSigns = listOf(
                DetectedSign(
                    id = "sign_1",
                    title = "Generic board",
                    croppedImageUri = "data:image/jpeg;base64,valid_crop",
                    confidence = 0.95f,
                    rawText = "WELCOME TO PARKING ZONE" // No duration in OCR!
                )
            ),
            isDemo = false
        )

        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 6, 16, 35, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val config = ParkingTimerCalculator.calculateConfig(result, cal.timeInMillis)

        assertFalse("Timer must not start because physical sign contains no duration limit", config.canStart)
        assertEquals(TimerSemanticMode.AMBIGUOUS_OR_RESTRICTED, config.mode)
        assertEquals("Uncertain time basis", config.timerBasis)
        assertEquals("Verify physical signage", config.allowedUntilTimeFormatted)
    }

    @Test
    fun testCaseD_UnclearOrContradictorySign() {
        // Unclear/contradictory sign
        val result = createBaseScanResult(
            verdict = ScanVerdict.AMBIGUOUS,
            allowedUntilTime = "Unclear",
            timeRemaining = "--",
            parkingRules = listOf("Contradictory signage detected"),
            detectedSigns = listOf(
                DetectedSign(
                    id = "sign_1",
                    title = "Temporary sign",
                    croppedImageUri = "data:image/jpeg;base64,valid_crop",
                    confidence = 0.95f,
                    rawText = "NO PARKING",
                    isUncertain = true
                )
            ),
            isDemo = false
        )

        val config = ParkingTimerCalculator.calculateConfig(result)

        assertFalse("Unclear sign must disable the timer", config.canStart)
        assertEquals(TimerSemanticMode.AMBIGUOUS_OR_RESTRICTED, config.mode)
        assertEquals("Verify physical signage", config.allowedUntilTimeFormatted)
    }

    @Test
    fun testCaseE_PhysicalOcrWinsOverGeminiDuration() {
        // Validated OCR says "1 HOUR PARKING" but Gemini says "2 Hour Parking"
        val result = createBaseScanResult(
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "2 Hour Parking",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking allowed"),
            detectedSigns = listOf(
                DetectedSign(
                    id = "sign_1",
                    title = "1 Hour Parking",
                    croppedImageUri = "data:image/jpeg;base64,valid_crop",
                    confidence = 0.95f,
                    rawText = "1 HOUR PARKING" // 1 Hour strictly in physical OCR!
                )
            ),
            isDemo = false
        )

        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 6, 16, 35, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val config = ParkingTimerCalculator.calculateConfig(result, cal.timeInMillis)

        assertTrue(config.canStart)
        assertEquals("Physical validated sign limit of 1 hour should win", 60, config.calculatedMinutes)
        assertEquals("1h 00m limit", config.timerBasis)
        assertEquals("5:35 PM", config.allowedUntilTimeFormatted)
    }
}
