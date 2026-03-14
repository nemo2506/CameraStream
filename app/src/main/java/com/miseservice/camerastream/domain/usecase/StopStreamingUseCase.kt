package com.miseservice.camerastream.domain.usecase

import com.miseservice.camerastream.domain.repository.StreamingRepository
import javax.inject.Inject

class StopStreamingUseCase @Inject constructor(
    private val repository: StreamingRepository
) {
    operator fun invoke() = repository.stopStreaming()
}

