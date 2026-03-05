package com.bor96dev.database

fun MomentEntity.toDomain(): Moment =
    Moment(
        date = date,
        videoUri = videoUri,
        locationText = locationText
    )

fun Moment.toEntity(): MomentEntity =
    MomentEntity(
        date = date,
        videoUri = videoUri,
        locationText = locationText
    )
