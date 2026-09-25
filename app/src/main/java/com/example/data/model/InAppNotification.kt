package com.example.data.model

import com.example.notification.NotificationType

data class InAppNotification(
    val id: String,
    val sessionId: Long,
    val title: String,
    val body: String,
    val locationName: String,
    val timestamp: Long,
    val type: NotificationType,
    val isRead: Boolean = false,
    val isActiveSession: Boolean = false,
    val endTimeMillis: Long = 0L,
    val remainingMinutes: Int = 0
)
