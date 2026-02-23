package com.bor96dev.glue.presentation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.bor96dev.database.MomentEntity
import com.bor96dev.glue.presentation.composables.Timeline
import com.bor96dev.glue.presentation.composables.VolumeControls
import com.bor96dev.glue.presentation.event.GlueEvent
import com.bor96dev.glue.presentation.state.AudioTrack

private fun getFileName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            return cursor.getString(nameIndex)
        }
    }
    return "Unknown track"
}

@Composable
fun GlueScreen(
    title: String,
    videoVolume: Float,
    musicVolume: Float,
    totalDurationMs: Long,
    currentTimeProvider: () -> Long,
    videoMoments: List<MomentEntity>,
    audioTracks: List<AudioTrack>,
    player: Player,
    onEvent: (GlueEvent) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var scrollEnabled by remember {mutableStateOf(true)}
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val name = getFileName(context, it)
            onEvent(GlueEvent.OnAudioAdded(it, name))
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState, enabled = scrollEnabled)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { onEvent(GlueEvent.OnExportClicked) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Export",
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        this.player = player
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = { onEvent(GlueEvent.TogglePlay) },
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        VolumeControls(
            videoVolume = videoVolume,
            musicVolume = musicVolume,
            onVideoVolumeChange = { onEvent(GlueEvent.OnVideoVolumeChanged(it)) },
            onMusicVolumeChange = { onEvent(GlueEvent.OnMusicVolumeChanged(it)) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Timeline(
            currentTimeProvider = currentTimeProvider,
            totalDurationMs = totalDurationMs,
            videoMoments = videoMoments,
            audioTracks = audioTracks,
            onSeek = { onEvent(GlueEvent.OnSeekChanged(it)) },
            onEvent = onEvent,
            onDragging = {dragging -> scrollEnabled = !dragging}
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Music Files",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            audioTracks.forEachIndexed { index, track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFa855f7), Color(0xFFec4899))),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFFa855f7),
                                        Color(0xFFec4899)
                                    )
                                ),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = track.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${track.startInTimelineMs / 1000f}s - ${track.endInTimelineMs / 1000f}s",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = { onEvent(GlueEvent.OnAudioRemoved(track.id)) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove track",
                            tint = Color.White
                        )
                    }
                }
            }

            Button(
                onClick = { galleryLauncher.launch("audio/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = audioTracks.size < 5,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7c3aed)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Music")
            }
        }
    }
}