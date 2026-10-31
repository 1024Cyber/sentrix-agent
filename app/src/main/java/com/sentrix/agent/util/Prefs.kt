package com.sentrix.agent.util

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private lateinit var prefs: SharedPreferences

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("sentrix_prefs", Context.MODE_PRIVATE)
    }

    // ── Relay ─────────────────────────────────────────────────────
    var relayUrl: String
        get() = prefs.getString("relay_url", "") ?: ""
        set(v) = prefs.edit().putString("relay_url", v).apply()

    var deviceToken: String
        get() = prefs.getString("device_token", "") ?: ""
        set(v) = prefs.edit().putString("device_token", v).apply()

    var deviceName: String
        get() = prefs.getString("device_name", android.os.Build.MODEL) ?: android.os.Build.MODEL
        set(v) = prefs.edit().putString("device_name", v).apply()

    // ── Agent state ───────────────────────────────────────────────
    var setupDone: Boolean
        get() = prefs.getBoolean("setup_done", false)
        set(v) = prefs.edit().putBoolean("setup_done", v).apply()

    var isRooted: Boolean
        get() = prefs.getBoolean("is_rooted", false)
        set(v) = prefs.edit().putBoolean("is_rooted", v).apply()

    var agentEnabled: Boolean
        get() = prefs.getBoolean("agent_enabled", true)
        set(v) = prefs.edit().putBoolean("agent_enabled", v).apply()

    // ── Upload ────────────────────────────────────────────────────
    var supabaseUrl: String
        get() = prefs.getString("supabase_url", "") ?: ""
        set(v) = prefs.edit().putString("supabase_url", v).apply()

    var supabaseKey: String
        get() = prefs.getString("supabase_key", "") ?: ""
        set(v) = prefs.edit().putString("supabase_key", v).apply()

    // ── Feature flags ─────────────────────────────────────────────
    var intruderDetectionEnabled: Boolean
        get() = prefs.getBoolean("intruder_detection", false)
        set(v) = prefs.edit().putBoolean("intruder_detection", v).apply()

    var simChangeAlertEnabled: Boolean
        get() = prefs.getBoolean("sim_change_alert", false)
        set(v) = prefs.edit().putBoolean("sim_change_alert", v).apply()

    var wrongPinPhotoEnabled: Boolean
        get() = prefs.getBoolean("wrong_pin_photo", false)
        set(v) = prefs.edit().putBoolean("wrong_pin_photo", v).apply()

    // ── Tracking ──────────────────────────────────────────────────
    var liveTrackingEnabled: Boolean
        get() = prefs.getBoolean("live_tracking", false)
        set(v) = prefs.edit().putBoolean("live_tracking", v).apply()
}
