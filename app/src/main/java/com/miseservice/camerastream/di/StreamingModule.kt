package com.miseservice.camerastream.di

import com.miseservice.camerastream.data.streaming.StreamingRuntime
import com.miseservice.camerastream.data.streaming.WebRtcStreamingRuntime
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StreamingModule {
    @Binds
    @Singleton
    abstract fun bindStreamingRuntime(
        impl: WebRtcStreamingRuntime
    ): StreamingRuntime
}
