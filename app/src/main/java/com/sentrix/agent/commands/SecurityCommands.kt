package com.sentrix.agent.commands

import android.content.Context
import android.media.AudioManager
import android.os.*
import android.telephony.SmsManager
import android.util.Log
import com.sentrix.agent.util.RootUtil

object SecurityCommands {
    private const val TAG = "SecurityCmds"

    // ── Lock screen (standard — works without root via KeyguardManager) ──────
    fun lockScreen(ctx: Context): String {
        return if (RootUtil.isRooted()) {
            if (RootUtil.run("input keyevent 223")) "Screen locked" else "Lock failed"
        } else {
            // On unrooted devices DevicePolicyManager.lockNow() requires Device Admin
            // We trigger it via power key event simulation
            try {
                val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
                @Suppress("DEPRECATION")
                if (!pm.isInteractive) return "Screen already off"
                RootUtil.run("input keyevent 26") // KEYCODE_POWER
                "Lock screen triggered"
            } catch (e: Exception) { "Lock failed: ${e.message}" }
        }
    }

    // ── Screen on/off ─────────────────────────────────────────────────────────
    fun screenOff(): String {
        if (!RootUtil.isRooted()) return "Screen off requires root"
        return if (RootUtil.run("input keyevent 223")) "Screen off" else "Failed"
    }

    fun screenOn(): String {
        if (!RootUtil.isRooted()) return "Screen on requires root"
        return if (RootUtil.run("input keyevent 224")) "Screen on" else "Failed"
    }

    // ── Touch block ───────────────────────────────────────────────────────────
    fun touchBlock(): String {
        if (!RootUtil.isRooted()) return "Touch block requires root"
        return if (RootUtil.run("settings put system pointer_location 1")) "Touch blocked" else "Failed"
    }

    fun touchUnblock(): String {
        if (!RootUtil.isRooted()) return "Touch unblock requires root"
        return if (RootUtil.run("settings put system pointer_location 0")) "Touch unblocked" else "Failed"
    }

    // ── Power menu ────────────────────────────────────────────────────────────
    fun powerMenuOff(): String {
        if (!RootUtil.isRooted()) return "Power menu control requires root"
        return if (RootUtil.run("settings put global policy_control immersive.full=*"))
            "Power menu disabled" else "Failed"
    }

    fun powerMenuOn(): String {
        if (!RootUtil.isRooted()) return "Power menu control requires root"
        return if (RootUtil.run("settings put global policy_control null"))
            "Power menu enabled" else "Failed"
    }

    // ── Reboot / reset ────────────────────────────────────────────────────────
    fun reboot(): String {
        if (!RootUtil.isRooted()) return "Reboot requires root"
        RootUtil.run("reboot")
        return "Rebooting…"
    }

    fun factoryReset(): String {
        if (!RootUtil.isRooted()) return "Factory reset requires root"
        RootUtil.run("am broadcast -a android.intent.action.MASTER_CLEAR")
        return "Factory reset initiated"
    }

    // ── Alert SMS — send owner's number a message with location ──────────────
    fun sendAlertSms(ctx: Context, number: String, message: String): String {
        return try {
            val sms = if (Build.VERSION.SDK_INT >= 31)
                ctx.getSystemService(SmsManager::class.java)
            else @Suppress("DEPRECATION") SmsManager.getDefault()
            sms.sendTextMessage(number, null, message, null, null)
            "Alert SMS sent to $number"
        } catch (e: Exception) {
            Log.e(TAG, "SMS failed: ${e.message}")
            "SMS failed: ${e.message}"
        }
    }

    // ── Lock screen message (device owner mode required for full effect) ──────
    fun setLockMessage(ctx: Context, message: String): String {
        return if (RootUtil.isRooted()) {
            val ok = RootUtil.run("settings put global lock_screen_owner_info_enabled 1") &&
                     RootUtil.run("settings put global lock_screen_owner_info \"$message\"")
            if (ok) "Lock message set" else "Failed"
        } else {
            "Lock message requires root"
        }
    }

    // ── Encryption check ──────────────────────────────────────────────────────
    fun encryptionCheck(ctx: Context): String {
        val dm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        return when (dm.storageEncryptionStatus) {
            android.app.admin.DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE           -> "Encrypted ✓"
            android.app.admin.DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY -> "Encrypted (default key)"
            android.app.admin.DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER  -> "Encrypted per-user ✓"
            android.app.admin.DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE         -> "Not encrypted"
            android.app.admin.DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED      -> "Encryption not supported"
            else -> "Unknown"
        }
    }
}
