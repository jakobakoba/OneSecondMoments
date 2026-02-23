package com.bor96dev.glue.presentation.composables

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AudioPlaybackSlider(
    progressProvider: () -> Float,
    totalDurationMs: Long,
    onSeek: (Long) -> Unit
) {
    Slider(
        value = progressProvider(),
        onValueChange = { onSeek((it * totalDurationMs).toLong()) },
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color(0xFF22c55e),
            inactiveTrackColor = Color.DarkGray
        )
    )
}