package com.bor96dev.glue.di

import com.bor96dev.glue.data.GlueRepositoryImpl
import com.bor96dev.glue.domain.GlueRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GlueModule {
    @Binds
    @Singleton
    abstract fun bindGlueRepository(
        glueRepositoryImpl: GlueRepositoryImpl
    ): GlueRepository
}