package com.miseservice.camerastream.data.streaming

import android.content.Context
import com.miseservice.camerastream.server.WebRtcHttpServer
import com.miseservice.camerastream.webrtc.WebRtcEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRtcStreamingRuntime @Inject constructor(
    @ApplicationContext private val context: Context
) : StreamingRuntime {

    private var webRtcEngine: WebRtcEngine? = null
    private var signalingServer: WebRtcHttpServer? = null

    override fun start() {
        if (signalingServer != null) return
        val engine = WebRtcEngine(context)
        engine.start()
        val server = WebRtcHttpServer(port = 8080, webRtcSessionGateway = engine)
        server.start()

        webRtcEngine = engine
        signalingServer = server
    }

    override fun stop() {
        signalingServer?.stop()
        signalingServer = null

        webRtcEngine?.stop()
        webRtcEngine = null
    }

    override fun switchCamera() {
        webRtcEngine?.switchCamera()
    }
}

