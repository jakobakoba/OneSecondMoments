package com.bor96dev.edit.domain

import com.bor96dev.database.Moment

interface EditRepository {
    suspend fun upsertMoment(moment: Moment)
}
