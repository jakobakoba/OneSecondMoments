package com.bor96dev.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MomentsDao {
    @Upsert
    suspend fun upsertMoment(moment: MomentEntity)

    @Query("SELECT * FROM moments WHERE date == :date")
    suspend fun getMomentByDate(date: String): MomentEntity?

    @Query("SELECT * FROM moments WHERE date LIKE :monthQuery || '-%' ORDER BY date ASC")
    fun getMomentsForMonth(monthQuery: String): Flow<List<MomentEntity>>

    @Delete
    suspend fun deleteMoment(moment: MomentEntity)
}