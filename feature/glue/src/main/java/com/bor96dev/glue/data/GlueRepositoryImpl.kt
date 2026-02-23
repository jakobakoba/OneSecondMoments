package com.bor96dev.glue.data

import com.bor96dev.database.MomentEntity
import com.bor96dev.database.MomentsDao
import com.bor96dev.glue.domain.GlueRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GlueRepositoryImpl @Inject constructor(
    private val momentsDao: MomentsDao
): GlueRepository {
    override fun getMomentsByMonth(month: String): Flow<List<MomentEntity>> {
        return momentsDao.getMomentsForMonth(month)
    }

    override fun getMomentsByYear(year: Int): Flow<List<MomentEntity>> {
        return momentsDao.getMomentsForYear(year.toString())
    }
}