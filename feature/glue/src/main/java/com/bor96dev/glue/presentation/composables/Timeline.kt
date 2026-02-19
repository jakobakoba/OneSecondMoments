package com.bor96dev.glue.presentation.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bor96dev.database.MomentEntity
import com.bor96dev.glue.presentation.state.AudioTrack

@Composable
fun Timeline(
    currentTimeProvider: () -> Long,
    totalDurationMs: Long,
    videoMoments: List<MomentEntity>,
    audioTracks: List<AudioTrack>,
    onSeek: (Long) -> Unit
) {
    val progress = remember (totalDurationMs){
        derivedStateOf {
            val time = currentTimeProvider()
            if (totalDurationMs > 0) time.toFloat() / totalDurationMs else 0f
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Timeline",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)){
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    videoMoments.forEach { _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 1.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF3b82f6), Color(0xFF2563eb))
                                    ),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )  {
                    if (audioTracks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Пусто - Добавьте музыку",
                                color = Color.DarkGray,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        audioTracks.forEach { track ->
                            Box (
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFa855f7), Color(0xFFec4899))
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = track.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val xOffset = maxWidth * progress.value
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(2.dp)
                                .offset(x = xOffset)
                                .background(Color(0xFF22c55e))
                        )
                    }
                }
                Slider(
                    value = progress.value,
                    onValueChange = {onSeek((it * totalDurationMs).toLong())},
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFF22c55e),
                        inactiveTrackColor = Color.DarkGray
                    )
                )
            }
        }
    }

}