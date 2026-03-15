package com.miseservice.camerastream.data.streaming

import android.content.Context
import com.miseservice.camerastream.domain.repository.BatteryRepository
import com.miseservice.camerastream.server.WebRtcHttpServer
import com.miseservice.camerastream.webrtc.WebRtcEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRtcStreamingRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
    private val batteryRepository: BatteryRepository
) : StreamingRuntime {

    private var webRtcEngine: WebRtcEngine? = null
    private var signalingServer: WebRtcHttpServer? = null
    private var currentPort: Int? = null

    override fun start(port: Int) {
        val existingEngine = webRtcEngine
        val existingServer = signalingServer

        if (existingEngine != null && existingServer != null) {
            if (currentPort == port) return
            // Hot-switch: keep camera/WebRTC engine alive, restart only signaling HTTP server.
            existingServer.stop()
            val newServer = WebRtcHttpServer(
                port = port,
                webRtcSessionGateway = existingEngine,
                batteryProvider = { batteryRepository.getCurrentBatteryInfo() }
            )
            newServer.start()
            signalingServer = newServer
            currentPort = port
            return
        }

        val engine = existingEngine ?: WebRtcEngine(context).also { it.start() }
        val server = WebRtcHttpServer(
            port = port,
            webRtcSessionGateway = engine,
            batteryProvider = { batteryRepository.getCurrentBatteryInfo() }
        ).also { it.start() }

        webRtcEngine = engine
        signalingServer = server
        currentPort = port
    }

    override fun stop() {
        signalingServer?.stop()
        signalingServer = null
        currentPort = null

        webRtcEngine?.stop()
        webRtcEngine = null
    }

    override fun switchCamera() {
        webRtcEngine?.switchCamera()
    }

    override fun onAppBackgroundChanged(isInBackground: Boolean) {
        webRtcEngine?.setForcePortraitMode(isInBackground)
    }
}

