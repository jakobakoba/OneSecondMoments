package com.bor96dev.calendar.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bor96dev.calendar.presentation.composables.CalendarHeader
import com.bor96dev.calendar.presentation.composables.DayCell
import com.bor96dev.calendar.presentation.composables.DaysOfWeek
import com.bor96dev.calendar.presentation.composables.VideoPreviewSection
import com.bor96dev.calendar.presentation.event.CalendarEvent
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    onNavigateToRecord: () -> Unit,
    onNavigateToEdit: (Uri, Long) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            state.selectedDay?.let { date ->
                val timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                onNavigateToEdit(it, timestamp)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        CalendarHeader(
            month = state.selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
            year = state.selectedMonth.year,
            onPreviousMonth = { viewModel.onEvent(CalendarEvent.PreviousMonth) },
            onNextMonth = { viewModel.onEvent(CalendarEvent.NextMonth) }
        )

        VideoPreviewSection(
            moment = state.selectedMoment,
            selectedDate = state.selectedDay,
            onDelete = { viewModel.onEvent(CalendarEvent.DeleteSelectedMoment) },
            onReplace = {
                if (state.selectedDay == LocalDate.now()) {
                    onNavigateToRecord()
                } else {
                    galleryLauncher.launch("video/*")
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        DaysOfWeek()

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            userScrollEnabled = false
        ) {
            items(state.days) { day ->
                DayCell(
                    day = day,
                    isSelected = day.date == state.selectedDay,
                    onClick = {
                        day.date?.let { viewModel.onEvent(CalendarEvent.SelectDay(it)) }
                    }
                )
            }
        }
    }
}