package com.bor96dev.montage.domain

import com.bor96dev.database.MomentEntity
import kotlinx.coroutines.flow.Flow

interface MontageRepository {
    fun getAllMoments(): Flow<List<MomentEntity>>
    fun getMomentsByMonth(monthQuery: String): Flow<List<MomentEntity>>
    fun getMomentsByYear(yearQuery: String): Flow<List<MomentEntity>>
}