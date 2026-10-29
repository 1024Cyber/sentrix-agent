package com.sentrix.agent.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight SharedPreferences wrapper.
 * All persistent config lives here — relay URL, device token, root status, etc.
 */
object Prefs {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("sentrix_prefs", Context.MODE_PRIVATE)
    }

    // ── Relay config ──────────────────────────────────────────────────────────
    /** WebSocket relay URL, e.g. wss://sentrix-relay.up.railway.app */
    var relayUrl: String
        get() = prefs.getString("relay_url", "") ?: ""
        set(v) = prefs.edit().putString("relay_url", v).apply()

    /** Unique token for this device — generated on first setup */
    var deviceToken: String
        get() = prefs.getString("device_token", "") ?: ""
        set(v) = prefs.edit().putString("device_token", v).apply()

    /** Human-readable device name shown in the dashboard */
    var deviceName: String
        get() = prefs.getString("device_name", android.os.Build.MODEL) ?: android.os.Build.MODEL
        set(v) = prefs.edit().putString("device_name", v).apply()

    // ── Agent state ───────────────────────────────────────────────────────────
    /** Whether setup has been completed at least once */
    var setupDone: Boolean
        get() = prefs.getBoolean("setup_done", false)
        set(v) = prefs.edit().putBoolean("setup_done", v).apply()

    /** Whether root access was confirmed at last check */
    var isRooted: Boolean
        get() = prefs.getBoolean("is_rooted", false)
        set(v) = prefs.edit().putBoolean("is_rooted", v).apply()

    /** Whether the agent service is enabled */
    var agentEnabled: Boolean
        get() = prefs.getBoolean("agent_enabled", true)
        set(v) = prefs.edit().putBoolean("agent_enabled", v).apply()

    // ── Upload config ─────────────────────────────────────────────────────────
    /** Supabase project URL for file uploads */
    var supabaseUrl: String
        get() = prefs.getString("supabase_url", "") ?: ""
        set(v) = prefs.edit().putString("supabase_url", v).apply()

    /** Supabase anon key */
    var supabaseKey: String
        get() = prefs.getString("supabase_key", "") ?: ""
        set(v) = prefs.edit().putString("supabase_key", v).apply()
}
