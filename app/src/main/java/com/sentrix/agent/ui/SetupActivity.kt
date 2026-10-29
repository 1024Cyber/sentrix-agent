package com.sentrix.agent.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sentrix.agent.AgentService
import com.sentrix.agent.databinding.ActivitySetupBinding
import com.sentrix.agent.util.Prefs
import com.sentrix.agent.util.RootUtil
import java.util.UUID

/**
 * SetupActivity — shown on first launch and accessible from the notification.
 *
 * Lets the user configure:
 *  - Relay WebSocket URL
 *  - Device name
 *  - Supabase URL + key (for file uploads)
 *
 * Generates a unique device token on first run.
 * After saving, starts AgentService.
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        populateFields()
        checkRootStatus()
        setupListeners()
    }

    private fun populateFields() {
        binding.inputRelayUrl.setText(Prefs.relayUrl)
        binding.inputDeviceName.setText(Prefs.deviceName)
        binding.inputSupabaseUrl.setText(Prefs.supabaseUrl)
        binding.inputSupabaseKey.setText(Prefs.supabaseKey)

        // Show generated token (read-only)
        if (Prefs.deviceToken.isBlank()) {
            Prefs.deviceToken = UUID.randomUUID().toString()
        }
        binding.textToken.text = "Device Token: ${Prefs.deviceToken}"

        // Show current agent state
        updateAgentStatus()
    }

    private fun checkRootStatus() {
        val rooted = RootUtil.isRooted()
        Prefs.isRooted = rooted
        binding.textRootStatus.text = if (rooted)
            "✓ Root detected — all features available"
        else
            "✗ No root — standard features only"

        binding.textRootStatus.setTextColor(
            if (rooted) 0xFF22C55E.toInt() else 0xFFF59E0B.toInt()
        )
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener { saveAndStart() }
        binding.btnCopyToken.setOnClickListener { copyToken() }
        binding.switchAgent.setOnCheckedChangeListener { _, checked ->
            Prefs.agentEnabled = checked
            if (checked) startAgent() else stopAgent()
            updateAgentStatus()
        }
    }

    private fun saveAndStart() {
        val relayUrl     = binding.inputRelayUrl.text.toString().trim()
        val deviceName   = binding.inputDeviceName.text.toString().trim()
        val supabaseUrl  = binding.inputSupabaseUrl.text.toString().trim()
        val supabaseKey  = binding.inputSupabaseKey.text.toString().trim()

        if (relayUrl.isBlank()) {
            binding.inputRelayUrl.error = "Relay URL required"
            return
        }

        if (!relayUrl.startsWith("ws://") && !relayUrl.startsWith("wss://")) {
            binding.inputRelayUrl.error = "Must start with ws:// or wss://"
            return
        }

        Prefs.relayUrl    = relayUrl
        Prefs.deviceName  = deviceName.ifBlank { android.os.Build.MODEL }
        Prefs.supabaseUrl = supabaseUrl
        Prefs.supabaseKey = supabaseKey
        Prefs.setupDone   = true

        Toast.makeText(this, "Config saved", Toast.LENGTH_SHORT).show()

        startAgent()
        updateAgentStatus()
    }

    private fun startAgent() {
        val intent = Intent(this, AgentService::class.java)
        startForegroundService(intent)
        binding.textStatus.text = "Agent: Running"
        binding.textStatus.setTextColor(0xFF22C55E.toInt())
    }

    private fun stopAgent() {
        val intent = Intent(this, AgentService::class.java)
        stopService(intent)
        binding.textStatus.text = "Agent: Stopped"
        binding.textStatus.setTextColor(0xFFEF4444.toInt())
    }

    private fun updateAgentStatus() {
        binding.switchAgent.isChecked = Prefs.agentEnabled
    }

    private fun copyToken() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(
            android.content.ClipData.newPlainText("Device Token", Prefs.deviceToken)
        )
        Toast.makeText(this, "Token copied", Toast.LENGTH_SHORT).show()
    }
}
