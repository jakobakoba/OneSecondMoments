package com.bor96dev.glue.presentation

import androidx.lifecycle.ViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.bor96dev.glue.domain.GlueRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = GlueViewModel.Factory::class)
class GlueViewModel @UnstableApi
@AssistedInject constructor(
    @Assisted private val monthQuery: String?,
    @Assisted private val year: Int?,
    private val repository: GlueRepository,
    playerBuilder: ExoPlayer.Builder
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(monthQuery: String?, year: Int?): GlueViewModel
    }
}