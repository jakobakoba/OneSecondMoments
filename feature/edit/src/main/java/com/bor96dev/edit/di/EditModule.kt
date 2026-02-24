package com.bor96dev.edit.di

import com.bor96dev.edit.data.EditRepositoryImpl
import com.bor96dev.edit.domain.EditRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EditModule {
    @Binds
    @Singleton
    abstract fun bindEditRepository(
        editRepositoryImpl: EditRepositoryImpl
    ): EditRepository
}