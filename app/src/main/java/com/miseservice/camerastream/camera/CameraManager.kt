package com.miseservice.camerastream.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager as AndroidCameraManager
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CameraManager(
    private val context: Context
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as AndroidCameraManager
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var captureSessions = mutableListOf<CameraCaptureSession>()

    private var currentCameraId = ""
    private var isUsingFrontCamera = true

    private val _latestFrameData = MutableStateFlow<ByteArray?>(null)
    val latestFrameData: StateFlow<ByteArray?> = _latestFrameData

    private val cameraDeviceCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d("CameraManager", "Camera opened")
            cameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(device: CameraDevice) {
            Log.d("CameraManager", "Camera disconnected")
            device.close()
            cameraDevice = null
        }

        override fun onError(device: CameraDevice, error: Int) {
            Log.e("CameraManager", "Camera error: $error")
            device.close()
            cameraDevice = null
        }
    }

    private val imageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        try {
            val image = reader.acquireLatestImage()
            if (image != null) {
                val nv21 = imageToNV21(image)
                image.close()
                _latestFrameData.value = nv21
            }
        } catch (e: Exception) {
            Log.e("CameraManager", "Error processing image: ${e.message}", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun initializeCamera() {
        try {
            handlerThread = HandlerThread("CameraThread").apply {
                start()
                handler = Handler(looper)
            }

            // Get camera IDs
            val cameraIds = cameraManager.cameraIdList
            if (cameraIds.isEmpty()) {
                Log.e("CameraManager", "No cameras available")
                return
            }

            currentCameraId = if (isUsingFrontCamera) {
                cameraIds.firstOrNull { id ->
                    try {
                        val characteristics = cameraManager.getCameraCharacteristics(id)
                        characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
                    } catch (e: Exception) {
                        false
                    }
                } ?: cameraIds.first()
            } else {
                cameraIds.firstOrNull { id ->
                    try {
                        val characteristics = cameraManager.getCameraCharacteristics(id)
                        characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                    } catch (e: Exception) {
                        false
                    }
                } ?: cameraIds.first()
            }

            Log.d("CameraManager", "Using camera: $currentCameraId (front=$isUsingFrontCamera)")

            // Create ImageReader for preview (use YUV format)
            imageReader = ImageReader.newInstance(1280, 720, android.graphics.ImageFormat.YUV_420_888, 2)
            imageReader?.setOnImageAvailableListener(imageAvailableListener, handler)

            // Open camera
            cameraManager.openCamera(currentCameraId, cameraDeviceCallback, handler)

            Log.d("CameraManager", "Camera initialization started")
        } catch (e: Exception) {
            Log.e("CameraManager", "Error initializing camera: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun createCaptureSession() {
        try {
            if (cameraDevice == null || imageReader == null || handler == null) {
                Log.e("CameraManager", "Camera device, imageReader or handler is null")
                return
            }

            val surface = imageReader!!.surface
            cameraDevice!!.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d("CameraManager", "Capture session configured")
                        captureSessions.add(session)
                        val captureRequest = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(surface)
                        }
                        session.setRepeatingRequest(captureRequest.build(), null, handler)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("CameraManager", "Capture session configuration failed")
                        session.close()
                    }
                },
                handler
            )
        } catch (e: Exception) {
            Log.e("CameraManager", "Error creating capture session: ${e.message}", e)
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun switchCamera() {
        isUsingFrontCamera = !isUsingFrontCamera
        release()
        initializeCamera()
    }

    fun isFrontCamera(): Boolean = isUsingFrontCamera

    private fun imageToNV21(image: Image): ByteArray {
        val planes = image.planes
        val ySize = planes[0].buffer.remaining()
        val uSize = planes[1].buffer.remaining()
        val vSize = planes[2].buffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        planes[0].buffer.get(nv21, 0, ySize)
        planes[2].buffer.get(nv21, ySize, vSize)
        planes[1].buffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }

    fun release() {
        try {
            captureSessions.forEach {
                try {
                    it.close()
                } catch (e: Exception) {
                    Log.e("CameraManager", "Error closing session: ${e.message}")
                }
            }
            captureSessions.clear()
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
            handlerThread?.quit()
            handlerThread = null
            handler = null
            Log.d("CameraManager", "Camera released")
        } catch (e: Exception) {
            Log.e("CameraManager", "Error releasing camera: ${e.message}", e)
        }
    }
}
