package com.bor96dev.calendar.data

import com.bor96dev.calendar.domain.CalendarRepository
import com.bor96dev.database.MomentEntity
import com.bor96dev.database.MomentsDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CalendarRepositoryImpl @Inject constructor(
    private val momentsDao: MomentsDao
): CalendarRepository {
    override fun getMomentsForMonth(monthQuery: String): Flow<List<MomentEntity>> {
        TODO("Not yet implemented")
    }

    override suspend fun getMomentByDate(date: String): MomentEntity? {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMoment(moment: MomentEntity) {
        TODO("Not yet implemented")
    }

}