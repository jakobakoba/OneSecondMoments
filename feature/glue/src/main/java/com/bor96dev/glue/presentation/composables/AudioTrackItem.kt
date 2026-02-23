package com.bor96dev.glue.presentation.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bor96dev.glue.presentation.state.AudioTrack

private enum class DragPart { START, END, CENTER }

@Composable
fun AudioTrackItem(
    track: AudioTrack,
    totalDurationMs: Long,
    occupiedRanges: List<Pair<Long, Long>> = emptyList(),
    onDragging: (Boolean) -> Unit = {},
    onUpdate: (Long, Long, Long) -> Unit
) {
    var localStartMs by remember(track.id) { mutableLongStateOf(track.startInTimelineMs) }
    var localEndMs by remember(track.id) { mutableLongStateOf(track.endInTimelineMs) }
    var localTrimMs by remember(track.id) { mutableLongStateOf(track.trimStartMs) }
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val sortedOccupied = remember(occupiedRanges) { occupiedRanges.sortedBy { it.first } }

    LaunchedEffect(track.startInTimelineMs, track.endInTimelineMs, track.trimStartMs) {
        if (!isDragging) {
            localStartMs = track.startInTimelineMs
            localEndMs = track.endInTimelineMs
            localTrimMs = track.trimStartMs
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val msToPx = if (totalDurationMs > 0) widthPx / totalDurationMs else 0f

        val startPx = localStartMs * msToPx
        val endPx = localEndMs * msToPx
        val trackWidthPx = (endPx - startPx).coerceAtLeast(1f)

        val trackWidthDp = with(density) { trackWidthPx.toDp() }
        val maxHandlePx = with(density){32.dp.toPx()}

        var dragPart by remember { mutableStateOf(DragPart.CENTER) }

        Box(
            modifier = Modifier
                .offset { IntOffset(x = startPx.toInt(), y = 0) }
                .width(trackWidthDp)
                .fillMaxHeight()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFa855f7), Color(0xFFec4899))
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                .pointerInput(track.id, totalDurationMs, sortedOccupied) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            onDragging(true)
                            val currentWidthPx = (localEndMs - localStartMs) * msToPx
                            val currentHandlePx = minOf(maxHandlePx, currentWidthPx / 3f)
                            val touchRatio = offset.x / currentWidthPx.coerceAtLeast(1f)

                            dragPart = when {
                                currentWidthPx < maxHandlePx * 2 -> {
                                    if (touchRatio < 0.5f) DragPart.START else DragPart.END
                                }
                                offset.x < currentHandlePx -> DragPart.START
                                offset.x > trackWidthPx - currentHandlePx -> DragPart.END
                                else -> DragPart.CENTER
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            onDragging(false)
                            onUpdate(localStartMs, localEndMs, localTrimMs)
                        },
                        onDragCancel = {
                            isDragging = false
                            onDragging(false)
                        },

                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            if (msToPx == 0f) return@detectHorizontalDragGestures
                            val deltaMs = (dragAmount / msToPx).toLong()

                            val prevEnd = sortedOccupied
                                .filter { it.second <= localStartMs }
                                .maxOfOrNull { it.second } ?: 0L
                            val nextStart = sortedOccupied
                                .filter { it.first >= localEndMs }
                                .minOfOrNull { it.first } ?: totalDurationMs

                            when (dragPart) {
                                DragPart.START -> {
                                    val newStart =
                                        (localStartMs + deltaMs).coerceIn(prevEnd, localEndMs - 200)
                                    val newTrim =
                                        (localTrimMs + (newStart - localStartMs)).coerceAtLeast(0)
                                    localStartMs = newStart
                                    localTrimMs = newTrim
                                }

                                DragPart.END -> {
                                    val newEnd = (localEndMs + deltaMs).coerceIn(
                                        localStartMs + 200,
                                        nextStart
                                    )
                                    if (newEnd - localStartMs + localTrimMs <= track.fileDurationMs) {
                                        localEndMs = newEnd
                                    }
                                }

                                DragPart.CENTER -> {
                                    val duration = localEndMs - localStartMs
                                    val newStart = (localStartMs + deltaMs).coerceIn(
                                        prevEnd,
                                        nextStart - duration
                                    )
                                    localStartMs = newStart
                                    localEndMs = newStart + duration
                                }
                            }
                            onUpdate(localStartMs, localEndMs, localTrimMs)
                        }
                    )
                }
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(6.dp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .background(Color.White.copy(0.4f), RoundedCornerShape(3.dp))
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(6.dp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .background(Color.White.copy(0.4f), RoundedCornerShape(3.dp))
            )
            Text(
                text = track.name,
                modifier = Modifier.align(Alignment.Center),
                fontSize = 10.sp,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}