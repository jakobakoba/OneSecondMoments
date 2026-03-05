package com.bor96dev.montage.domain

import com.bor96dev.database.Moment
import kotlinx.coroutines.flow.Flow

interface MontageRepository {
    fun getAllMoments(): Flow<List<Moment>>
    fun getMomentsByMonth(monthQuery: String): Flow<List<Moment>>
    fun getMomentsByYear(yearQuery: String): Flow<List<Moment>>
}
