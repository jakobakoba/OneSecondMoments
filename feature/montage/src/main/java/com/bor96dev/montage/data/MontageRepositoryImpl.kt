package com.bor96dev.montage.data

import com.bor96dev.database.Moment
import com.bor96dev.database.MomentsDao
import com.bor96dev.database.toDomain
import com.bor96dev.montage.domain.MontageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MontageRepositoryImpl @Inject constructor(
    private val momentsDao: MomentsDao
) : MontageRepository{
    override fun getAllMoments(): Flow<List<Moment>> {
        return momentsDao.getAllMoments()
            .map { moments -> moments.map { it.toDomain() } }
    }

    override fun getMomentsByMonth(monthQuery: String): Flow<List<Moment>> {
        return momentsDao.getMomentsForMonth(monthQuery)
            .map { moments -> moments.map { it.toDomain() } }
    }

    override fun getMomentsByYear(yearQuery: String): Flow<List<Moment>> {
        return momentsDao.getMomentsForYear(yearQuery)
            .map { moments -> moments.map { it.toDomain() } }
    }
}
