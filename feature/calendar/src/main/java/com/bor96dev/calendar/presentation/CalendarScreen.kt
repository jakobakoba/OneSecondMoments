package com.bor96dev.calendar.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bor96dev.calendar.presentation.composables.CalendarHeader
import com.bor96dev.calendar.presentation.event.CalendarEvent
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
    val context = LocalContext.current

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
            month = state.selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            year = state.selectedMonth.year,
            onPreviousMonth = { viewModel.onEvent(CalendarEvent.PreviousMonth) },
            onNextMonth = { viewModel.onEvent(CalendarEvent.NextMonth) }
        )
    }
}