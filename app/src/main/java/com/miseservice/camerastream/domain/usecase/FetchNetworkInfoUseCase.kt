package com.miseservice.camerastream.domain.usecase

import com.miseservice.camerastream.domain.model.NetworkInfo
import com.miseservice.camerastream.domain.repository.NetworkRepository
import javax.inject.Inject

class FetchNetworkInfoUseCase @Inject constructor(
    private val repository: NetworkRepository
) {
    suspend operator fun invoke(): NetworkInfo = repository.fetchNetworkInfo()
}

