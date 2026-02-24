package com.bor96dev.edit.data

import com.bor96dev.database.MomentEntity
import com.bor96dev.database.MomentsDao
import com.bor96dev.edit.domain.EditRepository
import javax.inject.Inject

class EditRepositoryImpl @Inject constructor(
    private val momentsDao: MomentsDao
) : EditRepository {
    override suspend fun upsertMoment(moment: MomentEntity) = momentsDao.upsertMoment(moment)
}