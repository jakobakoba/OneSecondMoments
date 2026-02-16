package com.bor96dev.database.di

import android.content.Context
import androidx.room.Room
import com.bor96dev.database.MomentsDao
import com.bor96dev.database.MomentsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MomentsDatabase {
        return Room.databaseBuilder(
            context,
            MomentsDatabase::class.java,
            "moments.db"
        ).build()
    }

    @Singleton
    @Provides
    fun provideDao(database: MomentsDatabase): MomentsDao {
        return database.momentsDao()
    }
}