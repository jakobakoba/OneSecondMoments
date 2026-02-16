package com.bor96dev.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MomentEntity::class], version = 1)
abstract class MomentsDatabase: RoomDatabase() {
    abstract fun momentsDao(): MomentsDao
}