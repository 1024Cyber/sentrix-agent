package com.sentrix.agent.commands

import android.content.Context
import com.sentrix.agent.util.RootUtil

object ConnectivityCommands {

    fun wifiOn(ctx: Context)  = rootCmd("svc wifi enable",  "WiFi enabled",  "WiFi Off")
    fun wifiOff(ctx: Context) = rootCmd("svc wifi disable", "WiFi disabled", "WiFi Off")

    fun dataOn()  = rootCmd("svc data enable",  "Mobile data enabled",  "Mobile data On")
    fun dataOff() = rootCmd("svc data disable", "Mobile data disabled", "Mobile data Off")

    fun btOn()  = rootCmd("svc bluetooth enable",  "Bluetooth enabled",  "Bluetooth On")
    fun btOff() = rootCmd("svc bluetooth disable", "Bluetooth disabled", "Bluetooth Off")

    fun hotspotOn(): String {
        if (!RootUtil.isRooted()) return needsRoot("Hotspot")
        RootUtil.run("settings put global tether_dun_required 0")
        return if (RootUtil.run("cmd connectivity start-tethering -t 0")) "Hotspot enabled" else "Hotspot failed"
    }

    fun hotspotOff(): String {
        if (!RootUtil.isRooted()) return needsRoot("Hotspot")
        return if (RootUtil.run("cmd connectivity stop-tethering -t 0")) "Hotspot disabled" else "Hotspot failed"
    }

    fun airplaneOn(): String {
        if (!RootUtil.isRooted()) return needsRoot("Airplane mode")
        RootUtil.run("settings put global airplane_mode_on 1")
        return if (RootUtil.run("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true"))
            "Airplane mode ON" else "Airplane mode failed"
    }

    fun airplaneOff(): String {
        if (!RootUtil.isRooted()) return needsRoot("Airplane mode")
        RootUtil.run("settings put global airplane_mode_on 0")
        return if (RootUtil.run("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false"))
            "Airplane mode OFF" else "Airplane mode failed"
    }

    private fun rootCmd(cmd: String, success: String, label: String): String {
        if (!RootUtil.isRooted()) return needsRoot(label)
        return if (RootUtil.run(cmd)) success else "$label command failed"
    }

    private fun needsRoot(feature: String) = "$feature requires root on Android 10+"
}
