package com.miseservice.camerastream.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager as AndroidCameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.hardware.display.DisplayManager
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
    private var currentSensorOrientation = 90
    private var currentLensFacing = CameraCharacteristics.LENS_FACING_FRONT
    private var selectedResolution = Size(1280, 720)

    companion object {
        private const val MAX_WIDTH       = 3840
        private const val MAX_HEIGHT      = 2160
        private const val TARGET_RATIO    = 16.0 / 9.0
        private const val RATIO_TOLERANCE = 0.1
    }

    /**
     * Choisit la meilleure résolution YUV_420_888 disponible :
     * - exclut les résolutions supérieures à 4K (3840×2160)
     * - préfère les tailles avec ratio 16/9 (±0.1)
     * - parmi les candidates, prend la plus grande surface
     * - repli sur 1280×720 si rien ne convient
     */
    private fun pickBestSize(map: StreamConfigurationMap): Size {
        val sizes = map.getOutputSizes(android.graphics.ImageFormat.YUV_420_888)
            ?.filter { it.width <= MAX_WIDTH && it.height <= MAX_HEIGHT }
            ?: return Size(1280, 720)

        val widescreen = sizes.filter { s ->
            val ratio = s.width.toDouble() / s.height
            kotlin.math.abs(ratio - TARGET_RATIO) < RATIO_TOLERANCE
        }

        val candidates = widescreen.ifEmpty { sizes }
        val best = candidates.maxByOrNull { it.width.toLong() * it.height }
            ?: return Size(1280, 720)

        Log.d("CameraManager", "Résolutions disponibles : ${sizes.joinToString { "${it.width}x${it.height}" }}")
        Log.d("CameraManager", "Meilleure résolution sélectionnée : ${best.width}x${best.height}")
        return best
    }

    private val _latestFrameData = MutableStateFlow<CameraFrame?>(null)
    val latestFrameData: StateFlow<CameraFrame?> = _latestFrameData

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
                val frame = CameraFrame(
                    nv21 = nv21,
                    width = image.width,
                    height = image.height,
                    rotationDegrees = calculateJpegRotationDegrees()
                )
                image.close()
                _latestFrameData.value = frame
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

            val selectedCharacteristics = cameraManager.getCameraCharacteristics(currentCameraId)
            currentSensorOrientation = selectedCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90
            currentLensFacing = selectedCharacteristics.get(CameraCharacteristics.LENS_FACING)
                ?: CameraCharacteristics.LENS_FACING_FRONT

            // Détection automatique de la meilleure résolution YUV supportée
            val streamMap = selectedCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            selectedResolution = if (streamMap != null) pickBestSize(streamMap) else Size(1280, 720)

            // Create ImageReader avec la résolution optimale détectée
            imageReader = ImageReader.newInstance(
                selectedResolution.width,
                selectedResolution.height,
                android.graphics.ImageFormat.YUV_420_888,
                2
            )
            imageReader?.setOnImageAvailableListener(imageAvailableListener, handler)

            // Open camera
            cameraManager.openCamera(currentCameraId, cameraDeviceCallback, handler)

            Log.d("CameraManager", "Camera initialization started at ${selectedResolution.width}x${selectedResolution.height}")
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

    private fun calculateJpegRotationDegrees(): Int {
        val displayRotationDegrees = getDisplayRotationDegrees()
        val sensorOrientation = currentSensorOrientation

        return if (currentLensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
            (sensorOrientation + displayRotationDegrees) % 360
        } else {
            (sensorOrientation - displayRotationDegrees + 360) % 360
        }
    }

    private fun getDisplayRotationDegrees(): Int {
        return try {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val rotation = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)?.rotation
                ?: Surface.ROTATION_0

            when (rotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            Log.w("CameraManager", "Cannot read display rotation, fallback to 0: ${e.message}")
            0
        }
    }

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
