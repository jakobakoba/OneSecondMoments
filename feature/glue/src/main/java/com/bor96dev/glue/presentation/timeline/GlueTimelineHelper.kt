package com.bor96dev.glue.presentation.timeline

import com.bor96dev.glue.presentation.state.AudioTrack

const val MIN_AUDIO_DURATION_MS = 2000L

data class TrackRange(
    val start: Long,
    val end: Long
)

fun List<AudioTrack>.toTrackRanges(): List<TrackRange> =
    map { TrackRange(it.startInTimelineMs, it.endInTimelineMs) }

fun findFirstGap(
    existingTracks: List<TrackRange>,
    neededDurationMs: Long,
    totalDurationMs: Long
): Long {
    val sorted = existingTracks.sortedBy { it.start }

    if (sorted.isEmpty()) return 0L
    if (sorted.first().start >= neededDurationMs) return 0L

    for (i in 0 until sorted.size - 1) {
        val gapStart = sorted[i].end
        val gapEnd = sorted[i + 1].start
        if (gapEnd - gapStart >= neededDurationMs) return gapStart
    }

    val lastEnd = sorted.last().end
    if (totalDurationMs - lastEnd >= neededDurationMs) return lastEnd

    return findLargestGapStart(sorted, totalDurationMs)
}

fun findLargestGapStart(sorted: List<TrackRange>, totalDurationMs: Long): Long {
    if (sorted.isEmpty()) return 0L
    var bestStart = 0L
    var bestSize = sorted.first().start

    for (i in 0 until sorted.size - 1) {
        val gapStart = sorted[i].end
        val gapSize = sorted[i + 1].start - gapStart
        if (gapSize > bestSize) {
            bestSize = gapSize
            bestStart = gapStart
        }
    }

    val afterLastSize = totalDurationMs - sorted.last().end
    if (afterLastSize > bestSize) {
        bestStart = sorted.last().end
    }

    return bestStart
}

fun clampedEndForGap(
    gapStart: Long,
    existingTracks: List<TrackRange>,
    desiredEndMs: Long,
    totalDurationMs: Long
): Long {
    val sorted = existingTracks.sortedBy { it.start }
    val nextTrackStart = sorted.filter { it.start > gapStart }
        .minOfOrNull { it.start } ?: totalDurationMs
    return kotlin.math.min(desiredEndMs, nextTrackStart)
}

fun hasSpaceForNewAudio(tracks: List<TrackRange>, totalDurationMs: Long): Boolean {
    if (totalDurationMs <= 0) return false
    val sorted = tracks.sortedBy { it.start }
    var prevEnd = 0L
    for (track in sorted) {
        if (track.start - prevEnd >= MIN_AUDIO_DURATION_MS) {
            return true
        }
        prevEnd = track.end
    }
    return totalDurationMs - prevEnd >= MIN_AUDIO_DURATION_MS
}