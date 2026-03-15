package com.miseservice.camerastream.data.repository

import android.content.Context
import com.miseservice.camerastream.data.converter.NetworkInfoConverter
import com.miseservice.camerastream.data.dto.NetworkInfoDto
import com.miseservice.camerastream.domain.model.NetworkInfo
import com.miseservice.camerastream.domain.repository.NetworkRepository
import com.miseservice.camerastream.utils.NetworkUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NetworkRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkRepository {
    override suspend fun fetchNetworkInfo(port: Int): NetworkInfo {
        val isWifiConnected = NetworkUtils.isWifiConnected(context)
        if (!isWifiConnected) {
            return NetworkInfoConverter.dtoToDomain(
                NetworkInfoDto(
                    isWifiConnected = false,
                    localIpAddress = null,
                    streamingUrl = null,
                    statusUrl = null,
                    batteryApiUrl = null,
                    wifiNetworkName = null,
                    errorMessage = "WiFi non connecté. Veuillez connecter le WiFi."
                )
            )
        }

        val ipAddress = NetworkUtils.getLocalIpAddress(context)
        val dto = NetworkInfoDto(
            isWifiConnected = ipAddress != null,
            localIpAddress = ipAddress,
            streamingUrl = NetworkUtils.getStreamingUrl(context, port),
            statusUrl = NetworkUtils.getStatusUrl(context, port),
            batteryApiUrl = NetworkUtils.getBatteryApiUrl(context, port),
            wifiNetworkName = NetworkUtils.getWifiNetworkName(context),
            errorMessage = if (ipAddress == null) {
                "Impossible de détecter l'adresse IP. Essayez de reconnecter le WiFi."
            } else {
                null
            }
        )
        return NetworkInfoConverter.dtoToDomain(dto)
    }
}

