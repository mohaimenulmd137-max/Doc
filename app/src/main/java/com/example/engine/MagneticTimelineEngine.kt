package com.example.engine

import com.example.model.AudioSfxItem
import com.example.model.ChapterMarker
import com.example.model.ClipColorGrade
import com.example.model.ClipTransition
import com.example.model.DocumentaryTheme
import com.example.model.DocumentaryTimeline
import com.example.model.KenBurnsPreset
import com.example.model.MotionTextOverlay
import com.example.model.MotionTextStyle
import com.example.model.VisualClip
import com.example.model.VoiceFitMode
import com.example.model.VoiceTrackItem
import java.util.Locale
import java.util.UUID

object MagneticTimelineEngine {

    /**
     * Enforces CapCut-style Magnetic Non-Overlapping layout on the main visual track.
     * Clips never collide or overlap; every clip starts cleanly right after the previous clip.
     */
    fun repackMagneticTrack(clips: List<VisualClip>): List<VisualClip> {
        var cursorMs = 0L
        return clips.map { clip ->
            val safeDuration = clip.durationMs.coerceAtLeast(500L)
            val updated = clip.copy(startMs = cursorMs, durationMs = safeDuration)
            cursorMs += safeDuration
            updated
        }
    }

    /**
     * Resizes a clip's duration by dragging either its start/end handle and ripples subsequent clips
     * so there is zero overlap. If dragged longer than original video length, automatically
     * activates Smooth Slow-Mo or Freeze+KenBurns fit so the clip never fails.
     */
    fun resizeClipWithRipple(
        clips: List<VisualClip>,
        clipId: String,
        newDurationMs: Long,
        preferredFitMode: VoiceFitMode = VoiceFitMode.SMOOTH_SLOW_MO
    ): List<VisualClip> {
        val updated = clips.map { clip ->
            if (clip.id != clipId) {
                clip
            } else {
                val clampedDuration = newDurationMs.coerceIn(600L, 60 * 60 * 1000L)
                if (!clip.isImage && clampedDuration > clip.originalDurationMs && clip.originalDurationMs > 0L) {
                    val speed = (clip.originalDurationMs.toFloat() / clampedDuration.toFloat())
                        .coerceIn(0.45f, 1.0f)
                    val mode = if (preferredFitMode == VoiceFitMode.NONE) {
                        VoiceFitMode.SMOOTH_SLOW_MO
                    } else {
                        preferredFitMode
                    }
                    clip.copy(
                        durationMs = clampedDuration,
                        voiceFitMode = mode,
                        playbackSpeed = if (mode == VoiceFitMode.SMOOTH_SLOW_MO) speed else 1.0f
                    )
                } else {
                    clip.copy(
                        durationMs = clampedDuration,
                        voiceFitMode = if (clampedDuration <= clip.originalDurationMs) VoiceFitMode.NONE else clip.voiceFitMode,
                        playbackSpeed = if (clampedDuration <= clip.originalDurationMs) 1.0f else clip.playbackSpeed
                    )
                }
            }
        }
        return repackMagneticTrack(updated)
    }

