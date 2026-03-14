package com.miseservice.camerastream.webrtc

interface WebRtcSessionGateway {
    suspend fun createAnswer(offerSdp: String): String
    fun activeSessionCount(): Int
}

