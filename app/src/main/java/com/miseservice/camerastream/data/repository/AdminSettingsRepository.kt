package com.miseservice.camerastream.data.repository

import com.miseservice.camerastream.data.local.AdminSettingsDao
import com.miseservice.camerastream.data.local.AdminSettingsEntity

class AdminSettingsRepository(private val dao: AdminSettingsDao) {
    suspend fun load(): AdminSettingsEntity? = dao.getById()

    suspend fun save(settings: AdminSettingsEntity) {
        dao.upsert(settings)
    }
}

