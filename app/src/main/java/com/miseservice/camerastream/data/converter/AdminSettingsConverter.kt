package com.miseservice.camerastream.data.converter

import com.miseservice.camerastream.data.dto.AdminSettingsDto
import com.miseservice.camerastream.data.local.AdminSettingsEntity
import com.miseservice.camerastream.domain.model.AdminSettings

object AdminSettingsConverter {
    fun entityToDto(entity: AdminSettingsEntity): AdminSettingsDto {
        return AdminSettingsDto(
            isStreaming = entity.isStreaming,
            isFrontCamera = entity.isFrontCamera,
            localIpAddress = entity.localIpAddress,
            isWakeLockActive = entity.isWakeLockActive,
            streamingPort = entity.streamingPort
        )
    }

    fun dtoToEntity(dto: AdminSettingsDto): AdminSettingsEntity {
        return AdminSettingsEntity(
            isStreaming = dto.isStreaming,
            isFrontCamera = dto.isFrontCamera,
            localIpAddress = dto.localIpAddress,
            isWakeLockActive = dto.isWakeLockActive,
            streamingPort = dto.streamingPort
        )
    }

    fun dtoToDomain(dto: AdminSettingsDto): AdminSettings {
        return AdminSettings(
            isStreaming = dto.isStreaming,
            isFrontCamera = dto.isFrontCamera,
            localIpAddress = dto.localIpAddress,
            isWakeLockActive = dto.isWakeLockActive,
            streamingPort = dto.streamingPort
        )
    }

    fun domainToDto(domain: AdminSettings): AdminSettingsDto {
        return AdminSettingsDto(
            isStreaming = domain.isStreaming,
            isFrontCamera = domain.isFrontCamera,
            localIpAddress = domain.localIpAddress,
            isWakeLockActive = domain.isWakeLockActive,
            streamingPort = domain.streamingPort
        )
    }
}

