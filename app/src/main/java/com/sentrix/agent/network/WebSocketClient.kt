package com.sentrix.agent.network

import android.util.Log
import com.sentrix.agent.util.Prefs
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Persistent WebSocket client with automatic reconnection.
 *
 * Connection lifecycle:
 *  connect() → onOpen → sends auth handshake
 *             → onMessage → dispatches to [onCommand]
 *             → onFailure / onClosed → schedules reconnect
 *  disconnect() → clean close, no reconnect
 */
class WebSocketClient(
    private val onCommand: (action: String, params: JSONObject) -> Unit
) {
    private val TAG = "SentrixWS"

    // ── Reconnect timing ──────────────────────────────────────────────────────
    // Increases with each failed attempt up to MAX_DELAY
    private var reconnectDelay = INITIAL_DELAY_MS
    private var reconnectJob: Job? = null
    private var socket: WebSocket? = null
    private var running = false

    private val scope  = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)   // keeps NAT/proxy connections alive
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)     // 0 = no read timeout (persistent connection)
        .build()

    // ── Public API ────────────────────────────────────────────────────────────

    fun connect() {
        running = true
        reconnectDelay = INITIAL_DELAY_MS
        openSocket()
    }

    fun disconnect() {
        running = false
        reconnectJob?.cancel()
        socket?.close(1000, "Agent stopping")
        socket = null
        Log.i(TAG, "Disconnected cleanly")
    }

    /** Send a JSON message up to the relay (e.g. command result, file URL) */
    fun send(json: JSONObject) {
        socket?.send(json.toString()) ?: Log.w(TAG, "send() called but socket is null")
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun openSocket() {
        val url = Prefs.relayUrl.ifBlank {
            Log.e(TAG, "Relay URL not configured — cannot connect")
            return
        }

        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, listener)
        Log.i(TAG, "Connecting to $url …")
    }

    private val listener = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "Connected ✓")
            reconnectDelay = INITIAL_DELAY_MS

            // Send auth handshake immediately after opening
            val auth = JSONObject().apply {
                put("type",  "auth")
                put("token", Prefs.deviceToken)
                put("name",  Prefs.deviceName)
                put("root",  Prefs.isRooted)
            }
            webSocket.send(auth.toString())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "← $text")
            try {
                val json   = JSONObject(text)
                val type   = json.optString("type")
                val action = json.optString("action")
                val params = json.optJSONObject("params") ?: JSONObject()

                when (type) {
                    "command" -> onCommand(action, params)
                    "ping"    -> webSocket.send(JSONObject().put("type", "pong").toString())
                    else      -> Log.d(TAG, "Unhandled message type: $type")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Message parse error: ${e.message}")
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "Connection failed: ${t.message} — retrying in ${reconnectDelay}ms")
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "Socket closed ($code: $reason)")
            if (running) scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (!running) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(reconnectDelay)
            // Exponential backoff capped at MAX_DELAY
            reconnectDelay = minOf(reconnectDelay * 2, MAX_DELAY_MS)
            if (running) openSocket()
        }
    }

    companion object {
        private const val INITIAL_DELAY_MS = 3_000L   // 3 seconds first retry
        private const val MAX_DELAY_MS     = 60_000L  // cap at 1 minute
    }
}
