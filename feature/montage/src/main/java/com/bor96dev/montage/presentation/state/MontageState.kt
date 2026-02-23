package com.bor96dev.montage.presentation.state

import java.time.YearMonth

data class MontageState (
    val isMonthly: Boolean = false,
    val yearlyStats: List<YearStat> = emptyList(),
    val monthlyStats: List<MonthStat> = emptyList(),
    val navigateToGlue: String? = null,
    val navigateToGlueYear: Int? = null
)

data class YearStat(
    val year: Int,
    val daysRecorded: Int
)

data class MonthStat(
    val yearMonth: YearMonth,
    val daysRecorded: Int
)