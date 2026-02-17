package com.bor96dev.montage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bor96dev.montage.presentation.composables.StatCard
import com.bor96dev.montage.presentation.event.MontageEvent

@Composable
fun MontageScreen(
    viewModel: MontageViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Montage",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (state.isMonthly) Color.White else Color.Transparent)
                    .clickable { if (!state.isMonthly) viewModel.onEvent(MontageEvent.TogglePeriod) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Monthly",
                    color = if (state.isMonthly) Color.Black else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (!state.isMonthly) Color.White else Color.Transparent)
                    .clickable { if (state.isMonthly) viewModel.onEvent(MontageEvent.TogglePeriod) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Yearly",
                    color = if (!state.isMonthly) Color.Black else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (state.isMonthly) "Select Month" else "Select Year",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isMonthly){
                items(state.monthlyStats){stat ->
                    StatCard(
                        title = "${stat.yearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${stat.yearMonth.year}",
                        subtitle = "${stat.daysRecorded} days recorded",
                        onClick = {viewModel.onEvent(MontageEvent.NavigateToGlueMonth(stat.yearMonth))}
                    )
                }
            } else {
                items(state.yearlyStats){stat ->
                    StatCard(
                        title = stat.year.toString(),
                        subtitle = "${stat.daysRecorded} days recorded",
                        onClick = {viewModel.onEvent(MontageEvent.NavigateToGlueYear(stat.year))}
                    )
                }
            }
        }
    }
}