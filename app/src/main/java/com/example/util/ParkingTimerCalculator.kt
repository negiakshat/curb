package com.example.util

import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ParkingTimerConfig(
    val isValidAllowed: Boolean,
    val isUnrestricted: Boolean,
    val isRestrictedOrAmbiguous: Boolean,
    val calculatedMinutes: Int,
    val formattedDuration: String,
    val timerBasis: String,
    val allowedUntilTimeFormatted: String,
    val confirmationHeadline: String,
    val confirmationSubtext: String,
    val ruleSummary: String
)

object ParkingTimerCalculator {

    fun calculateConfig(
        scanResult: ScanResult?,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): ParkingTimerConfig {
        if (scanResult == null) {
            return ParkingTimerConfig(
                isValidAllowed = false,
                isUnrestricted = false,
                isRestrictedOrAmbiguous = true,
                calculatedMinutes = 0,
                formattedDuration = "No scan",
                timerBasis = "No scan result",
                allowedUntilTimeFormatted = "",
                confirmationHeadline = "No active scan",
                confirmationSubtext = "Scan a parking sign to configure a smart parking timer.",
                ruleSummary = ""
            )
        }

        // 1. If RESTRICTED -> No timer allowed
        if (scanResult.verdict == ScanVerdict.RESTRICTED) {
            return ParkingTimerConfig(
                isValidAllowed = false,
                isUnrestricted = false,
                isRestrictedOrAmbiguous = true,
                calculatedMinutes = 0,
                formattedDuration = "Parking restricted",
                timerBasis = "Active restriction",
                allowedUntilTimeFormatted = "Prohibited",
                confirmationHeadline = "Parking prohibited",
                confirmationSubtext = "An active rule prohibits parking at this spot right now.",
                ruleSummary = scanResult.parkingRules.firstOrNull() ?: "Active restriction"
            )
        }

        // 2. If AMBIGUOUS or any sign is uncertain -> No timer allowed
        val hasUncertainty = scanResult.verdict == ScanVerdict.AMBIGUOUS ||
                scanResult.detectedSigns.any { it.isUncertain }

        if (hasUncertainty) {
            return ParkingTimerConfig(
                isValidAllowed = false,
                isUnrestricted = false,
                isRestrictedOrAmbiguous = true,
                calculatedMinutes = 0,
                formattedDuration = "Rule unclear",
                timerBasis = "Uncertain signage",
                allowedUntilTimeFormatted = "Uncertain",
                confirmationHeadline = "Parking rule is uncertain",
                confirmationSubtext = "Curb could not establish the active rule with certainty. Verify physical signs on-site before parking.",
                ruleSummary = "Uncertain signage"
            )
        }

        // 3. ALLOWED -> Check if Unrestricted (No time limit)
        val combinedText = buildString {
            append(scanResult.allowedUntilTime)
            append(" ")
            append(scanResult.timeRemaining)
            append(" ")
            scanResult.parkingRules.forEach { append(it).append(" ") }
            scanResult.detectedSigns.forEach { append(it.title).append(" ").append(it.restrictions).append(" ") }
        }.lowercase(Locale.getDefault())

        val isUnrestrictedKeywords = listOf(
            "no time limit",
            "unrestricted",
            "no limit",
            "anytime free",
            "free parking with no limit",
            "unrestricted parking"
        )

        val isUnrestricted = isUnrestrictedKeywords.any { combinedText.contains(it) } ||
                (scanResult.allowedUntilTime.contains("no time limit", ignoreCase = true) ||
                 scanResult.allowedUntilTime.contains("unrestricted", ignoreCase = true))

        if (isUnrestricted) {
            return ParkingTimerConfig(
                isValidAllowed = true,
                isUnrestricted = true,
                isRestrictedOrAmbiguous = false,
                calculatedMinutes = 0,
                formattedDuration = "No time limit",
                timerBasis = "Unrestricted parking",
                allowedUntilTimeFormatted = "No time limit",
                confirmationHeadline = "Parking allowed with no time limit.",
                confirmationSubtext = "You can park here without time restrictions. No timer is required.",
                ruleSummary = scanResult.parkingRules.firstOrNull() ?: "Unrestricted parking"
            )
        }

        // 4. ALLOWED WITH TIME LIMIT -> Calculate usable duration based on current time & detected limits
        val nowCal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }

        // Step A: Parse clock end time (e.g., "6:00 PM", "4:00 PM", "18:00")
        val parsedClockTimeMillis = parseClockEndTime(scanResult.allowedUntilTime, nowCal)

        var minutesUntilClockCutoff: Int? = null
        var formattedClockTimeStr = scanResult.allowedUntilTime

        if (parsedClockTimeMillis != null) {
            val diffMs = parsedClockTimeMillis - currentTimeMillis
            if (diffMs > 0) {
                minutesUntilClockCutoff = (diffMs / 60000L).toInt()
            }
            val sdfOut = SimpleDateFormat("h:mm a", Locale.getDefault())
            formattedClockTimeStr = sdfOut.format(Date(parsedClockTimeMillis))
        }

        // Step B: Parse posted limit from rules or signs (e.g. "2 Hour Parking" -> 120 mins)
        val postedLimitMinutes = parsePostedDurationLimitMinutes(combinedText)

