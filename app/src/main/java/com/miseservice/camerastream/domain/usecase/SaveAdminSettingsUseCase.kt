package com.miseservice.camerastream.domain.usecase

import com.miseservice.camerastream.domain.model.AdminSettings
import com.miseservice.camerastream.domain.repository.AdminSettingsRepository
import javax.inject.Inject

class SaveAdminSettingsUseCase @Inject constructor(
    private val repository: AdminSettingsRepository
) {
    suspend operator fun invoke(settings: AdminSettings) = repository.save(settings)
}

