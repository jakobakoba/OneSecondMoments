package com.bor96dev.database

data class Moment(
    val date: String,
    val videoUri: String,
    val locationText: String? = null,
)