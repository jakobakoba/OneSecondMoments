package com.bor96dev.calendar.domain

import com.bor96dev.database.Moment
import kotlinx.coroutines.flow.Flow

interface CalendarRepository {
    fun getMomentsForMonth(monthQuery: String): Flow<List<Moment>>
    suspend fun getMomentByDate(date: String) : Moment?
    suspend fun deleteMoment(moment: Moment)
}
