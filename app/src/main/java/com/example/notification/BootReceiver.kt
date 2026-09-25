package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.CurbDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = CurbDatabase.getDatabase(context)
                    val activeSession = db.parkingSessionDao().getActiveSessionDirect()
                    if (activeSession != null && !activeSession.isDemo && activeSession.isActive) {
                        ParkingNotificationScheduler.scheduleSessionNotifications(context, activeSession)
                    }

                    val savedPlaceEntities = db.savedPlaceDao().getSavedPlacesList()
                    savedPlaceEntities.forEach { entity ->
                        if (entity.reminderEnabled) {
                            val place = com.example.data.model.SavedPlace(
                                id = entity.id,
                                name = entity.name,
                                address = entity.address,
                                parkingNote = entity.parkingNote,
                                timestamp = entity.timestamp,
                                latitude = entity.latitude,
                                longitude = entity.longitude,
                                scanResultId = entity.scanResultId,
                                parkingRuleSummary = entity.parkingRuleSummary,
                                parkingSchedule = entity.parkingSchedule,
                                parkingVerdict = entity.parkingVerdict,
                                signImageUri = entity.signImageUri,
                                lastCheckedAt = entity.lastCheckedAt,
                                reminderEnabled = entity.reminderEnabled,
                                reminderMinutesBefore = entity.reminderMinutesBefore,
                                reminderScheduleText = entity.reminderScheduleText
                            )
                            ParkingNotificationScheduler.scheduleSavedPlaceReminder(context, place)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
