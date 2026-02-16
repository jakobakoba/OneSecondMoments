package com.bor96dev.calendar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bor96dev.calendar.domain.CalendarRepository
import com.bor96dev.calendar.presentation.event.CalendarEvent
import com.bor96dev.calendar.presentation.state.CalendarDay
import com.bor96dev.calendar.presentation.state.CalendarState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository
): ViewModel() {
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDay = MutableStateFlow<LocalDate?>(LocalDate.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _moments = _selectedMonth.flatMapLatest { month ->
        repository.getMomentsForMonth(month.toString())
    }

    val uiState: StateFlow<CalendarState> = combine (
        _selectedMonth,
        _selectedDay,
        _moments
    ) {month, selectedDay, momentsList ->
        val momentsMap = momentsList.associateBy {it.date}
        CalendarState(
            selectedMonth = month,
            selectedDay = selectedDay,
            moments = momentsMap,
            days = generateDays(month, momentsMap),
            selectedMoment = selectedDay?.let {momentsMap[it.toString()]}
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarState()
    )

    fun onEvent(event: CalendarEvent){
        when(event){
            CalendarEvent.NextMonth -> {
                _selectedMonth.update {it.plusMonths(1)}
            }

            CalendarEvent.PreviousMonth -> {
                _selectedMonth.update {it.minusMonths(1)}
            }
            is CalendarEvent.SelectDay -> {
                _selectedDay.value = event.date
            }
            CalendarEvent.DeleteSelectedMoment -> {
                viewModelScope.launch {
                    uiState.value.selectedMoment?.let{
                        repository.deleteMoment(it)
                    }
                }
            }
        }
    }



    private fun generateDays(month: YearMonth, momentsMap: Map<String, Any>): List<CalendarDay>{
        val daysInMonth = month.lengthOfMonth()
        val firstDayOfMonth = month.atDay(1).dayOfWeek.value % 7
        val totalCells = ((daysInMonth + firstDayOfMonth + 6) / 7 ) * 7

        val today = LocalDate.now()

        return List(totalCells){index ->
            val dayNumber = index - firstDayOfMonth + 1
            if (dayNumber in 1 .. daysInMonth){
                val date = month.atDay(dayNumber)
                CalendarDay(
                    date = date,
                    isToday = date == today,
                    isFuture = date.isAfter(today),
                    hasVideo = momentsMap.containsKey(date.toString())
                )
            } else {
                CalendarDay(date = null)
            }
        }
    }
}