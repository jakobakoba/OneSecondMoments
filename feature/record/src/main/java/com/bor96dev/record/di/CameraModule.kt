package com.bor96dev.record.di

import com.bor96dev.record.data.CameraManagerImpl
import com.bor96dev.record.domain.CameraManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CameraModule {
    @Binds
    @Singleton
    abstract fun bindCameraManager(cameraManagerImpl: CameraManagerImpl) : CameraManager
}