package com.sentrix.agent.commands

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.sentrix.agent.util.RootUtil

/**
 * Connectivity commands.
 *
 * WiFi/Data/Hotspot/Airplane on Android 10+ require root (system APIs are blocked).
 * Each function returns a human-readable result string sent back to the dashboard.
 */
object ConnectivityCommands {

    private const val TAG = "ConnectivityCmds"

    // ── WiFi ──────────────────────────────────────────────────────────────────

    fun wifiOn(context: Context): String {
        return if (RootUtil.isRooted()) {
            // Root path — works on all Android versions
            val ok = RootUtil.run("svc wifi enable")
            if (ok) "WiFi enabled" else "WiFi enable failed"
        } else {
            // Non-root: WifiManager.setWifiEnabled() was removed in API 29
            // We can only open the WiFi settings panel for the user
            "WiFi control requires root on Android 10+"
        }
    }

    fun wifiOff(context: Context): String {
        return if (RootUtil.isRooted()) {
            val ok = RootUtil.run("svc wifi disable")
            if (ok) "WiFi disabled" else "WiFi disable failed"
        } else {
            "WiFi control requires root on Android 10+"
        }
    }

    // ── Mobile Data ───────────────────────────────────────────────────────────

    fun dataOn(): String {
        return if (RootUtil.isRooted()) {
            val ok = RootUtil.run("svc data enable")
            if (ok) "Mobile data enabled" else "Data enable failed"
        } else {
            "Data control requires root on Android 10+"
        }
    }

    fun dataOff(): String {
        return if (RootUtil.isRooted()) {
            val ok = RootUtil.run("svc data disable")
            if (ok) "Mobile data disabled" else "Data disable failed"
        } else {
            "Data control requires root on Android 10+"
        }
    }

    // ── Hotspot ───────────────────────────────────────────────────────────────

    fun hotspotOn(): String {
        return if (RootUtil.isRooted()) {
            // Enable tethering via settings put
            val ok = RootUtil.run("settings put global tether_dun_required 0")
            RootUtil.run("cmd connectivity start-tethering -t 0")
            if (ok) "Hotspot enabled" else "Hotspot enable failed"
        } else {
            "Hotspot control requires root"
        }
    }

    fun hotspotOff(): String {
        return if (RootUtil.isRooted()) {
            val ok = RootUtil.run("cmd connectivity stop-tethering -t 0")
            if (ok) "Hotspot disabled" else "Hotspot disable failed"
        } else {
            "Hotspot control requires root"
        }
    }

    // ── Airplane Mode ─────────────────────────────────────────────────────────

    fun airplaneOn(): String {
        return if (RootUtil.isRooted()) {
            RootUtil.run("settings put global airplane_mode_on 1")
            val ok = RootUtil.run("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true")
            if (ok) "Airplane mode ON" else "Airplane mode failed"
        } else {
            "Airplane mode requires root on Android 10+"
        }
    }

    fun airplaneOff(): String {
        return if (RootUtil.isRooted()) {
            RootUtil.run("settings put global airplane_mode_on 0")
            val ok = RootUtil.run("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false")
            if (ok) "Airplane mode OFF" else "Airplane mode failed"
        } else {
            "Airplane mode requires root on Android 10+"
        }
    }

    // ── Bluetooth ─────────────────────────────────────────────────────────────

    fun bluetoothOn(): String {
        return if (RootUtil.isRooted()) {
            val ok = RootUtil.run("svc bluetooth enable")
            if (ok) "Bluetooth enabled" else "Bluetooth enable failed"
        } else {
            "Bluetooth control requires root on Android 13+"
        }
    }

    fun bluetoothOff(): String {
        return if (RootUtil.isRooted()) {
            val ok = RootUtil.run("svc bluetooth disable")
            if (ok) "Bluetooth disabled" else "Bluetooth disable failed"
        } else {
            "Bluetooth control requires root on Android 13+"
        }
    }
}
