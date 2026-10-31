package com.sentrix.agent.commands

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import android.os.VibrationEffect
import android.util.Log
import com.sentrix.agent.util.Prefs
import com.sentrix.agent.util.RootUtil

object AntiTheftCommands {
    private const val TAG = "AntiTheft"
    private var sirenPlayer: MediaPlayer? = null
    private var theftModeActive = false

    // ── Alarm / Siren ─────────────────────────────────────────────────────────
    fun alarmStart(ctx: Context): String {
        return try {
            // Max volume first
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_RING,
                am.getStreamMaxVolume(AudioManager.STREAM_RING), 0)

            sirenPlayer?.release()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            sirenPlayer = MediaPlayer().apply {
                setDataSource(ctx, uri)
                setAudioStreamType(AudioManager.STREAM_RING)
                isLooping = true
                prepare()
                start()
            }
            // Vibrate continuously
            vibrateContinuous(ctx)
            "Alarm triggered"
        } catch (e: Exception) {
            Log.e(TAG, "Alarm start: ${e.message}")
            "Alarm failed: ${e.message}"
        }
    }

    fun alarmStop(ctx: Context): String {
        sirenPlayer?.apply { stop(); release() }
        sirenPlayer = null
        stopVibrate(ctx)
        return "Alarm stopped"
    }

    // ── Anti-theft mode ───────────────────────────────────────────────────────
    fun theftModeOn(ctx: Context): String {
        theftModeActive = true
        // Max volume, keep screen off, enable intruder detection
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(AudioManager.STREAM_RING, am.getStreamMaxVolume(AudioManager.STREAM_RING), 0)
        Prefs.intruderDetectionEnabled = true
        if (RootUtil.isRooted()) RootUtil.run("input keyevent 223")
        return "Anti-Theft mode ON"
    }

    fun theftModeOff(ctx: Context): String {
        theftModeActive = false
        alarmStop(ctx)
        Prefs.intruderDetectionEnabled = false
        return "Anti-Theft mode OFF"
    }

    // ── SIM change alert ──────────────────────────────────────────────────────
    fun simAlertOn(): String {
        Prefs.simChangeAlertEnabled = true
        return "SIM change alert enabled"
    }

    fun simAlertOff(): String {
        Prefs.simChangeAlertEnabled = false
        return "SIM change alert disabled"
    }

    // ── Intruder detection ────────────────────────────────────────────────────
    fun intruderOn(): String {
        Prefs.intruderDetectionEnabled = true
        return "Intruder detection enabled"
    }

    fun intruderOff(): String {
        Prefs.intruderDetectionEnabled = false
        return "Intruder detection disabled"
    }

    // ── Wrong PIN photo ───────────────────────────────────────────────────────
    fun wrongPinPhotoOn(): String {
        Prefs.wrongPinPhotoEnabled = true
        return "Wrong PIN photo capture enabled"
    }

    fun wrongPinPhotoOff(): String {
        Prefs.wrongPinPhotoEnabled = false
        return "Wrong PIN photo capture disabled"
    }

    // ── Safe mode ─────────────────────────────────────────────────────────────
    fun triggerSafeMode(): String {
        if (!RootUtil.isRooted()) return "Safe mode requires root"
        return if (RootUtil.run("reboot safe-mode")) "Rebooting into safe mode" else "Safe mode failed"
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun vibrateContinuous(ctx: Context) {
        val pattern = longArrayOf(0, 500, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = ctx.getSystemService(VibratorManager::class.java).defaultVibrator
            vm.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            (ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                .vibrate(pattern, 0)
        }
    }

    private fun stopVibrate(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ctx.getSystemService(VibratorManager::class.java).defaultVibrator.cancel()
        } else {
            @Suppress("DEPRECATION")
            (ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        }
    }
}
