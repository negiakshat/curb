package com.example.data.model

enum class ScanVerdict {
    ALLOWED,
    RESTRICTED,
    AMBIGUOUS;

    val displayTitle: String
        get() = when (this) {
            ALLOWED -> "Parking allowed"
            RESTRICTED -> "Parking restricted"
            AMBIGUOUS -> "Rule unclear"
        }

    val subtitle: String
        get() = when (this) {
            ALLOWED -> "You can park here under the current rules."
            RESTRICTED -> "An active rule prohibits parking at this spot right now."
            AMBIGUOUS -> "Some signage is faded, incomplete, or obstructed. Curb couldn't confidently determine the active parking rule."
        }
}

data class SignBoundingBox(
    val id: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val label: String,
    val ocrText: String = "",
    val confidence: Float = 0.95f,
    val sourceWidth: Float = 0f,
    val sourceHeight: Float = 0f
)

data class DetectedSign(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val applicableDaysHours: String = "",
    val restrictions: String = "",
    val exceptions: String = "",
    val ruleText: String = "",
    val isRestrictingNow: Boolean = false,
    val isUncertain: Boolean = false,
    val statusBadge: String = "",
    val rawText: String = "",
    val croppedImageUri: String? = null,
    val confidence: Float = 0.95f
)

data class ScanResult(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val locationName: String = "Current Location",
    val cityState: String = "",
    val verdict: ScanVerdict = ScanVerdict.AMBIGUOUS,
    val statusChipText: String = "Signage unclear",
    val allowedUntilTime: String = "Verify physical signage",
    val timeRemaining: String = "--",
    val parkingRules: List<String> = listOf(
        "No verified parking rule has been established."
    ),
    val explanation: String = "Parking rules could not be determined from verified sign evidence.",
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
    val endTime: Long = System.currentTimeMillis(),
    val allowedUntilTime: String = "",
    val reminderMinutesBefore: Int = 15,
    val notes: String = "",
    val timerBasis: String = "",
    val parkingRuleSummary: String = "",
    val isActive: Boolean = true,
    val maxAllowedEndTimeMillis: Long? = null
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

data class ParkingSpot(
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val accuracy: Float? = null,
    val locationName: String = "",
    val sessionId: Long? = null,
    val isActive: Boolean = true
)

data class SavedPlace(
    val id: Long = 0,
    val name: String,
    val address: String,
    val parkingNote: String = "",
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
