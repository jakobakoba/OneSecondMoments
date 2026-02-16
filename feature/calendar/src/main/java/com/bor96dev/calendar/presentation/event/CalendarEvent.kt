package com.bor96dev.calendar.presentation.event

import java.time.LocalDate

sealed interface CalendarEvent {
    object NextMonth: CalendarEvent
    object PreviousMonth: CalendarEvent
    data class SelectDay(val date: LocalDate): CalendarEvent
    object DeleteSelectedMoment: CalendarEvent
}