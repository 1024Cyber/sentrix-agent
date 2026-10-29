package com.sentrix.agent

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.sentrix.agent.util.Prefs

class SentrixApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)

        // Persistent service notification (required for foreground service)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICE,
                "Sentrix Agent",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Sentrix active in the background"
                setShowBadge(false)
            }
        )

        // Alerts channel (SIM change, geofence, etc.)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                "Sentrix Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Security alerts from Sentrix"
            }
        )
    }

    companion object {
        const val CHANNEL_SERVICE = "sentrix_service"
        const val CHANNEL_ALERTS  = "sentrix_alerts"
    }
}
