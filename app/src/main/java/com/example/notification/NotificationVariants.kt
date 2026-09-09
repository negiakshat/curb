package com.example.notification

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class NotificationType {
    REMINDER,
    EXPIRATION,
    ENDED
}

data class NotificationText(
    val title: String,
    val body: String
)

object NotificationVariants {

    private val REMINDER_VARIANTS = listOf(
        NotificationText(
            title = "Parking time running low",
            body = "Your session at %s expires in %d minutes."
        ),
        NotificationText(
            title = "15-minute parking reminder ⏱️",
            body = "You have %d minutes remaining at %s."
        ),
        NotificationText(
            title = "Approaching parking limit",
            body = "Your vehicle at %s will reach its limit in %d minutes."
        ),
        NotificationText(
            title = "Time to return to your vehicle",
            body = "Your parking session at %s expires shortly."
        )
    )

    private val EXPIRATION_VARIANTS = listOf(
        NotificationText(
            title = "Parking session expired",
            body = "Your time at %s has ended. Check parking rules to avoid a citation."
        ),
        NotificationText(
            title = "Time limit reached 🅿️",
            body = "Your parking session at %s expired at %s."
        ),
        NotificationText(
            title = "Parking time completed",
            body = "The allowed parking duration at %s has ended."
        )
    )

    private val ENDED_VARIANTS = listOf(
        NotificationText(
            title = "Parking session ended",
            body = "Your timer for %s has been stopped."
        ),
        NotificationText(
            title = "Session completed",
            body = "Your parking session at %s is now closed."
        )
    )

    fun getReminderVariant(
        sessionId: Long,
        locationName: String,
        minutesRemaining: Int
    ): NotificationText {
        val safeLocation = locationName.ifBlank { "your spot" }
        val index = abs(sessionId.toInt()) % REMINDER_VARIANTS.size
        val template = REMINDER_VARIANTS[index]

        val formattedBody = try {
            if (template.body.contains("%s") && template.body.contains("%d")) {
                if (template.body.indexOf("%s") < template.body.indexOf("%d")) {
                    String.format(Locale.getDefault(), template.body, safeLocation, minutesRemaining)
                } else {
                    String.format(Locale.getDefault(), template.body, minutesRemaining, safeLocation)
                }
            } else if (template.body.contains("%s")) {
                String.format(Locale.getDefault(), template.body, safeLocation)
            } else if (template.body.contains("%d")) {
                String.format(Locale.getDefault(), template.body, minutesRemaining)
            } else {
                template.body
            }
        } catch (_: Exception) {
            "Your parking session at $safeLocation expires in $minutesRemaining minutes."
        }

        return NotificationText(title = template.title, body = formattedBody)
    }

    fun getExpirationVariant(
        sessionId: Long,
        locationName: String,
        endTimeMillis: Long
    ): NotificationText {
        val safeLocation = locationName.ifBlank { "your spot" }
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeStr = sdf.format(Date(endTimeMillis))
        val index = abs(sessionId.toInt()) % EXPIRATION_VARIANTS.size
        val template = EXPIRATION_VARIANTS[index]

        val formattedBody = try {
            if (template.body.contains("%s") && template.body.indexOf("%s") != template.body.lastIndexOf("%s")) {
                String.format(Locale.getDefault(), template.body, safeLocation, timeStr)
            } else if (template.body.contains("%s")) {
                String.format(Locale.getDefault(), template.body, safeLocation)
            } else {
                template.body
            }
        } catch (_: Exception) {
            "Your parking session at $safeLocation has expired."
        }

        return NotificationText(title = template.title, body = formattedBody)
    }

    fun getEndedVariant(
        sessionId: Long,
        locationName: String
    ): NotificationText {
        val safeLocation = locationName.ifBlank { "your spot" }
        val index = abs(sessionId.toInt()) % ENDED_VARIANTS.size
        val template = ENDED_VARIANTS[index]

        val formattedBody = try {
            if (template.body.contains("%s")) {
                String.format(Locale.getDefault(), template.body, safeLocation)
            } else {
                template.body
            }
        } catch (_: Exception) {
            "Your parking session at $safeLocation has ended."
        }

        return NotificationText(title = template.title, body = formattedBody)
    }
}
