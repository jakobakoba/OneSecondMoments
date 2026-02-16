package com.bor96dev.calendar.di

import com.bor96dev.calendar.data.CalendarRepositoryImpl
import com.bor96dev.calendar.domain.CalendarRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CalendarModule {
    @Binds
    @Singleton
    abstract fun bindCalendarRepository (
        calendarRepositoryImpl: CalendarRepositoryImpl
    ): CalendarRepository
}