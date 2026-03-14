package com.miseservice.camerastream.domain.model

data class AdminSettings(
    val isStreaming: Boolean = false,
    val isFrontCamera: Boolean = true,
    val localIpAddress: String? = null,
    val isWakeLockActive: Boolean = false
)

