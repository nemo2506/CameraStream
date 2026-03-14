package com.miseservice.camerastream.data.repository

import com.miseservice.camerastream.data.converter.AdminSettingsConverter
import com.miseservice.camerastream.data.local.AdminSettingsDao
import com.miseservice.camerastream.domain.model.AdminSettings
import com.miseservice.camerastream.domain.repository.AdminSettingsRepository
import javax.inject.Inject

class AdminSettingsRepositoryImpl @Inject constructor(
    private val dao: AdminSettingsDao
) : AdminSettingsRepository {
    override suspend fun load(): AdminSettings {
        val entity = dao.getById()
        if (entity == null) return AdminSettings()
        val dto = AdminSettingsConverter.entityToDto(entity)
        return AdminSettingsConverter.dtoToDomain(dto)
    }

    override suspend fun save(settings: AdminSettings) {
        val dto = AdminSettingsConverter.domainToDto(settings)
        dao.upsert(AdminSettingsConverter.dtoToEntity(dto))
    }
}

