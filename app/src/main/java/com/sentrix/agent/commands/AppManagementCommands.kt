package com.sentrix.agent.commands

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.sentrix.agent.network.WebSocketClient
import com.sentrix.agent.util.RootUtil
import org.json.JSONArray
import org.json.JSONObject

object AppManagementCommands {

    fun closeAllApps(): String {
        if (!RootUtil.isRooted()) return "Close all apps requires root"
        return if (RootUtil.run("am kill-all")) "All background apps closed" else "Failed"
    }

    fun killApp(pkg: String): String {
        if (!RootUtil.isRooted()) return "Kill app requires root"
        if (pkg.isBlank()) return "No package specified"
        return if (RootUtil.run("am force-stop $pkg")) "Killed $pkg" else "Failed"
    }

    fun disableApp(pkg: String): String {
        if (!RootUtil.isRooted()) return "Disable app requires root"
        if (pkg.isBlank()) return "No package specified"
        return if (RootUtil.run("pm disable-user --user 0 $pkg")) "Disabled $pkg" else "Failed"
    }

    fun enableApp(pkg: String): String {
        if (!RootUtil.isRooted()) return "Enable app requires root"
        if (pkg.isBlank()) return "No package specified"
        return if (RootUtil.run("pm enable $pkg")) "Enabled $pkg" else "Failed"
    }

    fun uninstallLock(): String {
        if (!RootUtil.isRooted()) return "Uninstall lock requires root"
        return if (RootUtil.run("settings put global package_verifier_enable 1"))
            "Uninstall protection on" else "Failed"
    }

    fun uninstallUnlock(): String {
        if (!RootUtil.isRooted()) return "Uninstall unlock requires root"
        return if (RootUtil.run("settings put global package_verifier_enable 0"))
            "Uninstall protection off" else "Failed"
    }

    fun clearCache(pkg: String): String {
        if (!RootUtil.isRooted()) return "Clear cache requires root"
        if (pkg.isBlank()) return "No package specified"
        return if (RootUtil.run("pm clear $pkg")) "Cache cleared for $pkg" else "Failed"
    }

    // ── Non-root: list installed user apps ───────────────────────
    fun listApps(ctx: Context, ws: WebSocketClient): String {
        val pm   = ctx.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { mapOf("name" to pm.getApplicationLabel(it).toString(), "pkg" to it.packageName) }

        val json = JSONObject().apply {
            put("type",    "result")
            put("action",  "apps_list")
            put("success", true)
            put("count",   apps.size)
            put("apps",    JSONArray(apps.map {
                JSONObject().apply { put("name", it["name"]); put("pkg", it["pkg"]) }
            }))
        }
        ws.send(json)
        return "App list sent (${apps.size} apps)"
    }

    // ── Non-root: check if specific app is running ────────────────
    fun appStatus(ctx: Context, pkg: String, ws: WebSocketClient): String {
        if (pkg.isBlank()) return "No package specified"
        val am       = ctx.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        val running  = am.runningAppProcesses?.any { it.processName == pkg } ?: false
        val json = JSONObject().apply {
            put("type",    "result")
            put("action",  "app_status")
            put("success", true)
            put("package", pkg)
            put("running", running)
        }
        ws.send(json)
        return "$pkg is ${if (running) "running" else "not running"}"
    }

    // ── Non-root: running processes ───────────────────────────────
    fun runningApps(ctx: Context, ws: WebSocketClient): String {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        val procs = am.runningAppProcesses ?: emptyList()
        val json = JSONObject().apply {
            put("type",    "result")
            put("action",  "apps_running")
            put("success", true)
            put("count",   procs.size)
            put("apps",    JSONArray(procs.map {
                JSONObject().apply { put("name", it.processName); put("pid", it.pid) }
            }))
        }
        ws.send(json)
        return "Running apps sent (${procs.size})"
    }
}
