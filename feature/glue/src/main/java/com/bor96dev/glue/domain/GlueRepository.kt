package com.bor96dev.glue.domain

import com.bor96dev.database.MomentEntity
import kotlinx.coroutines.flow.Flow

interface GlueRepository {
    fun getMomentsByMonth(month: String): Flow<List<MomentEntity>>
    fun getMomentsByYear(year: Int): Flow<List<MomentEntity>>
}