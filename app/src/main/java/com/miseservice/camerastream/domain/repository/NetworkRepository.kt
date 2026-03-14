package com.miseservice.camerastream.domain.repository

import com.miseservice.camerastream.domain.model.NetworkInfo

interface NetworkRepository {
    suspend fun fetchNetworkInfo(): NetworkInfo
}

