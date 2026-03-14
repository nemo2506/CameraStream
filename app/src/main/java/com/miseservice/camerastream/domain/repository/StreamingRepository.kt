package com.miseservice.camerastream.domain.repository

interface StreamingRepository {
    fun startStreaming()
    fun stopStreaming()
    fun switchCamera()
    fun copyToClipboard(label: String, value: String)
}

