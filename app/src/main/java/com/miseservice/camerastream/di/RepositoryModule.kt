package com.miseservice.camerastream.di

import com.miseservice.camerastream.data.repository.AdminSettingsRepositoryImpl
import com.miseservice.camerastream.data.repository.BatteryRepositoryImpl
import com.miseservice.camerastream.data.repository.NetworkRepositoryImpl
import com.miseservice.camerastream.data.repository.StreamingRepositoryImpl
import com.miseservice.camerastream.domain.repository.AdminSettingsRepository
import com.miseservice.camerastream.domain.repository.BatteryRepository
import com.miseservice.camerastream.domain.repository.NetworkRepository
import com.miseservice.camerastream.domain.repository.StreamingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAdminSettingsRepository(
        impl: AdminSettingsRepositoryImpl
    ): AdminSettingsRepository

    @Binds
    @Singleton
    abstract fun bindNetworkRepository(
        impl: NetworkRepositoryImpl
    ): NetworkRepository

    @Binds
    @Singleton
    abstract fun bindStreamingRepository(
        impl: StreamingRepositoryImpl
    ): StreamingRepository

    @Binds
    @Singleton
    abstract fun bindBatteryRepository(
        impl: BatteryRepositoryImpl
    ): BatteryRepository
}

