package com.miseservice.camerastream.server.signaling

interface SignalingDataSource {
    suspend fun createAnswer(offerSdp: String): String
    fun activeSessionCount(): Int
}

