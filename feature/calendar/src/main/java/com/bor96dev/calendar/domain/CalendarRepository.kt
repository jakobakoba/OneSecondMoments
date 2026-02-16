package com.bor96dev.calendar.domain

import com.bor96dev.database.MomentEntity
import kotlinx.coroutines.flow.Flow

interface CalendarRepository {
    fun getMomentsForMonth(monthQuery: String): Flow<List<MomentEntity>>
    suspend fun getMomentByDate(date: String) : MomentEntity?
    suspend fun deleteMoment(moment: MomentEntity)
}