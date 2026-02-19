package com.bor96dev.glue.presentation

import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.bor96dev.glue.presentation.composables.Timeline
import com.bor96dev.glue.presentation.composables.VolumeControls
import com.bor96dev.glue.presentation.event.GlueEvent
import com.bor96dev.glue.presentation.state.GlueState
import com.google.common.math.LinearTransformation.vertical

@Composable
fun GlueScreen(
    state: GlueState,
    player: Player,
    onEvent: (GlueEvent) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let{onEvent(GlueEvent.OnAudioAdded(it, "track.mp3"))}
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
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
                text = state.title,
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
            videoVolume = state.videoVolume,
            musicVolume = state.musicVolume,
            onVideoVolumeChange = {onEvent(GlueEvent.OnVideoVolumeChanged(it))},
            onMusicVolumeChange = {onEvent(GlueEvent.OnMusicVolumeChanged(it))}
        )

        Spacer(modifier = Modifier.height(24.dp))

        Timeline(
            currentTimeMs = state.currentTimeMs,
            totalDurationMs = state.totalDurationMs,
            videoMoments = state.videoMoments,
            audioTracks = state.audioTracks,
            onSeek = {onEvent(GlueEvent.OnSeekChanged(it))}
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Music Files",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.audioTracks.isEmpty()){
            Button (
                onClick = {galleryLauncher.launch("audio/*")},
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7c3aed)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Music")
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.audioTracks.forEach{ track ->
                    Row (
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(listOf(Color(0xFFa855f7), Color (0xFFec4899))),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box (
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFFa855f7), Color(0xFFec4899))),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "1", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ){
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

                        Button(
                            onClick = {galleryLauncher.launch("audio/*")},
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7c3aed)),
                            shape  = RoundedCornerShape(8.dp)
                        ) {
                            Text("Choose music", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}