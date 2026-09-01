package com.example.data.model

enum class ScanVerdict {
    ALLOWED,
    RESTRICTED,
    AMBIGUOUS;

    val displayTitle: String
        get() = when (this) {
            ALLOWED -> "Yes, you can park here"
            RESTRICTED -> "No, parking is restricted"
            AMBIGUOUS -> "Rule unclear — Verify Locally"
        }

    val subtitle: String
        get() = when (this) {
            ALLOWED -> "You can park here under the rules that apply right now."
            RESTRICTED -> "Parking is currently prohibited or restricted by active zone rules."
            AMBIGUOUS -> "Signs contain conflicting, obstructed, or faded text. Please verify physical signage."
        }
}

data class DetectedSign(
    val id: String,
    val title: String,
    val subtitle: String,
    val ruleText: String,
    val isRestrictingNow: Boolean = false,
    val rawText: String = ""
)

data class ScanResult(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val locationName: String = "Mission Street",
    val cityState: String = "San Francisco, CA",
    val verdict: ScanVerdict = ScanVerdict.ALLOWED,
    val statusChipText: String = "Updated just now",
    val allowedUntilTime: String = "6:00 PM",
    val timeRemaining: String = "2h 15m remaining",
    val parkingRules: List<String> = listOf(
        "2 Hour Parking: 8:00 AM – 6:00 PM, Mon – Fri",
        "Street Cleaning: Tuesday & Thursday, 8:00 AM – 10:00 AM",
        "Other restrictions: No restrictions on weekends and city holidays"
    ),
    val explanation: String = "Based on the signs you scanned, 2-hour parking is permitted between 8:00 AM and 6:00 PM on weekdays. Street sweeping is not active today.",
    val detectedSigns: List<DetectedSign> = listOf(
        DetectedSign("1", "2 HR PARKING", "8 AM TO 6 PM / MON-FRI", "2 hour limit applies during daytime hours."),
        DetectedSign("2", "NO PARKING", "8 AM TO 10 AM / TUE & THU", "Street cleaning restriction (inactive today)."),
        DetectedSign("3", "TOW-AWAY ZONE", "4 PM TO 6 PM / MON-FRI", "Evening commute tow-away lane.")
    ),
    val zoneType: String = "Metered parking zone",
    val paymentInfo: String = "Pay at meter or via app ($3.50/hr)",
    val vehicleApplicability: String = "Standard passenger vehicles (under 6,000 lbs)",
    val imageUri: String? = null
)

data class ActiveParkingSession(
    val id: Long = 0,
    val scanResultId: Long = 0,
    val locationName: String = "Mission Street",
    val startTime: Long = System.currentTimeMillis() - (15 * 60 * 1000), // 15 mins ago default
    val endTime: Long = System.currentTimeMillis() + (135 * 60 * 1000), // 2h 15m remaining
    val allowedUntilTime: String = "11:00 AM",
    val reminderMinutesBefore: Int = 15,
    val notes: String = "Metered parking • Space #42",
    val isActive: Boolean = true
) {
    val remainingMillis: Long
        get() = (endTime - System.currentTimeMillis()).coerceAtLeast(0)

    val totalMillis: Long
        get() = (endTime - startTime).coerceAtLeast(1000)

    val progressFraction: Float
        get() = (remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)

    val timerLargeDisplay: String
        get() {
            val totalSeconds = remainingMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes}m"
            }
        }

    val totalLimitDisplay: String
        get() {
            val totalSeconds = totalMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return if (hours > 0 && minutes > 0) {
                "${hours}h ${minutes}m"
            } else if (hours > 0) {
                "${hours}h"
            } else {
                "${minutes}m"
            }
        }

    val startedAtDisplay: String
        get() {
            val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(startTime))
        }

    val remainingFormatted: String
        get() {
            val totalSeconds = remainingMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return if (hours > 0) {
                "${hours}h ${minutes}m remaining"
            } else {
                "${minutes}m remaining"
            }
        }
}

data class SavedPlace(
    val id: Long = 0,
    val name: String,
    val address: String,
    val parkingNote: String = "2hr limit on weekdays",
    val timestamp: Long = System.currentTimeMillis()
)

data class UserProfile(
    val name: String = "Alex",
    val gender: String = "Not specified",
    val email: String = "alex@curbapp.com",
    val isPro: Boolean = false,
    val pushNotificationsEnabled: Boolean = true
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class SampleSignPreset(
    val id: String,
    val title: String,
    val previewDescription: String,
    val simulatedVerdict: ScanVerdict,
    val locationName: String,
    val allowedUntil: String,
    val rules: List<String>,
    val explanation: String,
    val detectedSigns: List<DetectedSign>
)

data class CurbNote(
    val id: Long = 0,
    val targetType: String, // "SAVED_PLACE" or "SCAN_RESULT"
    val targetId: Long,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TARGET_SAVED_PLACE = "SAVED_PLACE"
        const val TARGET_SCAN_RESULT = "SCAN_RESULT"
    }
}
