package com.bor96dev.calendar.presentation.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bor96dev.database.MomentEntity
import java.time.LocalDate

@Composable
fun VideoPreviewSection(
    moment: MomentEntity?,
    selectedDate: LocalDate?,
    onDelete: () -> Unit,
    onReplace: () -> Unit
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (player == null) {
                        player = ExoPlayer.Builder(context).build().apply {
                            repeatMode = Player.REPEAT_MODE_ONE
                        }
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    player?.release()
                    player = null
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player?.release()
            player = null
        }
    }

    LaunchedEffect(moment, player) {
        val currentPlayer = player ?: return@LaunchedEffect

        if (moment != null) {
            val mediaItem = MediaItem.fromUri(moment.videoUri)
            if (currentPlayer.currentMediaItem?.localConfiguration?.uri != mediaItem.localConfiguration?.uri) {
                currentPlayer.setMediaItem(mediaItem)
                currentPlayer.prepare()
            }
            currentPlayer.play()
        } else {
            currentPlayer.stop()
            currentPlayer.clearMediaItems()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Box (
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (moment != null){
                if (player != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                // Важно: устанавливаем плеер
                                this.player = player
                            }
                        },
                        update = { playerView ->
                            // Обновляем плеер при рекомпозиции
                            playerView.player = player
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                IconButton(
                    onClick = onReplace,
                    modifier = Modifier
                        .size(64.dp)
                ) {
                    Icon (
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        if (selectedDate != null){
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDate.month.name + " " + selectedDate.dayOfMonth + ", " + selectedDate.year,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                IconButton(
                    onClick = onReplace,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null
                    )
                }

                if (moment != null){
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null
                        )
                    }
                }

            }

        }

    }


}