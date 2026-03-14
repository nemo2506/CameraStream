package com.miseservice.camerastream.domain.repository

interface StreamingRepository {
    fun startStreaming(port: Int)
    fun stopStreaming()
    fun switchCamera()
    fun copyToClipboard(label: String, value: String)
}

