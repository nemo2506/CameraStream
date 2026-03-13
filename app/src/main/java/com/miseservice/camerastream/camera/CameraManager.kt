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
            cameraDevice = camera
        }

        override fun onDisconnected(device: CameraDevice) {
            device.close()
            cameraDevice = null
        }

        override fun onError(device: CameraDevice, error: Int) {
            device.close()
            cameraDevice = null
        }
    }

    private val imageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        if (image != null) {
            val nv21 = imageToNV21(image)
            image.close()
            _latestFrameData.value = nv21
        }
    }

    @SuppressLint("MissingPermission")
    fun initializeCamera() {
        handlerThread = HandlerThread("CameraThread").apply {
            start()
            handler = Handler(looper)
        }

        // Get camera IDs
        val cameraIds = cameraManager.cameraIdList
        currentCameraId = if (isUsingFrontCamera) {
            cameraIds.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            } ?: ""
        } else {
            cameraIds.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: ""
        }

        if (currentCameraId.isEmpty()) return

        // Create ImageReader for preview
        imageReader = ImageReader.newInstance(1280, 720, android.graphics.ImageFormat.NV21, 2)
        imageReader?.setOnImageAvailableListener(imageAvailableListener, handler)

        // Open camera
        try {
            cameraManager.openCamera(currentCameraId, cameraDeviceCallback, handler)
        } catch (e: Exception) {
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
        captureSessions.forEach { it.close() }
        captureSessions.clear()
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
        handlerThread?.quit()
        handlerThread = null
        handler = null
    }
}



