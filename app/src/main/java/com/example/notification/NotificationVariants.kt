package com.example.notification

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class NotificationType {
    REMINDER,
    EXPIRATION,
    SAVED_PLACE
}

data class NotificationText(
    val title: String,
    val body: String
)

object NotificationVariants {

    fun getReminderVariant(
        sessionId: Long,
        locationName: String,
        minutesRemaining: Int
    ): NotificationText {
        val safeLocation = locationName.ifBlank { "your spot" }
        return NotificationText(
            title = "Parking time running low",
            body = "$minutesRemaining minutes left at $safeLocation."
        )
    }

    fun getExpirationVariant(
        sessionId: Long,
        locationName: String,
        endTimeMillis: Long
    ): NotificationText {
        val safeLocation = locationName.ifBlank { "your spot" }
        return NotificationText(
            title = "Parking session expired",
            body = "Your parking time at $safeLocation has ended."
        )
    }

    fun getSavedPlaceVariant(locationName: String): NotificationText {
        val safeLocation = locationName.ifBlank { "your spot" }
        return NotificationText(
            title = "Check your parking spot",
            body = "$safeLocation · Your saved parking reminder is coming up."
        )
    }
}
