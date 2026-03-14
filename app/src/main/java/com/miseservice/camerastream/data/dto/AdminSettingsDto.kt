package com.miseservice.camerastream.data.dto

data class AdminSettingsDto(
    val isStreaming: Boolean,
    val isFrontCamera: Boolean,
    val localIpAddress: String?,
    val isWakeLockActive: Boolean
)

