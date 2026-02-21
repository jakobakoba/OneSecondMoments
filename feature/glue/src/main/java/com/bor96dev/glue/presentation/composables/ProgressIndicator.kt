package com.bor96dev.glue.presentation.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun ProgressIndicator(progressProvider: () -> Float) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp)
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .offset {
                    IntOffset(
                        x = (maxWidthPx * progressProvider()).toInt(),
                        y = 0
                    )
                }
                .background(Color(0xFF22c55e))
        )
    }
}