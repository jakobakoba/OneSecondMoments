package com.bor96dev.montage.di

import com.bor96dev.montage.data.MontageRepositoryImpl
import com.bor96dev.montage.domain.MontageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MontageModule {
    @Binds
    @Singleton
    abstract fun bindMontageRepository(
        montageRepositoryImpl: MontageRepositoryImpl
    ): MontageRepository
}