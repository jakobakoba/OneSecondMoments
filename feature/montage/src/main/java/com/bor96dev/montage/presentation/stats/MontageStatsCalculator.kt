package com.bor96dev.montage.presentation.stats

import com.bor96dev.database.Moment
import com.bor96dev.montage.presentation.state.MonthStat
import com.bor96dev.montage.presentation.state.YearStat
import java.time.LocalDate
import java.time.YearMonth

object MontageStatsCalculator {
    fun computeYearlyStats(moments: List<Moment>): List<YearStat> {
        return moments
            .groupBy { LocalDate.parse(it.date).year }
            .map { (year, list) -> YearStat(year, list.size) }
            .sortedByDescending { it.year }
    }

    fun computeMonthlyStats(moments: List<Moment>): List<MonthStat> {
        return moments
            .groupBy {
                YearMonth.parse(it.date.substring(0, 7))
            }
            .map { (yearMonth, list) -> MonthStat(yearMonth, list.size) }
            .sortedByDescending { it.yearMonth }
    }
}
