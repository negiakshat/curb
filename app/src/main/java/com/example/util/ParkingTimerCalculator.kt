package com.example.util

import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.model.DetectedSign
import com.example.data.detection.LocalSignCrop
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



enum class ParkingTimeEvidenceType {
    POSTED_DURATION,
    CLOCK_CUTOFF,
    BOTH,
    UNKNOWN
}

data class NormalizedTimeEvidence(
    val type: ParkingTimeEvidenceType,
    val durationMinutes: Int? = null,
    val cutoffTimeString: String? = null, // e.g. "6:00 PM"
    val cutoffMillis: Long? = null,
    val source: String = "" // "OCR", "GEMINI", or "UNKNOWN"
)

object ParkingTimeEvidenceBuilder {

    fun buildParkingTimeEvidence(
        scanResult: ScanResult,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): NormalizedTimeEvidence {
        // 1. PHYSICAL OCR EVIDENCE (Highest Priority)
        val physicalSigns = scanResult.detectedSigns.filter { sign ->
            !SignCandidateValidator.isDemoOrSampleCrop(sign.croppedImageUri, sign.isDemo, sign.id) &&
                    SignCandidateValidator.validateOcr(sign.rawText.ifBlank { sign.title }).isValid
        }

        if (physicalSigns.isNotEmpty()) {
            val physicalText = physicalSigns.joinToString(" ") { 
                "${it.title} ${it.restrictions} ${it.rawText}" 
            }.lowercase(Locale.US)

            val duration = parsePostedDurationLimitMinutes(physicalText)
            val nowCal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
            val cutoff = parseClockEndTime(scanResult.allowedUntilTime, physicalText, nowCal, currentTimeMillis)

            if (duration != null && cutoff != null) {
                return NormalizedTimeEvidence(
                    type = ParkingTimeEvidenceType.BOTH,
                    durationMinutes = duration,
                    cutoffMillis = cutoff,
                    cutoffTimeString = formatCutoffTime(cutoff),
                    source = "OCR"
                )
            } else if (duration != null) {
                return NormalizedTimeEvidence(
                    type = ParkingTimeEvidenceType.POSTED_DURATION,
                    durationMinutes = duration,
                    source = "OCR"
                )
            } else if (cutoff != null) {
                return NormalizedTimeEvidence(
                    type = ParkingTimeEvidenceType.CLOCK_CUTOFF,
                    cutoffMillis = cutoff,
                    cutoffTimeString = formatCutoffTime(cutoff),
                    source = "OCR"
                )
            }
        }

        // 2. Fallback to STRUCTURED GEMINI VISUAL EVIDENCE
        // Only allow if verdict is ALLOWED and signs are not marked uncertain
        val canUseGemini = scanResult.verdict == ScanVerdict.ALLOWED &&
                scanResult.detectedSigns.none { it.isUncertain || it.isRestrictingNow }

        if (canUseGemini) {
            val geminiText = buildString {
                append(scanResult.allowedUntilTime).append(" ")
                scanResult.parkingRules.forEach { append(it).append(" ") }
                scanResult.detectedSigns.forEach { append(it.title).append(" ").append(it.restrictions).append(" ").append(it.subtitle).append(" ").append(it.ruleText).append(" ") }
            }.lowercase(Locale.US)

            // Block vague Gemini-only phrases!
            val hasVaguePhrases = geminiText.contains("probably") ||
                    geminiText.contains("looks like") ||
                    geminiText.contains("standard parking") ||
                    geminiText.contains("assumed") ||
                    geminiText.contains("derived")

            if (!hasVaguePhrases) {
                val duration = parsePostedDurationLimitMinutes(geminiText)
                val nowCal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
                val cutoff = parseClockEndTime(scanResult.allowedUntilTime, geminiText, nowCal, currentTimeMillis)

                if (duration != null && cutoff != null) {
                    return NormalizedTimeEvidence(
                        type = ParkingTimeEvidenceType.BOTH,
                        durationMinutes = duration,
                        cutoffMillis = cutoff,
                        cutoffTimeString = formatCutoffTime(cutoff),
                        source = "GEMINI"
                    )
                } else if (duration != null) {
                    return NormalizedTimeEvidence(
                        type = ParkingTimeEvidenceType.POSTED_DURATION,
                        durationMinutes = duration,
                        source = "GEMINI"
                    )
                } else if (cutoff != null) {
                    return NormalizedTimeEvidence(
                        type = ParkingTimeEvidenceType.CLOCK_CUTOFF,
                        cutoffMillis = cutoff,
                        cutoffTimeString = formatCutoffTime(cutoff),
                        source = "GEMINI"
                    )
                }
            }
        }

        return NormalizedTimeEvidence(
            type = ParkingTimeEvidenceType.UNKNOWN,
            source = "UNKNOWN"
        )
    }

