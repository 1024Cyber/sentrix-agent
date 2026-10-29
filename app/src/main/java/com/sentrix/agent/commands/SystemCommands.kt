package com.sentrix.agent.commands

import android.app.KeyguardManager
import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.*
import android.util.Log
import com.sentrix.agent.util.RootUtil

/**
 * System-level commands.
 * Standard commands work without root.
 * Root-gated commands are clearly marked — they check RootUtil.isRooted() first.
 */
object SystemCommands {

    private const val TAG = "SystemCmds"

    // ── Flashlight ────────────────────────────────────────────────────────────
    // Standard — works without root

    fun torchOn(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull() ?: return "No camera found"
            cm.setTorchMode(id, true)
            "Flashlight ON"
        } catch (e: Exception) { "Flashlight error: ${e.message}" }
    }

    fun torchOff(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull() ?: return "No camera found"
            cm.setTorchMode(id, false)
            "Flashlight OFF"
        } catch (e: Exception) { "Flashlight error: ${e.message}" }
    }

    // ── Vibrate ───────────────────────────────────────────────────────────────

    fun vibrate(context: Context, durationMs: Long = 1000): String {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        return "Vibrating for ${durationMs}ms"
    }

    // ── Volume ────────────────────────────────────────────────────────────────

    fun setVolume(context: Context, streamType: Int, level: Int): String {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(streamType)
        val clamped = level.coerceIn(0, max)
        am.setStreamVolume(streamType, clamped, 0)
        return "Volume set to $clamped/$max"
    }

    fun loudAlarmOn(context: Context): String {
        val am  = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_RING)
        am.setStreamVolume(AudioManager.STREAM_RING, max, 0)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, max, 0)
        return "Volume maxed — alarm active"
    }

    // ── Screen ────────────────────────────────────────────────────────────────

    fun screenOn(): String {
        return if (RootUtil.isRooted()) {
            val ok = RootUtil.run("input keyevent 26")  // KEYCODE_POWER
            if (ok) "Screen toggled" else "Screen toggle failed"
        } else {
            // Without root we can only request via PowerManager flags (limited)
            "Screen on requires root for silent operation"
        }
    }

    fun screenOff(): String {
        return if (RootUtil.isRooted()) {
            val ok = RootUtil.run("input keyevent 223")  // KEYCODE_SLEEP
            if (ok) "Screen turned off" else "Screen off failed"
        } else {
            "Screen off requires root"
        }
    }

    // ── ROOT-ONLY system commands ─────────────────────────────────────────────

    /** Kill a specific app by package name */
    fun killApp(packageName: String): String {
        return if (RootUtil.isRooted()) {
            val ok = RootUtil.run("am force-stop $packageName")
            if (ok) "Killed $packageName" else "Kill failed"
        } else { "Kill app requires root" }
    }

    /** Kill all non-system apps */
    fun killAllApps(): String {
        return if (RootUtil.isRooted()) {
            // am kill-all kills background apps; force-stop needs individual packages
            val ok = RootUtil.run("am kill-all")
            if (ok) "All background apps killed" else "Kill all failed"
        } else { "Kill all requires root" }
    }

    /** Reboot the device */
    fun reboot(): String {
        return if (RootUtil.isRooted()) {
            RootUtil.run("reboot")
            "Rebooting…"
        } else { "Reboot requires root" }
    }

    /** Wipe — factory reset. Extreme action, double-confirmed at dashboard level */
    fun wipeDevice(): String {
        return if (RootUtil.isRooted()) {
            RootUtil.run("am broadcast -a android.intent.action.MASTER_CLEAR")
            "Factory reset initiated"
        } else { "Wipe requires root" }
    }

    /** Clear an app's data */
    fun clearAppData(packageName: String): String {
        return if (RootUtil.isRooted()) {
            val ok = RootUtil.run("pm clear $packageName")
            if (ok) "Cleared data for $packageName" else "Clear failed"
        } else { "Clear app data requires root" }
    }

    /** Lock the screen immediately */
    fun lockScreen(context: Context): String {
        return if (RootUtil.isRooted()) {
            val ok = RootUtil.run("input keyevent 223")
            if (ok) "Screen locked" else "Lock failed"
        } else {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            // Without Device Admin or root, we can't force-lock — log only
            Log.w(TAG, "Lock screen without root — not possible without Device Admin")
            "Lock screen requires root or Device Admin"
        }
    }
}
