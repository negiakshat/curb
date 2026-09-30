package com.example

import com.example.util.ParkingTimerFormatter.formatRemainingTime
import com.example.util.ParkingTimerFormatter.getValidReminderPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkingTimerFormatterTest {

    @Test
    fun testFormatRemainingTime_regressionAndEdgeCases() {
        // Expiry or zero
        assertEquals("0m", formatRemainingTime(0L))
        assertEquals("0m", formatRemainingTime(-500L))

        // 1ms -> sub-second behavior (seconds-only, MUST NOT show "0m")
        val formatted1ms = formatRemainingTime(1L)
        assertNotEquals("0m", formatted1ms)
        assertEquals("00s", formatted1ms)

        // 999ms -> seconds-only format
        assertEquals("00s", formatRemainingTime(999L))

        // Precise seconds-only cases below 1 minute
        assertEquals("01s", formatRemainingTime(1000L))
        assertEquals("08s", formatRemainingTime(8000L))
        assertEquals("43s", formatRemainingTime(43000L))
        assertEquals("59s", formatRemainingTime(59000L))

        // Transition from seconds-only to minutes + seconds
        assertEquals("1m 00s", formatRemainingTime(60000L))
        assertEquals("1m 01s", formatRemainingTime(61000L))
        assertEquals("1m 32s", formatRemainingTime(92000L))
        assertEquals("5m 07s", formatRemainingTime(307000L))
        assertEquals("9m 59s", formatRemainingTime(599000L))

        // Transition to minutes-only (10 minutes or more)
        assertEquals("10m", formatRemainingTime(600000L))
        assertEquals("10m", formatRemainingTime(601000L))

        // Longer periods (Hours and minutes)
        assertEquals("1h 00m", formatRemainingTime(3600000L))
        assertEquals("1h 25m", formatRemainingTime(5100000L))
    }

    @Test
    fun testFormatRemainingTime_safetyAssertions() {
        // Verify "0m 43s" is NEVER produced
        val formatted43s = formatRemainingTime(43000L)
        assertNotEquals("0m 43s", formatted43s)
        assertEquals("43s", formatted43s)

        // Verify a 1-minute active session does not immediately display "0m"
        val formatted1Min = formatRemainingTime(60000L)
        assertNotEquals("0m", formatted1Min)
        assertEquals("1m 00s", formatted1Min)
    }

    @Test
    fun testUnificationAndPresets_regression() {
        // A. 1-minute active session with 29 seconds remaining:
        // Parking Timer = "29s", Home = "29s"
        assertEquals("29s", formatRemainingTime(29000L))

        // B. 9m 59s:
        assertEquals("9m 59s", formatRemainingTime(599000L))

        // C. 59 seconds:
        assertEquals("59s", formatRemainingTime(59000L))

        // D. 9 seconds:
        assertEquals("09s", formatRemainingTime(9000L))

        // E. 0:
        assertEquals("0m", formatRemainingTime(0L))

        // F. 1-minute parking duration (valid presets should be empty)
        val presets1m = getValidReminderPresets(1)
        assertTrue(presets1m.isEmpty())

        // G. 30-minute parking duration: valid presets = 5, 10, 15
        val presets30m = getValidReminderPresets(30)
        assertEquals(listOf(5, 10, 15), presets30m)

        // H. 1-hour parking duration: valid presets = 5, 10, 15, 30
        val presets1h = getValidReminderPresets(60)
        assertEquals(listOf(5, 10, 15, 30), presets1h)
    }
}
