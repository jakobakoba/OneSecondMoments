package com.bor96dev.edit.presentation

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay
import androidx.media3.effect.TextureOverlay
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.bor96dev.database.MomentEntity
import com.bor96dev.database.MomentsDao
import com.bor96dev.edit.presentation.event.EditEvent
import com.bor96dev.edit.presentation.state.EditState
import com.google.common.collect.ImmutableList
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel(assistedFactory = EditViewModel.Factory::class)
class EditViewModel @UnstableApi
@AssistedInject constructor(
    @Assisted private val videoUri: String,
    @Assisted private val date: Long,
    @ApplicationContext private val context: Context,
    private val momentsDao: MomentsDao,
    playerBuilder: ExoPlayer.Builder
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(videoUri: String, date: Long): EditViewModel
    }

    private val _uiState = MutableStateFlow(EditState(dateText = date.toFormattedDateString()))
    val uiState = _uiState.asStateFlow()

    val player: Player = playerBuilder.build()

    init {

        Log.d("GTA5", "loading video: $videoUri")

        player.repeatMode = Player.REPEAT_MODE_OFF
        if (player is ExoPlayer) {
            player.setSeekParameters(SeekParameters.EXACT)
        }
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        })

        val uri = videoUri.toUri()
        _uiState.update { it.copy(videoUri = uri) }
        setupInitialPlayer(uri)
    }

    private fun setupInitialPlayer(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _uiState.update { it.copy(videoDurationMs = player.duration) }
                    player.removeListener(this)
                }
            }
        })
    }

    @OptIn(UnstableApi::class)
    private suspend fun trimVideo(
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
        dateText: String
    ): Uri =
        suspendCancellableCoroutine { continuation ->
            val outputFile = File(context.filesDir, "moment_${System.currentTimeMillis()}.mp4")

            val transformer = Transformer.Builder(context).build()

            val clippingConfiguration = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs)
                .setEndPositionMs(endMs)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(inputUri)
                .setClippingConfiguration(clippingConfiguration)
                .build()

            val overlayTextSpannable = SpannableString(dateText)

            overlayTextSpannable.setSpan(
                ForegroundColorSpan(Color.WHITE),
                0,
                overlayTextSpannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            overlayTextSpannable.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                overlayTextSpannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            overlayTextSpannable.setSpan(
                AbsoluteSizeSpan(70, true),
                0,
                overlayTextSpannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val settings = StaticOverlaySettings.Builder()
                .setOverlayFrameAnchor(-1f, -1f)
                .setBackgroundFrameAnchor(-0.95f, -0.90f)
                .build()

            val textOverlay = TextOverlay.createStaticTextOverlay(overlayTextSpannable, settings)
            val overlayEffect = OverlayEffect(ImmutableList.of(textOverlay as TextureOverlay))

            val effects = Effects(
                ImmutableList.of(),
                ImmutableList.of<Effect>(overlayEffect)
            )

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(effects)
                .build()

            transformer.addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    continuation.resume(Uri.fromFile(outputFile))
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    continuation.resumeWithException(exportException)
                }
            })

            transformer.start(editedMediaItem, outputFile.absolutePath)
            continuation.invokeOnCancellation { transformer.cancel() }
        }

    fun onEvent(event: EditEvent) {
        when (event) {
            is EditEvent.OnSeekChanged -> {
                _uiState.update { it.copy(selectedStartMs = event.positionMs) }

                val currentMediaItem = player.currentMediaItem
                val isClipped = currentMediaItem?.clippingConfiguration?.startPositionMs != 0L

                if (isClipped) {
                    _uiState.value.videoUri?.let { uri ->
                        player.setMediaItem(MediaItem.fromUri(uri))
                        player.prepare()
                    }
                }
                player.pause()
                player.seekTo(event.positionMs)
            }

            is EditEvent.TogglePlay -> {
                val uri = _uiState.value.videoUri ?: return
                val startMs = _uiState.value.selectedStartMs

                val clipping = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(startMs + 1000)
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setClippingConfiguration(clipping)
                    .build()

                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            }

            is EditEvent.SaveClicked -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isSaving = true) }
                    try {
                        val startMs = _uiState.value.selectedStartMs
                        val inputUri = _uiState.value.videoUri ?: return@launch
                        val outputUri =
                            trimVideo(inputUri, startMs, startMs + 1000, _uiState.value.dateText)
                        val dateString = date.toDateString()

                        momentsDao.upsertMoment(
                            MomentEntity(
                                date = dateString,
                                videoUri = outputUri.toString()
                            )
                        )
                        _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
                    } catch (e: Exception) {
                        Log.e("GTA5", "Save error", e)
                        _uiState.update { it.copy(isSaving = false, error = "Save failed") }
                    }
                }
            }

            else -> Unit
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}

fun Long.toDateString(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
}

fun Long.toFormattedDateString(): String {
    val formatter = DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH)
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)
}