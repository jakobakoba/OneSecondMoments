package com.bor96dev.montage

import com.bor96dev.database.MomentEntity
import com.bor96dev.montage.presentation.state.MonthStat
import com.bor96dev.montage.presentation.state.YearStat
import com.bor96dev.montage.presentation.stats.MontageStatsCalculator
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.time.YearMonth

class MontageStatsCalculatorTest : StringSpec({
    "показ по годам от новых к старым" {
        val moments = listOf(
            MomentEntity("2022-04-01", "uri", "geo"),
            MomentEntity("2023-05-20", "uri", "geo"),
            MomentEntity("2022-12-31", "uri", "geo")
        )

        val stats = MontageStatsCalculator.computeYearlyStats(moments)

        stats shouldBe listOf(
            YearStat(2023, 1),
            YearStat(2022, 2)
        )
    }

    "показ по месяцам и году от новых к старым" {
        val stats = MontageStatsCalculator.computeMonthlyStats(
            listOf(
                MomentEntity("2023-05-01", "uri", "geo"),
                MomentEntity("2023-04-10", "uri", "geo"),
                MomentEntity("2022-06-12", "uri", "geo"),
                MomentEntity("2022-06-22", "uri", "geo")
            )
        )

        stats shouldBe listOf(
            MonthStat(YearMonth.of(2023, 5), 1),
            MonthStat(YearMonth.of(2023, 4), 1),
            MonthStat(YearMonth.of(2022, 6), 2)
        )
    }

    "пустые должны быть пустые" {
        MontageStatsCalculator.computeYearlyStats(emptyList()).shouldBeEmpty()
        MontageStatsCalculator.computeMonthlyStats(emptyList()).shouldBeEmpty()
    }
})