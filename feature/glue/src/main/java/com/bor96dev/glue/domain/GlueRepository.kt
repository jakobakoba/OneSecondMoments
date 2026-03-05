package com.bor96dev.glue.domain

import com.bor96dev.database.Moment
import kotlinx.coroutines.flow.Flow

interface GlueRepository {
    fun getMomentsByMonth(month: String): Flow<List<Moment>>
    fun getMomentsByYear(year: Int): Flow<List<Moment>>
}