        // Step C: Determine final usable minutes
        val finalMinutes: Int = when {
            postedLimitMinutes != null && minutesUntilClockCutoff != null -> {
                minOf(postedLimitMinutes, minutesUntilClockCutoff)
            }
            postedLimitMinutes != null -> {
                postedLimitMinutes
            }
            minutesUntilClockCutoff != null -> {
                minutesUntilClockCutoff
            }
            else -> {
                parseRemainingMinutesFallback(scanResult.timeRemaining) ?: 120
            }
        }.coerceAtLeast(1)

        val formattedDuration = formatMinutesToDisplay(finalMinutes)

        val basisText = when {
            postedLimitMinutes != null && finalMinutes == postedLimitMinutes -> {
                "${formatMinutesToDisplay(postedLimitMinutes)} limit"
            }
            minutesUntilClockCutoff != null -> {
                "Allowed until $formattedClockTimeStr"
            }
            else -> {
                "Scanned $formattedDuration limit"
            }
        }

        val headline = if (formattedClockTimeStr.isNotBlank() && !formattedClockTimeStr.contains("remaining", ignoreCase = true)) {
            "Parking allowed until $formattedClockTimeStr."
        } else {
            "Parking allowed for $formattedDuration."
        }

        val subtext = when {
            postedLimitMinutes != null && finalMinutes == postedLimitMinutes -> {
                "Timer set for $formattedDuration based on the ${formatMinutesToDisplay(postedLimitMinutes)} limit."
            }
            minutesUntilClockCutoff != null && postedLimitMinutes != null && finalMinutes < postedLimitMinutes -> {
                "Timer set for $formattedDuration ($finalMinutes mins remaining before the $formattedClockTimeStr restriction cutoff)."
            }
            minutesUntilClockCutoff != null -> {
                "Timer set for $formattedDuration ($finalMinutes mins remaining until $formattedClockTimeStr)."
            }
            else -> {
                "Timer set for $formattedDuration based on detected parking rules."
            }
        }

        val ruleSummary = scanResult.parkingRules.firstOrNull() ?: "Standard parking rules apply"

        return ParkingTimerConfig(
            isValidAllowed = true,
            isUnrestricted = false,
            isRestrictedOrAmbiguous = false,
            calculatedMinutes = finalMinutes,
            formattedDuration = formattedDuration,
            timerBasis = basisText,
            allowedUntilTimeFormatted = if (formattedClockTimeStr.isNotBlank()) formattedClockTimeStr else "End of window",
            confirmationHeadline = headline,
            confirmationSubtext = subtext,
            ruleSummary = ruleSummary
        )
    }

    private fun parseClockEndTime(timeStr: String, nowCal: Calendar): Long? {
        if (timeStr.isBlank()) return null

        val clean = timeStr.trim().uppercase()
        val matcher1 = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(AM|PM)?").matcher(clean)

        if (matcher1.find()) {
            val hourStr = matcher1.group(1) ?: return null
            val minStr = matcher1.group(2) ?: "0"
            val amPmStr = matcher1.group(3)

            var hour = hourStr.toIntOrNull() ?: return null
            val min = minStr.toIntOrNull() ?: 0

            if (amPmStr == "PM" && hour < 12) hour += 12
            if (amPmStr == "AM" && hour == 12) hour = 0

            val targetCal = nowCal.clone() as Calendar
            targetCal.set(Calendar.HOUR_OF_DAY, hour)
            targetCal.set(Calendar.MINUTE, min)
            targetCal.set(Calendar.SECOND, 0)
            targetCal.set(Calendar.MILLISECOND, 0)

            if (targetCal.timeInMillis < nowCal.timeInMillis - 1800000L) {
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            return targetCal.timeInMillis
        }
        return null
    }

    private fun parsePostedDurationLimitMinutes(text: String): Int? {
        val lower = text.lowercase(Locale.getDefault())

        val hourMatcher = Pattern.compile("(\\d+)\\s*(?:hour|hr|h|\\-hour)").matcher(lower)
        if (hourMatcher.find()) {
            val hrs = hourMatcher.group(1)?.toIntOrNull()
            if (hrs != null && hrs in 1..24) {
                return hrs * 60
            }
        }

        val minMatcher = Pattern.compile("(\\d+)\\s*(?:min|minute|m)").matcher(lower)
        if (minMatcher.find()) {
            val mins = minMatcher.group(1)?.toIntOrNull()
            if (mins != null && mins in 5..300) {
                return mins
            }
        }

        return null
    }

    private fun parseRemainingMinutesFallback(remainingStr: String): Int? {
        if (remainingStr.isBlank()) return null
        val lower = remainingStr.lowercase()
        var totalMins = 0
        var found = false

        val hMatch = Pattern.compile("(\\d+)\\s*h").matcher(lower)
        if (hMatch.find()) {
            totalMins += (hMatch.group(1)?.toIntOrNull() ?: 0) * 60
            found = true
        }

        val mMatch = Pattern.compile("(\\d+)\\s*m").matcher(lower)
        if (mMatch.find()) {
            totalMins += (mMatch.group(1)?.toIntOrNull() ?: 0)
            found = true
        }

        return if (found && totalMins > 0) totalMins else null
    }

    private fun formatMinutesToDisplay(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h 00m"
            else -> "${m}m"
        }
    }
}
