package com.bor96dev.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MomentsDao {
    @Upsert
    suspend fun upsertMoment(moment: MomentEntity)

    @Query("SELECT * FROM moments WHERE date == :date")
    suspend fun getMomentByDate(date: String): MomentEntity?
}