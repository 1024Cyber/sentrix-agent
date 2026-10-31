package com.sentrix.agent.commands

import android.content.Context
import android.media.AudioManager
import android.os.*

object AudioCommands {

    fun volRingMax(ctx: Context): String {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(AudioManager.STREAM_RING, am.getStreamMaxVolume(AudioManager.STREAM_RING), 0)
        return "Ring volume maxed"
    }

    fun volRingMute(ctx: Context): String {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
        return "Ring muted"
    }

    fun volMediaMax(ctx: Context): String {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
        return "Media volume maxed"
    }

    fun volMediaMute(ctx: Context): String {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        return "Media muted"
    }

    fun vibrate(ctx: Context): String {
        val effect = VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ctx.getSystemService(VibratorManager::class.java).defaultVibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            (ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(effect)
        }
        return "Vibrating"
    }
}
