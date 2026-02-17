package com.bor96dev.montage.presentation.event

import java.time.YearMonth

sealed interface MontageEvent {
    object TogglePeriod: MontageEvent
    data class OnMusicToggled(val enabled: Boolean) : MontageEvent
    data class OnMusicVolumeChanged(val volume: Float): MontageEvent
    data class ExportYear(val year: Int): MontageEvent
    data class ExportMonth(val yearMonth: YearMonth): MontageEvent
}