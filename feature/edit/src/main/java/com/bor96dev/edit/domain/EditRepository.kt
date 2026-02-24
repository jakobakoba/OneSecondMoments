package com.bor96dev.edit.domain

import com.bor96dev.database.MomentEntity

interface EditRepository {
    suspend fun upsertMoment(moment: MomentEntity)
}