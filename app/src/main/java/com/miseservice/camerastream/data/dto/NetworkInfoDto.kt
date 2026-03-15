package com.miseservice.camerastream.data.dto

data class NetworkInfoDto(
    val isWifiConnected: Boolean,
    val localIpAddress: String?,
    val streamingUrl: String?,
    val statusUrl: String?,
    val batteryApiUrl: String?,
    val wifiNetworkName: String?,
    val errorMessage: String?
)

