package com.bor96dev.calendar.presentation.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bor96dev.calendar.presentation.state.CalendarDay

@Composable
fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val boxModifier = Modifier
        .aspectRatio(1f)
        .padding(4.dp)
        .clip(CircleShape)

    Box(
        modifier = if (day.date != null) {
            boxModifier
                .then(
                    if (day.hasVideo) {
                        Modifier.background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF6366f1), Color(0xFFa855f7))
                            )
                        )
                    } else {
                        Modifier.background(Color(0xFF1f2937))
                    }
                )
                .then(
                    if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                    else Modifier
                )
                .clickable(
                    enabled = !day.isFuture,
                    onClick = onClick
                )
        } else {
            boxModifier
        },
        contentAlignment = Alignment.Center
    ) {
        if (day.date != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    color = if (day.isFuture) Color.Gray else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (day.hasVideo) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}