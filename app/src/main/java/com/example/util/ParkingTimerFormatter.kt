package com.example.util

object ParkingTimerFormatter {

    fun formatRemainingTime(remainingMillis: Long): String {
        if (remainingMillis <= 0L) {
            return "0m"
        }

        // Rounding: Floor/truncate to total seconds so we never claim more time remains than actually does
        val totalSecs = remainingMillis / 1000L

        return if (remainingMillis >= 600000L) {
            // 10 MINUTES OR MORE: compact hour/minute style with whole standard minutes
            val totalMins = totalSecs / 60L
            val hours = totalMins / 60L
            val mins = totalMins % 60L
            if (hours > 0L) {
                String.format(java.util.Locale.US, "%dh %02dm", hours, mins)
            } else {
                String.format(java.util.Locale.US, "%dm", mins)
            }
        } else if (remainingMillis >= 60000L) {
            // BETWEEN 1 MINUTE AND 10 MINUTES: Show minutes and seconds (seconds zero-padded to two digits)
            val mins = totalSecs / 60L
            val secs = totalSecs % 60L
            String.format(java.util.Locale.US, "%dm %02ds", mins, secs)
        } else {
            // BELOW 1 MINUTE: Show seconds only (seconds zero-padded to two digits)
            String.format(java.util.Locale.US, "%02ds", totalSecs)
        }
    }
}
