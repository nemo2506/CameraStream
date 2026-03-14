package com.miseservice.camerastream.domain.usecase

import com.miseservice.camerastream.domain.model.AdminSettings
import com.miseservice.camerastream.domain.repository.AdminSettingsRepository
import javax.inject.Inject

class LoadAdminSettingsUseCase @Inject constructor(
    private val repository: AdminSettingsRepository
) {
    suspend operator fun invoke(): AdminSettings = repository.load()
}

