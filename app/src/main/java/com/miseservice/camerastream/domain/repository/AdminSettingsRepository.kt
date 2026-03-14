package com.miseservice.camerastream.domain.repository

import com.miseservice.camerastream.domain.model.AdminSettings

interface AdminSettingsRepository {
    suspend fun load(): AdminSettings
    suspend fun save(settings: AdminSettings)
}

