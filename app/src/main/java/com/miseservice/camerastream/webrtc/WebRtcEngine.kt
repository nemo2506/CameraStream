package com.miseservice.camerastream.webrtc

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

    private val eglBase: EglBase = EglBase.create()
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private val peerConnections = ConcurrentHashMap<String, PeerConnection>()
    private val sessionCounter = AtomicInteger(0)

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

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        videoTrack?.dispose()
        videoTrack = null

        videoSource?.dispose()
        videoSource = null

        peerConnectionFactory?.dispose()
        peerConnectionFactory = null

        eglBase.release()
        initialized.set(false)
    }

    fun switchCamera() {
        val capturer = videoCapturer ?: return
        capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                preferFrontCamera = isFrontCamera
            }

            override fun onCameraSwitchError(errorDescription: String?) {
                // Keep previous state when switch fails.
            }
        })
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
        val selected = selectCamera(enumerator)
            ?: error("Aucune camera compatible WebRTC")

        val capturer = enumerator.createCapturer(selected, null)
            ?: error("Impossible de creer le capturer WebRTC")

        surfaceTextureHelper = SurfaceTextureHelper.create("WebRtcCaptureThread", eglBase.eglBaseContext)
        videoSource = factory.createVideoSource(false)
        capturer.initialize(surfaceTextureHelper, appContext, videoSource?.capturerObserver)
        capturer.startCapture(1280, 720, 30)

        videoTrack = factory.createVideoTrack("camera-video-track", videoSource)
        videoTrack?.setEnabled(true)
        videoCapturer = capturer
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

