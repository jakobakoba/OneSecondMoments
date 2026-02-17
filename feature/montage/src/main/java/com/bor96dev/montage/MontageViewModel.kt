package com.bor96dev.montage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bor96dev.montage.domain.MontageRepository
import com.bor96dev.montage.presentation.state.MontageState
import com.bor96dev.montage.presentation.state.MonthStat
import com.bor96dev.montage.presentation.state.YearStat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class MontageViewModel @Inject constructor(
    private val repository: MontageRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(MontageState())
    val uiState = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats(){
        viewModelScope.launch {
            repository.getAllMoments().collect { moments ->
                val yearly = moments.groupBy {
                    LocalDate.parse(it.date).year
                }.map {(year, list) ->
                    YearStat(year, list.size)
                }.sortedByDescending { it.year }

                val monthly = moments.groupBy {
                    YearMonth.parse(it.date.substring(0, 7))
                }.map{(month, list) ->
                    MonthStat(month, list.size)
                }.sortedByDescending { it.yearMonth }

                _uiState.update {
                    it.copy(
                        yearlyStats = yearly,
                        monthlyStats = monthly
                    )
                }
            }
        }
    }
}