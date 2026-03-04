package com.bor96dev.glue

import com.bor96dev.glue.presentation.timeline.TrackRange
import com.bor96dev.glue.presentation.timeline.MIN_AUDIO_DURATION_MS
import com.bor96dev.glue.presentation.timeline.clampedEndForGap
import com.bor96dev.glue.presentation.timeline.findFirstGap
import com.bor96dev.glue.presentation.timeline.hasSpaceForNewAudio
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class GlueTimelineHelperTest : StringSpec({
    "findFirstGap возвращает 0 если треки пустые" {
        findFirstGap(emptyList(), neededDurationMs = 1000L, totalDurationMs = 10_000L) shouldBe 0L
    }

    "findFirstGap подбирает первый промежуток" {
        val tracks = listOf(
            trackRange(0L, 1000L),
            trackRange(3000L, 4000L)
        )

        findFirstGap(tracks, neededDurationMs = 1500L, totalDurationMs = 10_000L) shouldBe 1000L
    }

    "findFirstGap возвращает конец дорожки если по середине не помещается" {
        val tracks = listOf(
            trackRange(0L, 5000L),
            trackRange(5200L, 5400L)
        )

        findFirstGap(tracks, neededDurationMs = 3000L, totalDurationMs = 10_000L) shouldBe 5400L
    }

    "clampedEndForGap не вылезает за следующий трек" {
        val tracks = listOf(
            trackRange(0L, 2000L),
            trackRange(5000L, 6000L)
        )
        val clamped = clampedEndForGap(
            gapStart = 2000L,
            existingTracks = tracks,
            desiredEndMs = 5500L,
            totalDurationMs = 10_000L
        )
        clamped shouldBe 5000L
    }

    "clampedEndForGap не вылезает за totalDuration" {
        val tracks = listOf(
            trackRange(0L, 2000L)
        )
        val clamped = clampedEndForGap(
            gapStart = 2000L,
            existingTracks = tracks,
            desiredEndMs = 9500L,
            totalDurationMs = 9000L
        )
        clamped shouldBe 9000L
    }

    "hasSpaceForNewAudio учитывает минимальный отступ" {
        val tracks = listOf(
            trackRange(0L, MIN_AUDIO_DURATION_MS),
            trackRange(MIN_AUDIO_DURATION_MS * 2, MIN_AUDIO_DURATION_MS * 3)
        )
        hasSpaceForNewAudio(tracks, totalDurationMs = MIN_AUDIO_DURATION_MS * 4) shouldBe true
    }

    "hasSpaceForNewAudio возвращает false если нет пространства" {
        val tracks = listOf(
            trackRange(0L, 4000L),
            trackRange(4000L, 8000L)
        )
        hasSpaceForNewAudio(tracks, totalDurationMs = 8000L) shouldBe false
    }
})

private fun trackRange(start: Long, end: Long) = TrackRange(start, end)
