package com.bor96dev.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "moments")
data class MomentEntity (
    @PrimaryKey val date: String,
    val videoUri: String,
    val locationText: String? = null,
)