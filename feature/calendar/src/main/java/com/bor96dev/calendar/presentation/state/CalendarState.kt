package com.bor96dev.calendar.presentation.state

import com.bor96dev.database.MomentEntity
import java.time.LocalDate
import java.time.YearMonth

data class CalendarState (
    val selectedMonth: YearMonth = YearMonth.now(),
    val days: List<CalendarDay> = emptyList(),
    val moments: Map<String, MomentEntity> = emptyMap(),
    val selectedDay: LocalDate? = LocalDate.now(),
    val selectedMoment: MomentEntity? = null,
    val isLoading: Boolean = false
)

data class CalendarDay (
    val date: LocalDate?,
    val isToday: Boolean = false,
    val isFuture: Boolean = false,
    val hasVideo: Boolean = false
)