package com.miseservice.camerastream.domain.model

data class BatteryInfo(
    val levelPercent: Int,
    val isCharging: Boolean,
    val status: String,
    val temperatureC: Float?,
    val timestampMs: Long
)

