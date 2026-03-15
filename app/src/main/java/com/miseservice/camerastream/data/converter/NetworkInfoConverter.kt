package com.miseservice.camerastream.data.converter

import com.miseservice.camerastream.data.dto.NetworkInfoDto
import com.miseservice.camerastream.domain.model.NetworkInfo

object NetworkInfoConverter {
    fun dtoToDomain(dto: NetworkInfoDto): NetworkInfo {
        return NetworkInfo(
            isWifiConnected = dto.isWifiConnected,
            localIpAddress = dto.localIpAddress,
            streamingUrl = dto.streamingUrl,
            statusUrl = dto.statusUrl,
            batteryApiUrl = dto.batteryApiUrl,
            wifiNetworkName = dto.wifiNetworkName,
            errorMessage = dto.errorMessage
        )
    }
}

