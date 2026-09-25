package com.example.util

import com.example.data.model.SavedPlace
import java.util.Calendar
import java.util.Locale

object SavedPlaceReminderCalculator {

    fun calculateTriggerTime(place: SavedPlace): Long? {
        if (place.id <= 0L || !place.reminderEnabled) return null

        val verdict = place.parkingVerdict.uppercase(Locale.ROOT)
        if (verdict == "AMBIGUOUS" || verdict == "NOT_ALLOWED" || verdict == "RESTRICTED" || verdict == "UNRESTRICTED" || verdict == "NO_LIMIT") {
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

        // 1. Duration check: "X hour(s)" or "X hr(s)" or "X min(s)"
        val hrMatch = Regex("(\\d+)\\s*(?:hour|hr|hrs|hours)").find(lower)
        if (hrMatch != null) {
            val hours = hrMatch.groupValues[1].toLongOrNull() ?: return null
            if (hours > 0) {
                val baseTime = if (lastCheckedAt > 0) lastCheckedAt else System.currentTimeMillis()
                return baseTime + (hours * 60 * 60 * 1000L)
            }
        }

        val minMatch = Regex("(\\d+)\\s*(?:minute|min|mins|minutes)").find(lower)
        if (minMatch != null) {
            val mins = minMatch.groupValues[1].toLongOrNull() ?: return null
            if (mins > 0) {
                val baseTime = if (lastCheckedAt > 0) lastCheckedAt else System.currentTimeMillis()
                return baseTime + (mins * 60 * 1000L)
            }
        }

        // 2. Clock time check: "4:00 PM", "16:00", "4pm"
        val timeMatch = Regex("(1[0-2]|0?[1-9]):([0-5][0-9])\\s*(am|pm)", RegexOption.IGNORE_CASE).find(lower)
            ?: Regex("(1[0-2]|0?[1-9])\\s*(am|pm)", RegexOption.IGNORE_CASE).find(lower)

        if (timeMatch != null) {
            try {
                val baseTime = if (lastCheckedAt > 0) lastCheckedAt else System.currentTimeMillis()
                val cal = Calendar.getInstance()
                cal.timeInMillis = baseTime

                var hour = timeMatch.groupValues[1].toInt()
                val ampm = timeMatch.groupValues.last().lowercase(Locale.ROOT)
                if (ampm == "pm" && hour < 12) hour += 12
                if (ampm == "am" && hour == 12) hour = 0

                val minute = if (timeMatch.groupValues.size >= 4 && timeMatch.groupValues[2].isNotBlank()) {
                    timeMatch.groupValues[2].toIntOrNull() ?: 0
                } else 0

                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                var target = cal.timeInMillis
                if (target <= baseTime) {
                    target += 24 * 60 * 60 * 1000L
                }
                return target
            } catch (_: Exception) {}
        }

        return null
    }
}
