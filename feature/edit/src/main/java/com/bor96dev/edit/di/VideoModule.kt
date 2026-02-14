package com.bor96dev.edit.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object VideoModule {
    @Provides
    fun provideExoPlayerBuilder(@ApplicationContext context: Context): ExoPlayer.Builder {
        return ExoPlayer.Builder(context)
    }
}