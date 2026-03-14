package com.miseservice.camerastream.domain.usecase

import com.miseservice.camerastream.domain.repository.StreamingRepository
import javax.inject.Inject

class CopyToClipboardUseCase @Inject constructor(
    private val repository: StreamingRepository
) {
    operator fun invoke(label: String, value: String) = repository.copyToClipboard(label, value)
}

