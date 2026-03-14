package com.miseservice.camerastream.domain.model

data class NetworkInfo(
    val isWifiConnected: Boolean,
    val localIpAddress: String?,
    val streamingUrl: String?,
    val statusUrl: String?,
    val wifiNetworkName: String?,
    val errorMessage: String?
)

