package com.example.viewmodel.coordinators

import com.example.data.local.SessionPreferences
import com.example.data.model.ActiveParkingSession
import com.example.data.model.InAppNotification
import com.example.notification.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive

class NotificationCoordinator(
    private val sessionPreferences: SessionPreferences,
    private val activeSession: StateFlow<ActiveParkingSession?>,
    private val allSessions: StateFlow<List<ActiveParkingSession>>,
    private val coroutineScope: CoroutineScope
) {
    private val _lastNotificationReadTime = MutableStateFlow(sessionPreferences.lastNotificationReadTime)
    
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _notificationTicker = activeSession.flatMapLatest { active ->
        flow {
            val startReal = System.currentTimeMillis()
            var elapsed = 0L
            emit(startReal)
            if (active != null && !active.isDemo && active.isActive) {
                val duration = (active.endTime - startReal).coerceAtLeast(0L)
                while (currentCoroutineContext().isActive && elapsed < duration) {
                    delay(5000L)
                    elapsed += 5000L
                    emit(startReal + elapsed)
                }
            }
        }
    }

    private val _showNotificationDialog = MutableStateFlow(false)
    val showNotificationDialog: StateFlow<Boolean> = _showNotificationDialog.asStateFlow()

    val inAppNotifications: StateFlow<List<InAppNotification>> = combine(
        activeSession,
        allSessions,
        _lastNotificationReadTime,
        _notificationTicker
    ) { active, sessions, lastRead, currentTime ->
        val list = mutableListOf<InAppNotification>()
        val now = currentTime

        if (active != null && !active.isDemo) {
            val remainingMs = active.endTime - now
            val remainingMins = (remainingMs / 60000L).toInt().coerceAtLeast(0)
            val isExpired = remainingMs <= 0

            if (!isExpired) {
                list.add(
                    InAppNotification(
                        id = "active_reminder_${active.id}",
                        sessionId = active.id,
                        title = "Parking time running low",
                        body = "${active.locationName} · $remainingMins min remaining",
                        locationName = active.locationName,
                        timestamp = active.startTime,
                        type = NotificationType.REMINDER,
                        isRead = active.startTime <= lastRead,
                        isActiveSession = true,
                        endTimeMillis = active.endTime,
                        remainingMinutes = remainingMins
                    )
                )
            } else {
                list.add(
                    InAppNotification(
                        id = "active_expired_${active.id}",
                        sessionId = active.id,
                        title = "Parking session expired",
                        body = "${active.locationName} · Expired",
                        locationName = active.locationName,
                        timestamp = active.endTime,
                        type = NotificationType.EXPIRATION,
                        isRead = active.endTime <= lastRead,
                        isActiveSession = false,
                        endTimeMillis = active.endTime,
                        remainingMinutes = 0
                    )
                )
            }
        }

        val realPastSessions = sessions.filter { !it.isDemo && it.id != active?.id }
        for (session in realPastSessions) {
            val isExpired = session.endTime <= now
            list.add(
                InAppNotification(
                    id = "past_session_${session.id}",
                    sessionId = session.id,
                    title = if (isExpired) "Parking session expired" else "Parking session active",
                    body = "${session.locationName} · ${session.allowedUntilTime}",
                    locationName = session.locationName,
                    timestamp = session.startTime,
                    type = if (isExpired) NotificationType.EXPIRATION else NotificationType.REMINDER,
                    isRead = session.startTime <= lastRead,
                    isActiveSession = false,
                    endTimeMillis = session.endTime
                )
            )
        }

        list.sortedByDescending { it.timestamp }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    val hasUnreadNotifications: StateFlow<Boolean> = combine(
        inAppNotifications,
        _lastNotificationReadTime
    ) { notifications, lastRead ->
        notifications.any { !it.isRead && it.timestamp > lastRead }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, false)

    fun markNotificationsAsRead() {
        val now = System.currentTimeMillis()
        sessionPreferences.lastNotificationReadTime = now
        _lastNotificationReadTime.value = now
    }

    fun setShowNotificationDialog(show: Boolean) {
        _showNotificationDialog.value = show
    }

    fun resetEphemeralState() {
        _showNotificationDialog.value = false
    }
}
