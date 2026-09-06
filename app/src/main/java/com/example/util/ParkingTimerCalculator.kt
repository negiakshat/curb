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

        // 2. If AMBIGUOUS or any detected sign is uncertain -> No timer allowed
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
            scanResult.detectedSigns.forEach { append(it.title).append(" ").append(it.restrictions).append(" ").append(it.subtitle).append(" ") }
        }.lowercase(Locale.US)

        val isUnrestrictedKeywords = listOf(
            "no time limit",
            "unrestricted",
            "no limit",
            "anytime free",
            "free parking with no limit",
            "unrestricted parking"
        )

        val isUnrestricted = isUnrestrictedKeywords.any { combinedText.contains(it) } ||
                scanResult.allowedUntilTime.contains("no time limit", ignoreCase = true) ||
                scanResult.allowedUntilTime.contains("unrestricted", ignoreCase = true)

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

        // 4. ALLOWED WITH TIME LIMIT OR CUTOFF
        val nowCal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }

        // Step A: Parse posted duration limit (e.g., "2 Hour Parking" -> 120 mins, "1 Hour" -> 60 mins)
        val postedLimitMinutes = parsePostedDurationLimitMinutes(combinedText)

        // Step B: Parse clock cutoff time (e.g., "6:00 PM", "4:00 PM", "18:00")
        val clockCutoffMillis = parseClockEndTime(scanResult.allowedUntilTime, combinedText, nowCal, currentTimeMillis)

        var minutesUntilClockCutoff: Int? = null
        var formattedClockCutoffStr: String? = null

        if (clockCutoffMillis != null && clockCutoffMillis > currentTimeMillis) {
            val diffMs = clockCutoffMillis - currentTimeMillis
            val mins = Math.round(diffMs / 60000.0).toInt()
            if (mins > 0) {
                minutesUntilClockCutoff = mins
                val sdfOut = SimpleDateFormat("h:mm a", Locale.US)
                formattedClockCutoffStr = sdfOut.format(Date(clockCutoffMillis))
            }
        }

        // Step C: Parse remaining time fallback from scanResult.timeRemaining
        val timeRemainingMinutes = parseRemainingMinutesFallback(scanResult.timeRemaining)

        // Step D: Candidate minutes list (take the minimum of all valid positive constraints)
        val candidateMinutesList = listOfNotNull(
            postedLimitMinutes,
            minutesUntilClockCutoff,
            timeRemainingMinutes
        ).filter { it > 0 }

        if (candidateMinutesList.isEmpty()) {
            // Cannot confidently determine a specific duration or clock cutoff -> do not start a misleading timer!
            return ParkingTimerConfig(
                isValidAllowed = false,
                isUnrestricted = false,
                isRestrictedOrAmbiguous = true,
                calculatedMinutes = 0,
                formattedDuration = "Duration unspecified",
                timerBasis = "Unspecified limit",
                allowedUntilTimeFormatted = "Verify signs",
                confirmationHeadline = "Parking duration unspecified",
                confirmationSubtext = "Curb detected that parking is allowed, but could not determine a specific time limit. Check physical signs on-site before parking.",
                ruleSummary = scanResult.parkingRules.firstOrNull() ?: "No verified parking rule has been established."
            )
        }

        // Final usable minutes is strictly capped by the strictest candidate restriction
        val finalMinutes = candidateMinutesList.minOrNull()!!

        // Calculate exact end time formatted
        val calculatedEndTimeMillis = currentTimeMillis + (finalMinutes * 60000L)
        val sdfEnd = SimpleDateFormat("h:mm a", Locale.US)
        val formattedEndTimeStr = sdfEnd.format(Date(calculatedEndTimeMillis))

        val formattedDuration = formatMinutesToDisplay(finalMinutes)

        val basisText = when {
            postedLimitMinutes != null && finalMinutes == postedLimitMinutes -> {
                "$formattedDuration limit"
            }
            formattedClockCutoffStr != null && finalMinutes == minutesUntilClockCutoff -> {
                "Allowed until $formattedClockCutoffStr"
            }
            else -> {
                "$formattedDuration limit"
            }
        }

        val headline = "Parking allowed until $formattedEndTimeStr."

        val subtext = when {
            postedLimitMinutes != null && finalMinutes == postedLimitMinutes -> {
                "Timer set for $formattedDuration based on the $formattedDuration limit."
            }
            minutesUntilClockCutoff != null && finalMinutes == minutesUntilClockCutoff -> {
                "Timer set for $formattedDuration ($finalMinutes mins remaining before the $formattedClockCutoffStr restriction cutoff)."
            }
            else -> {
                "Timer set for $formattedDuration based on validated parking rules."
            }
        }

        val ruleSummary = scanResult.parkingRules.firstOrNull() ?: "No verified parking rule has been established."

        return ParkingTimerConfig(
            isValidAllowed = true,
            isUnrestricted = false,
            isRestrictedOrAmbiguous = false,
            calculatedMinutes = finalMinutes,
            formattedDuration = formattedDuration,
            timerBasis = basisText,
            allowedUntilTimeFormatted = formattedEndTimeStr,
            confirmationHeadline = headline,
            confirmationSubtext = subtext,
            ruleSummary = ruleSummary
        )
    }

    private fun parseClockEndTime(
        allowedUntilTime: String,
        combinedText: String,
        nowCal: Calendar,
        currentTimeMillis: Long
    ): Long? {
        val candidateStrings = listOf(allowedUntilTime, combinedText)

        for (timeStr in candidateStrings) {
            if (timeStr.isBlank()) continue
            val lower = timeStr.lowercase(Locale.US)

            // Ignore duration-only or unrestricted/prohibited phrases
            if (lower.contains("no time limit") || lower.contains("unrestricted") ||
                lower.contains("prohibited") || lower.contains("unclear") || lower.contains("uncertain")) {
                continue
            }

            // Check if string is purely a duration expression (e.g. "2 hour parking", "2 hours", "30 min") without AM/PM or :
            val isDurationOnly = (lower.contains("hour") || lower.contains("hr") || lower.contains("min")) &&
                    !lower.contains("am") && !lower.contains("pm") && !lower.contains(":")
            if (isDurationOnly) {
                continue
            }

            val clean = timeStr.trim().uppercase(Locale.US)

            // Pattern 1: HH:MM AM/PM or HH AM/PM (e.g. "6:00 PM", "6 PM", "12:00 AM", "8 AM")
            val amPmMatcher = Pattern.compile("(?:UNTIL|ENDS?\\s+AT|BY)?\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(AM|PM)").matcher(clean)
            if (amPmMatcher.find()) {
                val hStr = amPmMatcher.group(1) ?: continue
                val mStr = amPmMatcher.group(2) ?: "0"
                val amPm = amPmMatcher.group(3)

                var hour = hStr.toIntOrNull() ?: continue
                val min = mStr.toIntOrNull() ?: 0

                if (hour < 1 || hour > 12 || min < 0 || min > 59) continue

                if (amPm == "PM" && hour < 12) hour += 12
                if (amPm == "AM" && hour == 12) hour = 0

                val targetCal = nowCal.clone() as Calendar
                targetCal.set(Calendar.HOUR_OF_DAY, hour)
                targetCal.set(Calendar.MINUTE, min)
                targetCal.set(Calendar.SECOND, 0)
                targetCal.set(Calendar.MILLISECOND, 0)

                if (targetCal.timeInMillis <= currentTimeMillis - 1800000L) {
                    // Crossing midnight or target clock cutoff is tomorrow
                    targetCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                if (targetCal.timeInMillis > currentTimeMillis) {
                    return targetCal.timeInMillis
                }
            }

            // Pattern 2: 24-hour military clock (e.g. "18:00", "08:00")
            val militaryMatcher = Pattern.compile("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b").matcher(clean)
            if (militaryMatcher.find()) {
                val hour = militaryMatcher.group(1)?.toIntOrNull() ?: continue
                val min = militaryMatcher.group(2)?.toIntOrNull() ?: 0

                val targetCal = nowCal.clone() as Calendar
                targetCal.set(Calendar.HOUR_OF_DAY, hour)
                targetCal.set(Calendar.MINUTE, min)
                targetCal.set(Calendar.SECOND, 0)
                targetCal.set(Calendar.MILLISECOND, 0)

                if (targetCal.timeInMillis <= currentTimeMillis - 1800000L) {
                    targetCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                if (targetCal.timeInMillis > currentTimeMillis) {
                    return targetCal.timeInMillis
                }
            }
        }

        return null
    }

    private fun parsePostedDurationLimitMinutes(text: String): Int? {
        val lower = text.lowercase(Locale.US)
        var lowestMinutes: Int? = null

        // Match hour patterns like "2 hour", "2-hour", "2 hr", "2 hrs", "1h", "2h"
        val hourMatcher = Pattern.compile("(\\d+)\\s*(?:-?\\s*hour|hr|hrs|h|\\-hour)").matcher(lower)
        while (hourMatcher.find()) {
            val hrs = hourMatcher.group(1)?.toIntOrNull()
            if (hrs != null && hrs in 1..12) { // Sanity check: posted limits are 1-12 hours
                val mins = hrs * 60
                if (lowestMinutes == null || mins < lowestMinutes) {
                    lowestMinutes = mins
                }
            }
        }

        // Match minute patterns like "30 min", "15 minute", "45 mins", "30m"
        val minMatcher = Pattern.compile("(\\d+)\\s*(?:-?\\s*min|minute|mins|minutes|m)").matcher(lower)
        while (minMatcher.find()) {
            val mins = minMatcher.group(1)?.toIntOrNull()
            if (mins != null && mins in 5..300) {
                if (lowestMinutes == null || mins < lowestMinutes) {
                    lowestMinutes = mins
                }
            }
        }

        return lowestMinutes
    }

    private fun parseRemainingMinutesFallback(remainingStr: String): Int? {
        if (remainingStr.isBlank()) return null
        val lower = remainingStr.lowercase(Locale.US)

        if (lower.contains("no time limit") || lower.contains("unrestricted") ||
            lower.contains("prohibited") || lower.contains("unclear")) {
            return null
        }

        var totalMins = 0
        var found = false

        val hMatch = Pattern.compile("(\\d+)\\s*h").matcher(lower)
        if (hMatch.find()) {
            val h = hMatch.group(1)?.toIntOrNull() ?: 0
            if (h in 1..12) {
                totalMins += h * 60
                found = true
            }
        }

        val mMatch = Pattern.compile("(\\d+)\\s*m").matcher(lower)
        if (mMatch.find()) {
            val m = mMatch.group(1)?.toIntOrNull() ?: 0
            if (m in 1..59) {
                totalMins += m
                found = true
            }
        }

        return if (found && totalMins in 5..720) totalMins else null
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
