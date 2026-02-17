package com.bor96dev.montage.data

import com.bor96dev.database.MomentEntity
import com.bor96dev.database.MomentsDao
import com.bor96dev.montage.domain.MontageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MontageRepositoryImpl @Inject constructor(
    private val momentsDao: MomentsDao
) : MontageRepository{
    override fun getAllMoments(): Flow<List<MomentEntity>> {
        return momentsDao.getAllMoments()
    }

    override fun getMomentsByMonth(monthQuery: String): Flow<List<MomentEntity>> {
        return momentsDao.getMomentsForMonth(monthQuery)
    }

    override fun getMomentsByYear(yearQuery: String): Flow<List<MomentEntity>> {
        return momentsDao.getMomentsForYear(yearQuery)
    }
}