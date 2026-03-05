package com.bor96dev.calendar.data

import com.bor96dev.calendar.domain.CalendarRepository
import com.bor96dev.database.Moment
import com.bor96dev.database.MomentsDao
import com.bor96dev.database.toDomain
import com.bor96dev.database.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CalendarRepositoryImpl @Inject constructor(
    private val momentsDao: MomentsDao
): CalendarRepository {
    override fun getMomentsForMonth(monthQuery: String): Flow<List<Moment>> {
        return momentsDao.getMomentsForMonth(monthQuery)
            .map { moments -> moments.map { it.toDomain() } }
    }

    override suspend fun getMomentByDate(date: String): Moment? {
        return momentsDao.getMomentByDate(date)?.toDomain()
    }

    override suspend fun deleteMoment(moment: Moment) {
        momentsDao.deleteMoment(moment.toEntity())
    }
}
