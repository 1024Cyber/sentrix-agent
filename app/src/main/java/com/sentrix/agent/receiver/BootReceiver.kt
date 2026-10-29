package com.sentrix.agent.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sentrix.agent.AgentService
import com.sentrix.agent.util.Prefs

/**
 * Starts AgentService automatically after:
 *  - Device boot (BOOT_COMPLETED)
 *  - App update (MY_PACKAGE_REPLACED)
 *
 * Only starts if setup has been completed and agent is enabled.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i("BootReceiver", "Received: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            if (Prefs.setupDone && Prefs.agentEnabled) {
                val serviceIntent = Intent(context, AgentService::class.java)
                context.startForegroundService(serviceIntent)
                Log.i("BootReceiver", "AgentService started after boot")
            } else {
                Log.i("BootReceiver", "Setup not done or agent disabled — skipping auto-start")
            }
        }
    }
}
