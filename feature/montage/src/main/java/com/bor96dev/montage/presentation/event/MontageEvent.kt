package com.bor96dev.montage.presentation.event

import java.time.YearMonth

sealed interface MontageEvent {
    object TogglePeriod: MontageEvent
    data class NavigateToGlueYear(val year: Int): MontageEvent
    data class NavigateToGlueMonth(val yearMonth: YearMonth): MontageEvent
    object OnNavigationDone: MontageEvent
}