package com.miseservice.camerastream.webrtc

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as AndroidCameraManager
import android.util.Log
import android.view.OrientationEventListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SessionDescription
import org.webrtc.SdpObserver
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class WebRtcEngine(
    context: Context
) : WebRtcSessionGateway {
    private val appContext = context.applicationContext
    private val androidCameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as AndroidCameraManager

    private val eglBase: EglBase = EglBase.create()
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var cameraEnumerator: Camera2Enumerator? = null
    private var currentCameraId: String? = null
    private val peerConnections = ConcurrentHashMap<String, PeerConnection>()
    private val sessionCounter = AtomicInteger(0)
    @Volatile
    private var currentSensorOrientation = 90
    @Volatile
    private var currentLensFacing = CameraCharacteristics.LENS_FACING_FRONT
    @Volatile
    private var currentDisplayRotationDegrees = 0
    @Volatile
    private var lastForegroundDisplayRotationDegrees = 0
    @Volatile
    private var forcePortraitMode = false
    @Volatile
    private var currentCaptureWidth = 1280
    @Volatile
    private var currentCaptureHeight = 720
    @Volatile
    private var currentCaptureFps = 30

    private val orientationListener: OrientationEventListener by lazy {
        object : OrientationEventListener(appContext) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                currentDisplayRotationDegrees = when {
                    orientation <= 45 || orientation > 315 -> 0
                    orientation in 46..135 -> 270
                    orientation in 136..225 -> 180
                    else -> 90
                }
                applyCurrentFrameRotation()
            }
        }
    }

    companion object {
        private const val TAG = "WebRtcEngine"
    }

    override fun activeSessionCount(): Int = peerConnections.size

    private val initialized = AtomicBoolean(false)
    @Volatile
    private var preferFrontCamera = true

    fun start() {
        if (initialized.getAndSet(true)) return
        initializeFactory()
        startVideoCapture()
        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable()
            applyCurrentFrameRotation()
        }
    }

    fun stop() {
        peerConnections.values.forEach { connection ->
            try {
                connection.close()
            } catch (_: Exception) {
            }
        }
        peerConnections.clear()

        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) {
        }
        videoCapturer?.dispose()
        videoCapturer = null
        cameraEnumerator = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        videoTrack?.dispose()
        videoTrack = null

        videoSource?.dispose()
        videoSource = null

        peerConnectionFactory?.dispose()
        peerConnectionFactory = null

        orientationListener.disable()

        eglBase.release()
        initialized.set(false)
    }

    fun switchCamera() {
        val capturer = videoCapturer ?: return
        capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                preferFrontCamera = isFrontCamera
                refreshSelectedCameraCharacteristics(isFrontCamera)
                reconfigureCaptureForSelectedCamera()
                applyCurrentFrameRotation()
            }

            override fun onCameraSwitchError(errorDescription: String?) {
                // Keep previous state when switch fails.
            }
        })
    }

    fun setForcePortraitMode(enabled: Boolean) {
        if (enabled && !forcePortraitMode) {
            // Save last known display orientation before forcing portrait in background.
            lastForegroundDisplayRotationDegrees = currentDisplayRotationDegrees
        } else if (!enabled && forcePortraitMode) {
            // Restore previous portrait/landscape state on return to foreground.
            currentDisplayRotationDegrees = lastForegroundDisplayRotationDegrees
        }
        forcePortraitMode = enabled
        applyCurrentFrameRotation()
    }

    override suspend fun createAnswer(offerSdp: String): String = withContext(Dispatchers.Default) {
        ensureStarted()
        val factory = peerConnectionFactory ?: error("PeerConnectionFactory non initialisee")
        val currentTrack = videoTrack ?: error("Video track non initialisee")

        val gatherComplete = CompletableDeferred<Unit>()

        val rtcConfig = PeerConnection.RTCConfiguration(
            mutableListOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val sessionId = "viewer-${sessionCounter.incrementAndGet()}"
        lateinit var connection: PeerConnection

        connection = factory.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    if (
                        state == PeerConnection.IceConnectionState.FAILED ||
                        state == PeerConnection.IceConnectionState.DISCONNECTED ||
                        state == PeerConnection.IceConnectionState.CLOSED
                    ) {
                        removeAndCloseSession(sessionId)
                    }
                }
                override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                    if (state == PeerConnection.IceGatheringState.COMPLETE && !gatherComplete.isCompleted) {
                        gatherComplete.complete(Unit)
                    }
                }

                override fun onIceCandidate(candidate: IceCandidate?) = Unit
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
                override fun onAddStream(stream: org.webrtc.MediaStream?) = Unit
                override fun onRemoveStream(stream: org.webrtc.MediaStream?) = Unit
                override fun onDataChannel(dataChannel: org.webrtc.DataChannel?) = Unit
                override fun onRenegotiationNeeded() = Unit
                override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out org.webrtc.MediaStream>?) = Unit
            }
        ) ?: error("Impossible de creer PeerConnection")

        peerConnections[sessionId] = connection
        Log.i(TAG, "Session $sessionId started — active viewers: ${peerConnections.size}")
        connection.addTrack(currentTrack, listOf("camera-stream"))

        val remoteOffer = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        setSessionDescription(
            setter = { observer, description -> connection.setRemoteDescription(observer, description) },
            description = remoteOffer
        )

        val answer = createAnswerDescription(connection)
        setSessionDescription(
            setter = { observer, description -> connection.setLocalDescription(observer, description) },
            description = answer
        )

        // Non-trickle ICE: wait briefly so localDescription includes ICE candidates.
        withTimeoutOrNull(3000) {
            gatherComplete.await()
        }

        val local = connection.localDescription
        local?.description ?: answer.description
    }

    private fun removeAndCloseSession(sessionId: String) {
        val existing = peerConnections.remove(sessionId) ?: return
        try { existing.close() } catch (_: Exception) {}
        Log.i(TAG, "Session $sessionId removed — active viewers: ${peerConnections.size}")
    }

    private fun ensureStarted() {
        if (!initialized.get()) {
            start()
        }
    }

    private fun initializeFactory() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(appContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )

        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase.eglBaseContext,
            true,
            true
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    private fun startVideoCapture() {
        val factory = peerConnectionFactory ?: error("PeerConnectionFactory non initialisee")
        val enumerator = Camera2Enumerator(appContext)
        cameraEnumerator = enumerator
        val selected = selectCamera(enumerator)
            ?: error("Aucune camera compatible WebRTC")
        currentCameraId = selected
        updateCameraCharacteristics(selected)
        val captureFormat = selectBestCaptureFormat(enumerator, selected)

        val capturer = enumerator.createCapturer(selected, null)
            ?: error("Impossible de creer le capturer WebRTC")

        surfaceTextureHelper = SurfaceTextureHelper.create("WebRtcCaptureThread", eglBase.eglBaseContext)
        applyCurrentFrameRotation()
        videoSource = factory.createVideoSource(false)
        capturer.initialize(surfaceTextureHelper, appContext, videoSource?.capturerObserver)
        currentCaptureWidth = captureFormat.width
        currentCaptureHeight = captureFormat.height
        currentCaptureFps = captureFormat.fps
        Log.i(
            TAG,
            "Starting capture on $selected at ${captureFormat.width}x${captureFormat.height}@${captureFormat.fps}fps"
        )
        capturer.startCapture(captureFormat.width, captureFormat.height, captureFormat.fps)

        videoTrack = factory.createVideoTrack("camera-video-track", videoSource)
        videoTrack?.setEnabled(true)
        videoCapturer = capturer
    }

    private fun refreshSelectedCameraCharacteristics(isFrontCamera: Boolean) {
        val enumerator = cameraEnumerator ?: return
        val cameraId = if (isFrontCamera) {
            enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
        } else {
            enumerator.deviceNames.firstOrNull { enumerator.isBackFacing(it) }
        } ?: return
        currentCameraId = cameraId
        updateCameraCharacteristics(cameraId)
    }

    private fun reconfigureCaptureForSelectedCamera() {
        val enumerator = cameraEnumerator ?: return
        val capturer = videoCapturer ?: return
        val cameraId = currentCameraId ?: return
        val captureFormat = selectBestCaptureFormat(enumerator, cameraId)

        if (
            captureFormat.width == currentCaptureWidth &&
            captureFormat.height == currentCaptureHeight &&
            captureFormat.fps == currentCaptureFps
        ) {
            return
        }

        try {
            capturer.stopCapture()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to stop capture before reconfigure: ${e.message}")
        }

        try {
            currentCaptureWidth = captureFormat.width
            currentCaptureHeight = captureFormat.height
            currentCaptureFps = captureFormat.fps
            Log.i(
                TAG,
                "Reconfiguring capture on $cameraId to ${captureFormat.width}x${captureFormat.height}@${captureFormat.fps}fps"
            )
            capturer.startCapture(captureFormat.width, captureFormat.height, captureFormat.fps)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart capture on new camera format: ${e.message}", e)
        }
    }

    private data class CaptureConfig(
        val width: Int,
        val height: Int,
        val fps: Int
    )

    private fun selectBestCaptureFormat(
        enumerator: Camera2Enumerator,
        cameraId: String
    ): CaptureConfig {
        val formats = try {
            enumerator.getSupportedFormats(cameraId).orEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query supported formats for $cameraId: ${e.message}")
            emptyList<CameraEnumerationAndroid.CaptureFormat>()
        }

        val best = formats.maxWithOrNull(
            compareBy<CameraEnumerationAndroid.CaptureFormat> { it.width.toLong() * it.height.toLong() }
                .thenBy { it.framerate.max }
        )

        if (best == null) {
            return CaptureConfig(width = 1280, height = 720, fps = 30)
        }

        val fps = (best.framerate.max / 1000)
            .takeIf { it > 0 }
            ?.coerceAtMost(30)
            ?: 30

        return CaptureConfig(
            width = best.width,
            height = best.height,
            fps = fps
        )
    }

    private fun updateCameraCharacteristics(cameraId: String) {
        try {
            val characteristics = androidCameraManager.getCameraCharacteristics(cameraId)
            currentSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90
            currentLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                ?: CameraCharacteristics.LENS_FACING_FRONT
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read camera characteristics for $cameraId: ${e.message}")
        }
    }

    private fun applyCurrentFrameRotation() {
        val effectiveDisplayRotation = if (forcePortraitMode) 0 else currentDisplayRotationDegrees
        val rotationDegrees = if (currentLensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
            (currentSensorOrientation + effectiveDisplayRotation) % 360
        } else {
            (currentSensorOrientation - effectiveDisplayRotation + 360) % 360
        }
        surfaceTextureHelper?.setFrameRotation(rotationDegrees)
    }

    private fun selectCamera(enumerator: Camera2Enumerator): String? {
        val devices = enumerator.deviceNames.toList()
        if (devices.isEmpty()) return null

        val preferred = if (preferFrontCamera) {
            devices.firstOrNull { enumerator.isFrontFacing(it) }
        } else {
            devices.firstOrNull { enumerator.isBackFacing(it) }
        }

        if (preferred != null) return preferred

        return devices.firstOrNull { enumerator.isBackFacing(it) }
            ?: devices.firstOrNull { enumerator.isFrontFacing(it) }
            ?: devices.first()
    }

    private suspend fun createAnswerDescription(connection: PeerConnection?): SessionDescription {
        val deferred = CompletableDeferred<SessionDescription>()
        connection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) {
                if (description != null && !deferred.isCompleted) {
                    deferred.complete(description)
                } else if (!deferred.isCompleted) {
                    deferred.completeExceptionally(IllegalStateException("Answer SDP vide"))
                }
            }

            override fun onSetSuccess() = Unit
            override fun onCreateFailure(error: String?) {
                if (!deferred.isCompleted) {
                    deferred.completeExceptionally(IllegalStateException(error ?: "Erreur createAnswer"))
                }
            }

            override fun onSetFailure(error: String?) = Unit
        }, MediaConstraints())

        return deferred.await()
    }

    private suspend fun setSessionDescription(
        setter: (SdpObserver, SessionDescription) -> Unit,
        description: SessionDescription
    ) {
        val deferred = CompletableDeferred<Unit>()
        setter(
            object : SdpObserver {
                override fun onSetSuccess() {
                    if (!deferred.isCompleted) deferred.complete(Unit)
                }

                override fun onSetFailure(error: String?) {
                    if (!deferred.isCompleted) {
                        deferred.completeExceptionally(IllegalStateException(error ?: "Erreur setSessionDescription"))
                    }
                }

                override fun onCreateSuccess(p0: SessionDescription?) = Unit
                override fun onCreateFailure(p0: String?) = Unit
            },
            description
        )
        deferred.await()
    }
}

