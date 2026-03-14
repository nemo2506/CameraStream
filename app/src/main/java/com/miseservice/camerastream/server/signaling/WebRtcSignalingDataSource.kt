package com.miseservice.camerastream.server.signaling

import com.miseservice.camerastream.webrtc.WebRtcSessionGateway

class WebRtcSignalingDataSource(
    private val gateway: WebRtcSessionGateway
) : SignalingDataSource {
    override suspend fun createAnswer(offerSdp: String): String = gateway.createAnswer(offerSdp)
    override fun activeSessionCount(): Int = gateway.activeSessionCount()
}

