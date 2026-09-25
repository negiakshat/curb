package com.example.util

import com.example.data.model.SavedPlace
import java.util.Calendar
import java.util.Locale

object SavedPlaceReminderCalculator {

    fun calculateTriggerTime(place: SavedPlace): Long? {
        if (place.id <= 0L || !place.reminderEnabled) return null

        val verdict = place.parkingVerdict.uppercase(Locale.ROOT)
        if (verdict.isEmpty() ||
            verdict.contains("AMBIGUOUS") || verdict.contains("UNCLEAR") ||
            verdict.contains("NOT_ALLOWED") || verdict.contains("RESTRICTED") ||
            verdict.contains("UNRESTRICTED") || verdict.contains("NO_LIMIT") || verdict.contains("NO LIMIT")
        ) {
            return null
        }

        val textToParse = "${place.parkingSchedule} ${place.parkingRuleSummary} ${place.reminderScheduleText}"
        val expiryMillis = parseExpiryMillis(textToParse, place.lastCheckedAt) ?: return null

        val reminderMins = if (place.reminderMinutesBefore > 0) place.reminderMinutesBefore else 15
        val triggerTime = expiryMillis - (reminderMins * 60 * 1000L)

        val now = System.currentTimeMillis()
        if (triggerTime <= now) {
            return null
        }

        return triggerTime
    }

    private fun parseExpiryMillis(text: String, lastCheckedAt: Long): Long? {
        if (text.isBlank()) return null
        val lower = text.lowercase(Locale.ROOT)

        if (lower.contains("unrestricted") ||
            lower.contains("no limit") ||
            lower.contains("no parking") ||
            lower.contains("verify physical signage") ||
            lower.contains("no verified") ||
            lower.contains("unclear")
        ) {
            return null
        }

        val baseTime = if (lastCheckedAt > 0) lastCheckedAt else System.currentTimeMillis()

        // 1. Duration check: "X hour(s)" or "X hr(s)" or "X min(s)"
        val hrMatch = Regex("""\b(\d+)\s*(?:hour|hr|hrs|hours)\b""").find(lower)
        if (hrMatch != null) {
            val hours = hrMatch.groupValues[1].toLongOrNull() ?: return null
            if (hours in 1..24) {
                return baseTime + (hours * 60 * 60 * 1000L)
            }
        }

        val minMatch = Regex("""\b(\d+)\s*(?:minute|min|mins|minutes)\b""").find(lower)
        if (minMatch != null) {
            val mins = minMatch.groupValues[1].toLongOrNull() ?: return null
            if (mins in 1..1440) {
                return baseTime + (mins * 60 * 1000L)
            }
        }

        // 2. Explicit time range check: e.g. "8 AM - 6 PM" -> cutoff is the END time (6 PM)
        val rangeRegex = Regex("""(?:(?:1[0-2]|0?[1-9])(?::[0-5][0-9])?\s*(?:am|pm)?)\s*(?:to|-|–|—)\s*((?:1[0-2]|0?[1-9])(?::[0-5][0-9])?\s*(?:am|pm))""")
        val rangeMatch = rangeRegex.find(lower)
        if (rangeMatch != null) {
            val endTimeStr = rangeMatch.groupValues[1]
            val cutoffMillis = parseClockTime(endTimeStr, baseTime)
            if (cutoffMillis != null) return cutoffMillis
        }

        // 3. Explicit cutoff check: "until 6:00 PM" or "allowed until 6 PM"
        val untilRegex = Regex("""(?:until|allowed until)\s+((?:1[0-2]|0?[1-9])(?::[0-5][0-9])?\s*(?:am|pm))""")
        val untilMatch = untilRegex.find(lower)
        if (untilMatch != null) {
            val endTimeStr = untilMatch.groupValues[1]
            val cutoffMillis = parseClockTime(endTimeStr, baseTime)
            if (cutoffMillis != null) return cutoffMillis
        }

        return null
    }

    private fun parseClockTime(timeStr: String, baseTime: Long): Long? {
        val timeMatch = Regex("""(1[0-2]|0?[1-9])(?::([0-5][0-9]))?\s*(am|pm)""").find(timeStr.trim()) ?: return null
        try {
            val cal = Calendar.getInstance()
            cal.timeInMillis = baseTime

            var hour = timeMatch.groupValues[1].toInt()
            val minute = if (timeMatch.groupValues.size >= 3 && timeMatch.groupValues[2].isNotBlank()) {
                timeMatch.groupValues[2].toIntOrNull() ?: 0
            } else 0
            val ampm = timeMatch.groupValues.last().lowercase(Locale.ROOT)

            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0

            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            var target = cal.timeInMillis
            if (target <= baseTime) {
                target += 24 * 60 * 60 * 1000L
            }
            return target
        } catch (_: Exception) {
            return null
        }
    }
}
