package com.bor96dev.edit.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.location.Address
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
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
import com.bor96dev.edit.domain.EditRepository
import com.bor96dev.edit.presentation.event.EditEvent
import com.bor96dev.edit.presentation.state.EditState
import com.google.common.collect.ImmutableList
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
    private val editRepository: EditRepository,
    private val playerBuilder: ExoPlayer.Builder
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(videoUri: String, date: Long): EditViewModel
    }

    private val _uiState = MutableStateFlow(EditState(dateText = date.toFormattedDateString()))
    val uiState = _uiState.asStateFlow()

    private var internalPlayer: ExoPlayer? = null
    private val _playerFlow = MutableStateFlow<Player?>(null)
    val playerFlow = _playerFlow.asStateFlow()

    companion object {
        private const val PREVIEW_LENGTH_MS = 1000L
    }

    init {
        val uri = videoUri.toUri()
        _uiState.update { it.copy(videoUri = uri) }
    }

    @OptIn(UnstableApi::class)
    fun onStart() {
        if (internalPlayer != null) return
        val player = playerBuilder.build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            setSeekParameters(SeekParameters.EXACT)
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.update { it.copy(isPlaying = isPlaying) }
                }
            })
        }
        internalPlayer = player
        _playerFlow.value = player
        setupInitialPlayer(player, videoUri.toUri())
    }

    fun onStop() {
        internalPlayer?.let {
            it.stop()
            it.release()
        }
        internalPlayer = null
        _playerFlow.value = null
    }

    private fun setupInitialPlayer(player: ExoPlayer, uri: Uri) {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _uiState.update { it.copy(videoDurationMs = player.duration) }
                    player.removeListener(this)
                    repeatClipAt(_uiState.value.selectedStartMs)
                }
            }
        })
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    private fun repeatClipAt(startMs: Long) {
        val player = internalPlayer ?: return
        val uri = _uiState.value.videoUri ?: return
        val maxStart = maxOf(0L, maxOf(_uiState.value.videoDurationMs, PREVIEW_LENGTH_MS) - PREVIEW_LENGTH_MS)
        val clampedStart = startMs.coerceIn(0L, maxStart)
        _uiState.update { it.copy(selectedStartMs = clampedStart) }

        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(clampedStart)
            .setEndPositionMs(clampedStart + PREVIEW_LENGTH_MS)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(clipping)
            .build()

        player.setMediaItem(mediaItem)
        player.playWhenReady = true
        player.prepare()
    }

    @OptIn(UnstableApi::class)
    private suspend fun trimVideo(
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
        dateText: String,
        locationText: String?
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

            val overlayTextSpannable = buildOverlayText(dateText, locationText)

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

    private fun buildOverlayText(dateText: String, locationText: String?): SpannableString {
        val fullText = if (locationText.isNullOrBlank()) {
            dateText
        } else {
            "${locationText}\n$dateText"
        }
        val spannable = SpannableString(fullText)
        spannable.setSpan(
            ForegroundColorSpan(Color.WHITE),
            0,
            spannable.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (locationText.isNullOrBlank()) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                spannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                AbsoluteSizeSpan(70, true),
                0,
                spannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else {
            val dateStart = locationText.length + 1
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                dateStart,
                spannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                AbsoluteSizeSpan(56, true),
                0,
                locationText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                AbsoluteSizeSpan(70, true),
                dateStart,
                spannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    fun onEvent(event: EditEvent) {
        when (event) {
            is EditEvent.OnSeekChanged -> {
                repeatClipAt(event.positionMs)
            }

            is EditEvent.LocationPermissionResult -> {
                if (event.granted) {
                    loadLocation()
                }
            }

            is EditEvent.SaveClicked -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isSaving = true) }
                    try {
                        val startMs = _uiState.value.selectedStartMs
                        val inputUri = _uiState.value.videoUri ?: return@launch
                        val outputUri =
                            trimVideo(
                                inputUri,
                                startMs,
                                startMs + 1000,
                                _uiState.value.dateText,
                                _uiState.value.locationText
                            )
                        val dateString = date.toDateString()

                        editRepository.upsertMoment(
                            MomentEntity(
                                date = dateString,
                                videoUri = outputUri.toString(),
                                locationText = _uiState.value.locationText
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

    private fun loadLocation() {
        if (_uiState.value.locationText != null) return
        viewModelScope.launch {
            val location = getLocation() ?: return@launch
            val locationText = resolveLocationText(location) ?: return@launch
            _uiState.update { it.copy(locationText = locationText) }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLocation(): Location? {
        if (!hasLocationPermission()) return null
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        val lastKnown = providers.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
        if (lastKnown != null) return lastKnown

        val provider = when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED -> LocationManager.GPS_PROVIDER

            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) ==
                    PackageManager.PERMISSION_GRANTED -> LocationManager.NETWORK_PROVIDER

            else -> LocationManager.PASSIVE_PROVIDER
        }

        return suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    continuation.resume(location)
                }

                override fun onProviderEnabled(provider: String) = Unit

                override fun onProviderDisabled(provider: String) = Unit
            }
            try {
                locationManager.requestLocationUpdates(
                    provider,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
                continuation.invokeOnCancellation { locationManager.removeUpdates(listener) }
            } catch (_: Exception) {
                locationManager.removeUpdates(listener)
                continuation.resume(null)
            }
        }
    }

    private suspend fun resolveLocationText(location: Location): String? {
        return withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(context, Locale.ENGLISH)
            val addresses = getAddresses(geocoder, location.latitude, location.longitude)
            val address = addresses?.firstOrNull() ?: return@withContext null
            val city = address.locality ?: address.subAdminArea ?: address.adminArea
            val country = address.countryName
            formatLocationText(city, country)
        }
    }

    private suspend fun getAddresses(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): List<Address>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) {
                                continuation.resume(addresses)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    }
                )
            }
        } else {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        onStop()
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
