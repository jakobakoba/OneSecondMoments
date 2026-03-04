package com.bor96dev.edit.presentation

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.bor96dev.edit.presentation.event.EditEvent
import com.bor96dev.edit.presentation.state.EditState
import com.bor96dev.ui.R
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun EditScreen(
    state: EditState,
    player: Player?,
    onEvent: (EditEvent) -> Unit
) {
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentSelectedStartMs by rememberUpdatedState(state.selectedStartMs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onEvent(EditEvent.OnBackClicked) },
                modifier = Modifier
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                )
            }
            Text(
                text = stringResource(R.string.edit_screen_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { onEvent(EditEvent.SaveClicked) },
                enabled = !state.isSaving,
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = stringResource(R.string.edit_screen_save_button),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (player != null) {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            useController = false
                            setKeepContentOnPlayerReset(true)
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            this.player = player
                        }
                    },
                    update = { playerView ->
                        playerView.player = player
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (state.dateText.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    if (!state.locationText.isNullOrBlank()) {
                        Text(
                            text = state.locationText,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = state.dateText,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total: ${"%.1f".format(state.videoDurationMs / 1000f)}s",
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.edit_screen_choose_the_moment),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            val rangeWidth = 1000f
            val maxStart = maxOf(0f, (state.videoDurationMs - rangeWidth))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val widthPx = constraints.maxWidth.toFloat()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                )

                if (state.videoDurationMs > 0) {
                    val thumbWidth = (rangeWidth / state.videoDurationMs.toFloat()) * widthPx
                    val thumbOffset =
                        (currentSelectedStartMs.toFloat() / state.videoDurationMs.toFloat()) * widthPx

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(thumbOffset.roundToInt(), 0) }
                            .width(with(LocalDensity.current) { thumbWidth.toDp() })
                            .fillMaxHeight()
                            .background(Color.Yellow, RoundedCornerShape(4.dp))
                            .pointerInput(state.videoDurationMs) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaMs = (dragAmount.x / widthPx) * state.videoDurationMs
                                    val newStart =
                                        (currentSelectedStartMs + deltaMs).coerceIn(0f, maxStart)
                                    currentOnEvent(EditEvent.OnSeekChanged(newStart.toLong()))
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onEvent(EditEvent.TogglePlay) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.edit_screen_play_selected_1_second),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