    /**
     * Matches a clip (e.g., 8s B-roll) cleanly to a target voice segment duration (e.g., 10s)
     * using Smooth Slow-Mo, Freeze + Ken Burns Hold, or Seamless Ambient Loop.
     */
    fun applyVoiceFit(
        clips: List<VisualClip>,
        clipId: String,
        targetDurationMs: Long,
        mode: VoiceFitMode
    ): List<VisualClip> {
        val safeTarget = targetDurationMs.coerceAtLeast(1000L)
        val updated = clips.map { clip ->
            if (clip.id != clipId) {
                clip
            } else {
                when (mode) {
                    VoiceFitMode.NONE -> clip.copy(
                        durationMs = clip.originalDurationMs.coerceAtLeast(1000L),
                        voiceFitMode = VoiceFitMode.NONE,
                        playbackSpeed = 1.0f
                    )
                    VoiceFitMode.SMOOTH_SLOW_MO -> {
                        val speed = if (clip.isImage || clip.originalDurationMs <= 0L) {
                            1.0f
                        } else {
                            (clip.originalDurationMs.toFloat() / safeTarget.toFloat()).coerceIn(0.40f, 1.50f)
                        }
                        clip.copy(
                            durationMs = safeTarget,
                            voiceFitMode = VoiceFitMode.SMOOTH_SLOW_MO,
                            playbackSpeed = speed
                        )
                    }
                    VoiceFitMode.FREEZE_KEN_BURNS -> clip.copy(
                        durationMs = safeTarget,
                        voiceFitMode = VoiceFitMode.FREEZE_KEN_BURNS,
                        playbackSpeed = 1.0f,
                        kenBurns = if (clip.kenBurns == KenBurnsPreset.NONE) {
                            KenBurnsPreset.SLOW_ZOOM_IN
                        } else {
                            clip.kenBurns
                        }
                    )
                    VoiceFitMode.SEAMLESS_LOOP -> clip.copy(
                        durationMs = safeTarget,
                        voiceFitMode = VoiceFitMode.SEAMLESS_LOOP,
                        playbackSpeed = 1.0f
                    )
                }
            }
        }
        return repackMagneticTrack(updated)
    }

    fun splitClipAt(clips: List<VisualClip>, clipId: String, playheadMs: Long): List<VisualClip> {
        val result = mutableListOf<VisualClip>()
        for (clip in clips) {
            if (clip.id != clipId) {
                result.add(clip)
                continue
            }
            val relativeOffset = playheadMs - clip.startMs
            if (relativeOffset < 500L || clip.durationMs - relativeOffset < 500L) {
                result.add(clip)
                continue
            }
            val firstHalf = clip.copy(
                id = UUID.randomUUID().toString(),
                durationMs = relativeOffset
            )
            val secondHalf = clip.copy(
                id = UUID.randomUUID().toString(),
                trimStartMs = if (clip.isImage) 0L else clip.trimStartMs + relativeOffset,
                durationMs = clip.durationMs - relativeOffset,
                transitionIn = ClipTransition.NONE
            )
            result.add(firstHalf)
            result.add(secondHalf)
        }
        return repackMagneticTrack(result)
    }

    fun deleteClipWithRipple(clips: List<VisualClip>, clipId: String): List<VisualClip> {
        return repackMagneticTrack(clips.filterNot { it.id == clipId })
    }

    fun duplicateClip(clips: List<VisualClip>, clipId: String): List<VisualClip> {
        val result = mutableListOf<VisualClip>()
        for (clip in clips) {
            result.add(clip)
            if (clip.id == clipId) {
                result.add(clip.copy(id = UUID.randomUUID().toString()))
            }
        }
        return repackMagneticTrack(result)
    }

    fun applyColorToAll(
        clips: List<VisualClip>,
        grade: ClipColorGrade,
        brightness: Float,
        contrast: Float,
        saturation: Float
    ): List<VisualClip> {
        return clips.map {
            it.copy(
                colorGrade = grade,
                brightness = brightness,
                contrast = contrast,
                saturation = saturation
            )
        }
    }

