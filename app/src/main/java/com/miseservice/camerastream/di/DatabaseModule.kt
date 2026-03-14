package com.miseservice.camerastream.di

import android.content.Context
import com.miseservice.camerastream.data.local.AdminSettingsDao
import com.miseservice.camerastream.data.local.CameraStreamDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CameraStreamDatabase {
        return CameraStreamDatabase.getInstance(context)
    }

    @Provides
    fun provideAdminSettingsDao(database: CameraStreamDatabase): AdminSettingsDao {
        return database.adminSettingsDao()
    }
}

