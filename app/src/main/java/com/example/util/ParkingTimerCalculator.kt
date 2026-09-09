package com.example.util

import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

enum class TimerSemanticMode {
    TIMED_LIMIT,
    CLOCK_CUTOFF,
    METERED_WITHOUT_VERIFIED_TIME_LIMIT,
    UNRESTRICTED_OR_NO_VERIFIED_LIMIT,
    AMBIGUOUS_OR_RESTRICTED
}

data class ParkingTimerConfig(
    val canStart: Boolean,
    val mode: TimerSemanticMode,
    val reason: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis(),
    val maxAllowedEndTimeMillis: Long? = null,
    val isValidAllowed: Boolean = canStart,
    val isUnrestricted: Boolean = (mode == TimerSemanticMode.UNRESTRICTED_OR_NO_VERIFIED_LIMIT),
    val isRestrictedOrAmbiguous: Boolean = (mode == TimerSemanticMode.AMBIGUOUS_OR_RESTRICTED),
    val calculatedMinutes: Int = 0,
    val formattedDuration: String = "",
    val timerBasis: String = "",
    val allowedUntilTimeFormatted: String = "",
    val confirmationHeadline: String = "",
    val confirmationSubtext: String = "",
    val ruleSummary: String = ""
)

object ParkingTimerCalculator {