    /**
     * One-tap Auto Documentary Assembly: pairs imported voiceovers, B-roll/evidence photos,
     * and BGM into a complete magnetic, ducked, animated documentary timeline.
     */
    fun autoAssembleDocumentary(
        theme: DocumentaryTheme,
        visualInputs: List<VisualClip>,
        voiceTracks: List<VoiceTrackItem>,
        bgmItem: AudioSfxItem?,
        autoKenBurns: Boolean,
        autoDucking: Boolean,
        autoFitToVoice: VoiceFitMode,
        projectTitle: String
    ): DocumentaryTimeline {
        val totalVoiceMs = voiceTracks.sumOf { it.durationMs }.coerceAtLeast(
            visualInputs.sumOf { it.durationMs }.coerceAtLeast(15_000L)
        )

        val kbRotation = listOf(
            KenBurnsPreset.SLOW_ZOOM_IN,
            KenBurnsPreset.PAN_LEFT_RIGHT,
            KenBurnsPreset.SLOW_ZOOM_OUT,
            KenBurnsPreset.DRAMATIC_PUSH
        )

        val assembledClips = if (visualInputs.isEmpty()) {
            emptyList()
        } else {
            val targetPerClip = (totalVoiceMs / visualInputs.size).coerceAtLeast(3_500L)
            visualInputs.mapIndexed { index, clip ->
                val needsExtend = !clip.isImage && clip.originalDurationMs in 1L until targetPerClip
                val speed = if (needsExtend && autoFitToVoice == VoiceFitMode.SMOOTH_SLOW_MO) {
                    (clip.originalDurationMs.toFloat() / targetPerClip.toFloat()).coerceIn(0.45f, 1.0f)
                } else {
                    1.0f
                }
                clip.copy(
                    durationMs = targetPerClip,
                    kenBurns = if (autoKenBurns) kbRotation[index % kbRotation.size] else KenBurnsPreset.NONE,
                    colorGrade = theme.defaultGrade,
                    transitionIn = if (index == 0) ClipTransition.NONE else theme.defaultTransition,
                    voiceFitMode = if (needsExtend) autoFitToVoice else VoiceFitMode.NONE,
                    playbackSpeed = speed
                )
            }
        }

        val magneticClips = repackMagneticTrack(assembledClips)

        // Position voice tracks sequentially
        var voiceCursor = 0L
        val sequencedVoice = voiceTracks.map { vt ->
            val positioned = vt.copy(startMs = voiceCursor)
            voiceCursor += vt.durationMs
            positioned
        }

        // Auto-duck BGM across timeline
        val audioList = if (bgmItem != null) {
            listOf(
                bgmItem.copy(
                    startMs = 0L,
                    durationMs = maxOf(totalVoiceMs, magneticClips.sumOf { it.durationMs }),
                    autoDuckEnabled = autoDucking,
                    duckLevel = theme.defaultDuckLevel,
                    isBgm = true
                )
            )
        } else {
            emptyList()
        }

        // Starter documentary motion texts & chapters
        val chapters = mutableListOf(
            ChapterMarker(title = "01 • Opening Hook", startMs = 0L, colorHex = 0xFF00D1FF)
        )
        if (magneticClips.size >= 3) {
            val midClip = magneticClips[magneticClips.size / 2]
            chapters.add(
                ChapterMarker(title = "02 • Investigation & Evidence", startMs = midClip.startMs, colorHex = 0xFFF59E0B)
            )
        }

        val starterTexts = listOf(
            MotionTextOverlay(
                text = projectTitle,
                subText = theme.bengaliTag,
                style = MotionTextStyle.IMPACT_CHAPTER_TITLE,
                startMs = 500L,
                durationMs = 4000L,
                yPositionFraction = 0.50f
            ),
            MotionTextOverlay(
                text = "ARCHIVE FOOTAGE • CASE FILE",
                subText = "DocuCut Verified Timeline",
                style = MotionTextStyle.SOURCE_CITATION,
                startMs = 4800L,
                durationMs = 4000L,
                yPositionFraction = 0.14f
            )
        )

        return DocumentaryTimeline(
            theme = theme,
            vignetteIntensity = theme.defaultVignette,
            filmGrainIntensity = theme.defaultGrain,
            visualClips = magneticClips,
            motionTexts = starterTexts,
            voiceTracks = sequencedVoice,
            audioSfxItems = audioList,
            chapters = chapters
        )
    }

    fun generateYoutubeChapters(chapters: List<ChapterMarker>): String {
        if (chapters.isEmpty()) return "00:00 Introduction"
        return chapters.sortedBy { it.startMs }.joinToString("\n") { marker ->
            "${formatTimecode(marker.startMs, includeMillis = false)} ${marker.title}"
        }
    }

    fun formatTimecode(ms: Long, includeMillis: Boolean = false): String {
        val safeMs = ms.coerceAtLeast(0L)
        val totalSeconds = safeMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        val centis = (safeMs % 1000L) / 10L
        return if (hours > 0) {
            if (includeMillis) {
                String.format(Locale.US, "%02d:%02d:%02d:%02d", hours, minutes, seconds, centis)
            } else {
                String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            }
        } else {
            if (includeMillis) {
                String.format(Locale.US, "%02d:%02d:%02d", minutes, seconds, centis)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }
    }
}
