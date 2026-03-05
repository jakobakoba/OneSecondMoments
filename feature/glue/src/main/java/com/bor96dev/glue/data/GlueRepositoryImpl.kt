package com.bor96dev.glue.data

import com.bor96dev.database.Moment
import com.bor96dev.database.MomentsDao
import com.bor96dev.database.toDomain
import com.bor96dev.glue.domain.GlueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GlueRepositoryImpl @Inject constructor(
    private val momentsDao: MomentsDao
): GlueRepository {
    override fun getMomentsByMonth(month: String): Flow<List<Moment>> {
        return momentsDao.getMomentsForMonth(month)
            .map { moments -> moments.map { it.toDomain() } }
    }

    override fun getMomentsByYear(year: Int): Flow<List<Moment>> {
        return momentsDao.getMomentsForYear(year.toString())
            .map { moments -> moments.map { it.toDomain() } }
    }
}
