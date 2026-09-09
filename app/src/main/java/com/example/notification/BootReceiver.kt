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
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