    private fun formatCutoffTime(millis: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.US)
        return sdf.format(Date(millis))
    }

    fun parsePostedDurationLimitMinutes(text: String): Int? {
        val lower = text.lowercase(Locale.US)
        var lowestMinutes: Int? = null

        val hourMatcher = Pattern.compile("(\\d+)\\s*(?:-?\\s*hour|hr|hrs|h|\\-hour)").matcher(lower)
        while (hourMatcher.find()) {
            val hrs = hourMatcher.group(1)?.toIntOrNull()
            if (hrs != null && hrs in 1..12) {
                val mins = hrs * 60
                if (lowestMinutes == null || mins < lowestMinutes) {
                    lowestMinutes = mins
                }
            }
        }

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

    fun parseClockEndTime(
        allowedUntilTime: String,
        combinedText: String,
        nowCal: Calendar,
        currentTimeMillis: Long
    ): Long? {
        val candidateStrings = listOf(allowedUntilTime, combinedText)

        for (timeStr in candidateStrings) {
            if (timeStr.isBlank()) continue
            val lower = timeStr.lowercase(Locale.US)

            if (lower.contains("no time limit") || lower.contains("unrestricted") ||
                lower.contains("prohibited") || lower.contains("unclear") || lower.contains("uncertain")) {
                continue
            }

            val isRecurringWindow = lower.contains("-") || lower.contains(" to ") || lower.contains("mon") || lower.contains("tue") || lower.contains("wed") || lower.contains("thu") || lower.contains("fri")
            val hasExplicitCutoffKeyword = lower.contains("until") || lower.contains("allowed until") || lower.contains("after") || lower.contains("ends") || lower.contains("cutoff")
            if (isRecurringWindow && !hasExplicitCutoffKeyword) {
                continue
            }

            val clean = timeStr.trim().uppercase(Locale.US)

            val amPmMatcher = Pattern.compile("(?:UNTIL|ALLOWED UNTIL|ENDS?\\s+AT|BY|AFTER)?\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(AM|PM)").matcher(clean)
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
                    targetCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                if (targetCal.timeInMillis > currentTimeMillis) {
                    return targetCal.timeInMillis
                }
            }
        }

        return null
    }

    fun parseRemainingMinutesFallback(remainingStr: String): Int? {
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

    fun formatMinutesToDisplay(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h 00m"
            else -> "${m}m"
        }
    }
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
    val ruleSummary: String = "",
    val nextRestrictionText: String = ""
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
                scanResult.detectedSigns.any { it.isUncertain }

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
                allowedUntilTimeFormatted = "Verify physical signage",
                confirmationHeadline = "Parking rule is uncertain",
                confirmationSubtext = "Curb could not establish the active rule with certainty. Verify physical signs on-site before parking.",
                ruleSummary = "Uncertain signage"
            )
        }

        // Authority gate validation for non-demo scans
        if (!scanResult.isDemo) {
            if (!ParkingAuthority.canAuthorizeTimer(scanResult) || !SemanticConsistencyValidator.canAuthorizeTimer(scanResult)) {
                return ParkingTimerConfig(
                    canStart = false,
                    mode = TimerSemanticMode.AMBIGUOUS_OR_RESTRICTED,
                    reason = "Scan lacks verified time-limit signage evidence.",
                    startTime = currentTimeMillis,
                    endTime = currentTimeMillis,
                    maxAllowedEndTimeMillis = null,
                    calculatedMinutes = 0,
                    formattedDuration = "--",
                    timerBasis = "Uncertain time basis",
                    allowedUntilTimeFormatted = "Verify physical signage",
                    confirmationHeadline = "Uncertain time basis",
                    confirmationSubtext = "Curb could not establish a reliable duration or cutoff from validated physical evidence. Verify physical signs on-site.",
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
                formattedDuration = "No fixed time limit",
                timerBasis = "Unrestricted parking",
                allowedUntilTimeFormatted = "No fixed time limit",
                confirmationHeadline = "Parking allowed — no fixed time limit detected",
                confirmationSubtext = "You can park here without a fixed time limit countdown.",
                ruleSummary = scanResult.parkingRules.firstOrNull() ?: "Unrestricted parking"
            )
        }

        // 4. ALLOWED WITH TIME LIMIT OR CUTOFF
        val evidence = ParkingTimeEvidenceBuilder.buildParkingTimeEvidence(scanResult, currentTimeMillis)

        // If evidence is UNKNOWN -> No timer
        if (evidence.type == ParkingTimeEvidenceType.UNKNOWN) {
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
                reason = "No verified time-limit signage evidence.",
                startTime = currentTimeMillis,
                endTime = currentTimeMillis,
                maxAllowedEndTimeMillis = null,
                calculatedMinutes = 0,
                formattedDuration = "No fixed time limit",
                timerBasis = "Uncertain time basis",
                allowedUntilTimeFormatted = "Verify physical signage",
                confirmationHeadline = "Parking allowed — verify physical signage",
                confirmationSubtext = if (isPayment) "Metered parking detected, but no verified time limit was established by signage." else "You can park here without a fixed time limit countdown.",
                ruleSummary = scanResult.parkingRules.firstOrNull() ?: "No verified time limit"
            )
        }

        // Determine effective mode and minutes from evidence
        val mode: TimerSemanticMode
        val finalMinutes: Int
        val maxAllowedMillis: Long

        val postedLimitMinutes = evidence.durationMinutes
        val clockCutoffMillis = evidence.cutoffMillis

        var minutesUntilClockCutoff: Int? = null
        if (clockCutoffMillis != null && clockCutoffMillis > currentTimeMillis) {
            val diffMs = clockCutoffMillis - currentTimeMillis
            minutesUntilClockCutoff = Math.round(diffMs / 60000.0).toInt()
        }

        if (postedLimitMinutes != null && minutesUntilClockCutoff != null) {
            val postedEndTime = currentTimeMillis + (postedLimitMinutes * 60000L)
            if (clockCutoffMillis!! <= postedEndTime) {
                mode = TimerSemanticMode.CLOCK_CUTOFF
                finalMinutes = minutesUntilClockCutoff
                maxAllowedMillis = clockCutoffMillis!!
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
            return ParkingTimerConfig(
                canStart = false,
                mode = TimerSemanticMode.AMBIGUOUS_OR_RESTRICTED,
                reason = "Uncertain time basis",
                startTime = currentTimeMillis,
                endTime = currentTimeMillis,
                maxAllowedEndTimeMillis = null,
                calculatedMinutes = 0,
                formattedDuration = "Rule unclear",
                timerBasis = "Uncertain time basis",
                allowedUntilTimeFormatted = "Verify physical signage"
            )
        }

        val calculatedEndTimeMillis = currentTimeMillis + (finalMinutes * 60000L)
        val sdfEnd = SimpleDateFormat("h:mm a", Locale.US)
        val formattedEndTimeStr = sdfEnd.format(Date(calculatedEndTimeMillis))
        val formattedDuration = ParkingTimeEvidenceBuilder.formatMinutesToDisplay(finalMinutes)

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
        val signText = buildString {
            append(scanResult.allowedUntilTime).append(" ")
            scanResult.parkingRules.forEach { append(it).append(" ") }
            scanResult.detectedSigns.forEach { append(it.title).append(" ").append(it.restrictions).append(" ").append(it.subtitle).append(" ") }
        }.lowercase(Locale.US)
        val nextRestriction = parseNextRestrictionSchedule(signText)

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
            ruleSummary = ruleSummary,
            nextRestrictionText = nextRestriction
        )
    }

    private fun parseNextRestrictionSchedule(text: String): String {
        val lower = text.lowercase(Locale.US)
        val scheduleRegex = Regex("""(?i)\b(mon|tue|wed|thu|fri|sat|sun)[-\s]*(mon|tue|wed|thu|fri|sat|sun)?\s*(\d{1,2}(?::\d{2})?\s*(?:am|pm)?\s*[-–—toTO]+\s*\d{1,2}(?::\d{2})?\s*(?:am|pm)?)\b""")
        val match = scheduleRegex.find(lower)
        return if (match != null) {
            "Next restriction: ${match.value.uppercase(Locale.US)}"
        } else {
            ""
        }
    }
}
