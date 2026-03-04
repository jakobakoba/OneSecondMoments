package com.bor96dev.montage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bor96dev.montage.domain.MontageRepository
import com.bor96dev.montage.presentation.event.MontageEvent
import com.bor96dev.montage.presentation.state.MontageState
import com.bor96dev.montage.presentation.stats.MontageStatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
                val yearly = MontageStatsCalculator.computeYearlyStats(moments)
                val monthly = MontageStatsCalculator.computeMonthlyStats(moments)

                _uiState.update {
                    it.copy(
                        yearlyStats = yearly,
                        monthlyStats = monthly
                    )
                }
            }
        }
    }

    fun onEvent(event: MontageEvent){
        when(event){
            is MontageEvent.TogglePeriod -> {
                _uiState.update{it.copy(isMonthly = !it.isMonthly)}
            }
            is MontageEvent.NavigateToGlueYear -> {
                _uiState.update {it.copy(navigateToGlueYear = event.year)}
            }
            is MontageEvent.NavigateToGlueMonth -> {
                _uiState.update {it.copy(navigateToGlue = event.yearMonth.toString())}
            }
            is MontageEvent.OnNavigationDone -> {
                _uiState.update {it.copy(
                    navigateToGlue = null,
                    navigateToGlueYear = null
                )}
            }
        }
    }
}
