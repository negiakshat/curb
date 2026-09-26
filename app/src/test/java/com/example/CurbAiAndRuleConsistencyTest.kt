package com.example

import com.example.data.model.ChatMessage
import com.example.data.model.DetectedSign
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.remote.GeminiService
import com.example.util.ParkingTimerCalculator
import com.example.util.TimerSemanticMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurbAiAndRuleConsistencyTest {

    @Test
    fun testGreetingIntentReturnsFriendlyGreetingWithoutRules() {
        val response = GeminiService.answerParkingLocally("Hello", emptyList(), null)
        assertTrue("Greeting response should start with Hello", response.startsWith("Hello!"))
        assertFalse("Greeting should not contain scan rules", response.contains("VERDICT") || response.contains("RESTRICTED"))
    }

    @Test
    fun testThanksIntentReturnsPoliteAcknowledgment() {
        val response = GeminiService.answerParkingLocally("Thanks!", emptyList(), null)
        assertTrue("Thanks response should acknowledge politely", response.contains("welcome"))
        assertFalse("Thanks response should not contain scan rules", response.contains("VERDICT") || response.contains("RESTRICTED"))
    }

    @Test
    fun testWhyIntentExplainsRestrictionEvidence() {
        val scan = ScanResult(
            locationName = "123 Main St",
            verdict = ScanVerdict.RESTRICTED,
            explanation = "Street sweeping restriction active",
            detectedSigns = listOf(
                DetectedSign(
                    id = "sign_1",
                    title = "TOW AWAY NO PARKING",
                    restrictions = "Street Sweeping 8 AM - 10 AM TUE",
                    subtitle = "8 AM - 10 AM TUE",
                    isRestrictingNow = true
                )
            )
        )

        val response = GeminiService.answerParkingLocally("why?", emptyList(), scan)
        assertTrue("Why response should explain restriction evidence", response.contains("restricted") || response.contains("TOW AWAY"))
    }

    @Test
    fun testRecurringScheduleDoesNotCreateCountdown() {
        val scan = ScanResult(
            locationName = "456 Market St",
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "Verify physical signage",
            parkingRules = listOf("MON-FRI 9 AM-5 PM"),
            detectedSigns = listOf(
                DetectedSign(
                    id = "sign_1",
                    title = "PARKING SCHEDULE",
                    restrictions = "MON-FRI 9 AM-5 PM",
                    subtitle = "MON-FRI 9 AM-5 PM",
                    isRestrictingNow = false
                )
            ),
            isDemo = true
        )

        val config = ParkingTimerCalculator.calculateConfig(scan)
        assertFalse("Recurring schedule without explicit stay limit should NOT allow countdown timer", config.canStart)
        assertEquals(TimerSemanticMode.UNRESTRICTED_OR_NO_VERIFIED_LIMIT, config.mode)
        assertEquals("Parking allowed — verify physical signage", config.confirmationHeadline)
    }

    @Test
    fun testExplicitTwoHourLimitCreatesTwoHourTimer() {
        val scan = ScanResult(
            locationName = "789 Broadway",
            verdict = ScanVerdict.ALLOWED,
            allowedUntilTime = "2 Hour Limit",
            parkingRules = listOf("2 HOUR PARKING 9 AM - 5 PM MON-FRI"),
            detectedSigns = listOf(
                DetectedSign(
                    id = "sign_1",
                    title = "2 HOUR PARKING",
                    restrictions = "2 HOUR PARKING 9 AM - 5 PM MON-FRI",
                    subtitle = "2 HOUR PARKING",
                    isRestrictingNow = false
                )
            ),
            isDemo = true
        )

        val config = ParkingTimerCalculator.calculateConfig(scan)
        assertTrue("Explicit 2 hour limit SHOULD allow starting timer", config.canStart)
        assertEquals(TimerSemanticMode.TIMED_LIMIT, config.mode)
        assertEquals(120, config.calculatedMinutes)
        assertEquals("2h 00m limit", config.timerBasis)
    }
}
