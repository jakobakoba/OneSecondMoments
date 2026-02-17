package com.bor96dev.montage.presentation.state

import java.time.YearMonth

data class MontageState (
    val isMonthly: Boolean = false,
    val yearlyStats: List<YearStat> = emptyList(),
    val monthlyStats: List<MonthStat> = emptyList(),
    val isMusicEnabled: Boolean = false,
    val musicVolume: Float = 1.0f,
    val videoVolume: Float = 0.7f,
    val isExporting: Boolean = false
)

data class YearStat(
    val year: Int,
    val daysRecorded: Int
)

data class MonthStat(
    val yearMonth: YearMonth,
    val daysRecorded: Int
)