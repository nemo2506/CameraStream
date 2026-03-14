package com.miseservice.camerastream.domain.usecase

import com.miseservice.camerastream.domain.repository.StreamingRepository
import javax.inject.Inject

class SwitchCameraUseCase @Inject constructor(
    private val repository: StreamingRepository
) {
    operator fun invoke() = repository.switchCamera()
}