    fun calculateConfig(
        scanResult: ScanResult?,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): ParkingTimerConfig {
        if (scanResult == null) {
            return ParkingTimerConfig(
                canStart = false,
                mode = TimerSemanticMode.AMBIGUOUS_OR_RESTRICTED,
                reason = "No active scan result provided.",
                startTime = currentTimeMillis,
                endTime = currentTimeMillis,
                maxAllowedEndTimeMillis = null,
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
                canStart = false,
                mode = TimerSemanticMode.AMBIGUOUS_OR_RESTRICTED,
                reason = "Parking is prohibited at this location.",
                startTime = currentTimeMillis,
                endTime = currentTimeMillis,
                maxAllowedEndTimeMillis = null,
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
                scanResult.detectedSigns.any { it.isUncertain || it.isRestrictingNow }

        if (hasUncertainty) {
            return ParkingTimerConfig(
                canStart = false,
                mode = TimerSemanticMode.AMBIGUOUS_OR_RESTRICTED,
                reason = "Parking sign rules are ambiguous or uncertain.",
                startTime = currentTimeMillis,
                endTime = currentTimeMillis,
                maxAllowedEndTimeMillis = null,
                calculatedMinutes = 0,
                formattedDuration = "Rule unclear",
                timerBasis = "Uncertain signage",
                allowedUntilTimeFormatted = "Uncertain",
                confirmationHeadline = "Parking rule is uncertain",
                confirmationSubtext = "Curb could not establish the active rule with certainty. Verify physical signs on-site before parking.",
                ruleSummary = "Uncertain signage"
            )
        }

        // Authority gate validation for non-demo scans
        if (!scanResult.isDemo) {
            if (!ParkingAuthority.canAuthorizeTimer(scanResult) || !SemanticConsistencyValidator.canAuthorizeTimer(scanResult)) {
                val combined = buildString {
                    append(scanResult.allowedUntilTime).append(" ")
                    append(scanResult.paymentInfo).append(" ")
                    scanResult.parkingRules.forEach { append(it).append(" ") }
                }.lowercase(Locale.US)
                val isPayment = (scanResult.paymentInfo.isNotBlank() &&
                        !scanResult.paymentInfo.contains("free", ignoreCase = true) &&
                        !scanResult.paymentInfo.contains("no fee", ignoreCase = true)) ||
                        combined.contains("meter") || combined.contains("pay")
                val mode = if (isPayment) TimerSemanticMode.METERED_WITHOUT_VERIFIED_TIME_LIMIT else TimerSemanticMode.UNRESTRICTED_OR_NO_VERIFIED_LIMIT

                return ParkingTimerConfig(
                    canStart = false,
                    mode = mode,
                    reason = "Scan lacks verified time-limit signage evidence.",
                    startTime = currentTimeMillis,
                    endTime = currentTimeMillis,
                    maxAllowedEndTimeMillis = null,
                    calculatedMinutes = 0,
                    formattedDuration = if (isPayment) "Pay to park" else "No time limit",
                    timerBasis = if (isPayment) "Metered parking" else "Unrestricted parking",
                    allowedUntilTimeFormatted = scanResult.allowedUntilTime,
                    confirmationHeadline = if (isPayment) "Pay to park" else "No time limit",
                    confirmationSubtext = if (isPayment) "Metered parking detected, but no verified time limit was established by signage." else "You can park here without time restrictions. No legal-limit countdown can be set.",
                    ruleSummary = scanResult.parkingRules.firstOrNull() ?: "No verified time limit"
                )
            }
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
                canStart = false,
                mode = TimerSemanticMode.UNRESTRICTED_OR_NO_VERIFIED_LIMIT,
                reason = "Unrestricted parking detected.",
                startTime = currentTimeMillis,
                endTime = currentTimeMillis,
                maxAllowedEndTimeMillis = null,
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

        val signText = buildString {
            append(scanResult.allowedUntilTime).append(" ")
            scanResult.parkingRules.forEach { append(it).append(" ") }
            scanResult.detectedSigns.forEach { append(it.title).append(" ").append(it.restrictions).append(" ").append(it.subtitle).append(" ") }
        }.lowercase(Locale.US)

        // Step A: Parse posted duration limit (e.g., "2 Hour Parking" -> 120 mins, "1 Hour" -> 60 mins)
        val postedLimitMinutes = parsePostedDurationLimitMinutes(signText)

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

        val hasPayment = (scanResult.paymentInfo.isNotBlank() &&
                !scanResult.paymentInfo.contains("free", ignoreCase = true) &&
                !scanResult.paymentInfo.contains("no fee", ignoreCase = true)) ||
                combinedText.contains("meter") || combinedText.contains("pay")

        if (postedLimitMinutes == null && minutesUntilClockCutoff == null && timeRemainingMinutes == null) {
            val mode = if (hasPayment) TimerSemanticMode.METERED_WITHOUT_VERIFIED_TIME_LIMIT else TimerSemanticMode.UNRESTRICTED_OR_NO_VERIFIED_LIMIT
            return ParkingTimerConfig(
                canStart = false,
                mode = mode,
                reason = if (hasPayment) "Metered parking detected, but no verified time limit was established." else "No verified time limit established by signage.",
                startTime = currentTimeMillis,
                endTime = currentTimeMillis,
                maxAllowedEndTimeMillis = null,
                calculatedMinutes = 0,
                formattedDuration = if (hasPayment) "Pay to park" else "Duration unspecified",
                timerBasis = if (hasPayment) "Metered parking" else "Unspecified limit",
                allowedUntilTimeFormatted = scanResult.allowedUntilTime,
                confirmationHeadline = if (hasPayment) "Pay to park" else "Parking duration unspecified",
                confirmationSubtext = if (hasPayment) "Metered parking detected, but no maximum time limit was established by signage." else "Curb detected that parking is allowed, but could not determine a specific time limit. Check physical signs on-site before parking.",
                ruleSummary = scanResult.parkingRules.firstOrNull() ?: "No verified parking rule has been established."
            )
        }

        // Determine effective mode and minutes
        val mode: TimerSemanticMode
        val finalMinutes: Int
        val maxAllowedMillis: Long

        if (postedLimitMinutes != null && minutesUntilClockCutoff != null) {
            val postedEndTime = currentTimeMillis + (postedLimitMinutes * 60000L)
            if (clockCutoffMillis!! <= postedEndTime) {
                mode = TimerSemanticMode.CLOCK_CUTOFF
                finalMinutes = minutesUntilClockCutoff
                maxAllowedMillis = clockCutoffMillis
            } else {
                mode = TimerSemanticMode.TIMED_LIMIT
                finalMinutes = postedLimitMinutes
                maxAllowedMillis = postedEndTime
            }
        } else if (postedLimitMinutes != null) {
            mode = TimerSemanticMode.TIMED_LIMIT
            finalMinutes = postedLimitMinutes
            maxAllowedMillis = currentTimeMillis + (postedLimitMinutes * 60000L)
        } else if (minutesUntilClockCutoff != null) {
            mode = TimerSemanticMode.CLOCK_CUTOFF
            finalMinutes = minutesUntilClockCutoff
            maxAllowedMillis = clockCutoffMillis!!
        } else {
            mode = TimerSemanticMode.TIMED_LIMIT
            finalMinutes = timeRemainingMinutes!!
            maxAllowedMillis = currentTimeMillis + (timeRemainingMinutes * 60000L)
        }

        val calculatedEndTimeMillis = currentTimeMillis + (finalMinutes * 60000L)
        val sdfEnd = SimpleDateFormat("h:mm a", Locale.US)
        val formattedEndTimeStr = sdfEnd.format(Date(calculatedEndTimeMillis))
        val formattedDuration = formatMinutesToDisplay(finalMinutes)

        val basisText = when (mode) {
            TimerSemanticMode.TIMED_LIMIT -> "$formattedDuration limit"
            TimerSemanticMode.CLOCK_CUTOFF -> "Allowed until $formattedEndTimeStr"
            else -> "$formattedDuration limit"
        }

        val headline = "Parking allowed until $formattedEndTimeStr."
        val subtext = when (mode) {
            TimerSemanticMode.TIMED_LIMIT -> "Timer set for $formattedDuration based on verified $formattedDuration limit."
            TimerSemanticMode.CLOCK_CUTOFF -> "Timer set for $formattedDuration ($finalMinutes mins remaining before restriction cutoff at $formattedEndTimeStr)."
            else -> "Timer set for $formattedDuration based on verified parking rules."
        }

        val ruleSummary = scanResult.parkingRules.firstOrNull() ?: "No verified parking rule has been established."

        return ParkingTimerConfig(
            canStart = true,
            mode = mode,
            reason = "Verified parking time limit established.",
            startTime = currentTimeMillis,
            endTime = maxAllowedMillis,
            maxAllowedEndTimeMillis = maxAllowedMillis,
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
