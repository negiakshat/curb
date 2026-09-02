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
    val locationName: String = "Current Location",
    val cityState: String = "",
    val verdict: ScanVerdict = ScanVerdict.ALLOWED,
    val statusChipText: String = "Updated just now",
    val allowedUntilTime: String = "6:00 PM",
    val timeRemaining: String = "2h 00m remaining",
    val parkingRules: List<String> = listOf(
        "Standard parking rules apply based on visible signage."
    ),
    val explanation: String = "Sign analysis completed for this parking location.",
    val detectedSigns: List<DetectedSign> = emptyList(),
    val zoneType: String = "Parking zone",
    val paymentInfo: String = "",
    val vehicleApplicability: String = "Standard passenger vehicles",
    val imageUri: String? = null
)

data class ActiveParkingSession(
    val id: Long = 0,
    val scanResultId: Long = 0,
    val locationName: String = "Parked Spot",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis() + (120 * 60 * 1000), // 2h default
    val allowedUntilTime: String = "",
    val reminderMinutesBefore: Int = 15,
    val notes: String = "",
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
    val email: String = "",
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
