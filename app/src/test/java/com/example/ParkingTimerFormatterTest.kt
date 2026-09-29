package com.example

import com.example.util.ParkingTimerFormatter.formatRemainingTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
}
