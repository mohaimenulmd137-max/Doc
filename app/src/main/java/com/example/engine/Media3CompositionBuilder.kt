package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.example.model.ClipColorGrade
import com.example.model.DocumentaryTimeline
import com.example.model.EvidenceOverlay
import com.example.model.EvidenceOverlayType
import com.example.model.ExportConfig
import com.example.model.KenBurnsPreset
import com.example.model.MotionTextOverlay
import com.example.model.MotionTextStyle
import com.example.model.VisualClip
import com.example.model.VoiceFitMode
import kotlin.math.min

data class ExportChunkSpec(
    val chunkIndex: Int,
    val totalChunks: Int,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val timelineSlice: DocumentaryTimeline
)

@UnstableApi
object Media3CompositionBuilder {

    /**
     * Splits a 3-4 hour DocumentaryTimeline into safe 10-15 minute chunks (or chapter chunks)
     * so Media3 Transformer keeps RAM flat and never crashes with OutOfMemoryError on mobile.
     */
    fun planExportChunks(
        timeline: DocumentaryTimeline,
        config: ExportConfig
    ): List<ExportChunkSpec> {
        val totalMs = timeline.totalDurationMs.coerceAtLeast(1000L)
        if (config.exportChapterSplit && timeline.chapters.size > 1) {
            val sorted = timeline.chapters.sortedBy { it.startMs }
            return sorted.mapIndexed { idx, chapter ->
                val endMs = if (idx < sorted.lastIndex) sorted[idx + 1].startMs else totalMs
                ExportChunkSpec(
                    chunkIndex = idx,
                    totalChunks = sorted.size,
                    title = chapter.title,
                    startMs = chapter.startMs,
                    endMs = endMs.coerceAtLeast(chapter.startMs + 1000L),
                    timelineSlice = sliceTimeline(timeline, chapter.startMs, endMs)
                )
            }
        }

        val chunkSizeMs = (config.chunkMinutes.coerceIn(5, 30)) * 60_000L
        if (totalMs <= chunkSizeMs) {
            return listOf(
                ExportChunkSpec(
                    chunkIndex = 0,
                    totalChunks = 1,
                    title = "Full Documentary",
                    startMs = 0L,
                    endMs = totalMs,
                    timelineSlice = timeline
                )
            )
        }

        val specs = mutableListOf<ExportChunkSpec>()
        var cursor = 0L
        var index = 0
        val estimatedTotal = ((totalMs + chunkSizeMs - 1) / chunkSizeMs).toInt()
        while (cursor < totalMs) {
            val end = min(totalMs, cursor + chunkSizeMs)
            specs.add(
                ExportChunkSpec(
                    chunkIndex = index,
                    totalChunks = estimatedTotal,
                    title = "Part ${index + 1}/$estimatedTotal",
                    startMs = cursor,
                    endMs = end,
                    timelineSlice = sliceTimeline(timeline, cursor, end)
                )
            )
            cursor = end
            index++
        }
        return specs
    }

    private fun sliceTimeline(
        timeline: DocumentaryTimeline,
        startMs: Long,
        endMs: Long
    ): DocumentaryTimeline {
        val clips = timeline.visualClips.filter {
            val clipEnd = it.startMs + it.durationMs
            clipEnd > startMs && it.startMs < endMs
        }.map { clip ->
            val clippedStart = maxOf(clip.startMs, startMs)
            val clippedEnd = minOf(clip.startMs + clip.durationMs, endMs)
            val offsetInside = clippedStart - clip.startMs
            clip.copy(
                startMs = clippedStart - startMs,
                durationMs = (clippedEnd - clippedStart).coerceAtLeast(500L),
                trimStartMs = clip.trimStartMs + offsetInside
            )
        }
        val texts = timeline.motionTexts.filter {
            val tEnd = it.startMs + it.durationMs
            tEnd > startMs && it.startMs < endMs
        }.map {
            it.copy(startMs = (it.startMs - startMs).coerceAtLeast(0L))
        }
        val evidence = timeline.evidenceOverlays.filter {
            val eEnd = it.startMs + it.durationMs
            eEnd > startMs && it.startMs < endMs
        }.map {
            it.copy(startMs = (it.startMs - startMs).coerceAtLeast(0L))
        }
        return timeline.copy(
            visualClips = MagneticTimelineEngine.repackMagneticTrack(clips),
            motionTexts = texts,
            evidenceOverlays = evidence
        )
    }

    fun buildComposition(
        context: Context,
        timeline: DocumentaryTimeline,
        config: ExportConfig,
        brandWatermark: String?,
        chunkStartOffsetMs: Long = 0L
    ): Composition {
        val (outW, outH) = computeTargetDimensions(timeline, config)

        val videoItems = timeline.visualClips.map { clip ->
            buildEditedVisualItem(clip, timeline, outW, outH)
        }

        val sequences = mutableListOf<EditedMediaItemSequence>()
        if (videoItems.isNotEmpty()) {
            val hasOnlyImages = timeline.visualClips.all { it.isImage }
            val visualSeq = if (hasOnlyImages) {
                EditedMediaItemSequence.withVideoFrom(videoItems)
            } else {
                EditedMediaItemSequence.withAudioAndVideoFrom(videoItems)
            }
            sequences.add(visualSeq)
        }

        // Voiceover Audio Sequence
        val voiceItems = timeline.voiceTracks.map { vt ->
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(vt.uriString))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(0L)
                        .setEndPositionMs(vt.durationMs)
                        .build()
                )
                .build()
            EditedMediaItem.Builder(mediaItem)
                .setRemoveVideo(true)
                .build()
        }
        if (voiceItems.isNotEmpty()) {
            sequences.add(EditedMediaItemSequence.withAudioFrom(voiceItems))
        }

        // BGM / SFX Audio Sequence (with Auto-Ducking gain when voiceover is present)
        val bgmItem = timeline.audioSfxItems.firstOrNull { it.isBgm }
        if (bgmItem != null) {
            val bgmGain = if (bgmItem.autoDuckEnabled && voiceItems.isNotEmpty()) {
                bgmItem.volume * bgmItem.duckLevel
            } else {
                bgmItem.volume
            }
            val channelMix = ChannelMixingAudioProcessor()
            channelMix.putChannelMixingMatrix(
                ChannelMixingMatrix.createForConstantGain(2, 2).scaleBy(bgmGain)
            )
            val mediaItem = MediaItem.fromUri(Uri.parse(bgmItem.uriString))
            val editedBgm = EditedMediaItem.Builder(mediaItem)
                .setRemoveVideo(true)
                .setEffects(Effects(listOf(channelMix), emptyList()))
                .build()
            val seq = EditedMediaItemSequence.Builder(listOf(editedBgm))
                .setIsLooping(true)
                .build()
            sequences.add(seq)
        }

        val globalEffects = mutableListOf<Effect>(
            Presentation.createForWidthAndHeight(outW, outH, Presentation.LAYOUT_SCALE_TO_FIT)
        )

        // Render Documentary Motion Texts, Evidence Highlighters, Brand Watermark & Draft Timecode
        val overlayBitmap = renderDocumentaryOverlayBitmap(
            width = outW,
            height = outH,
            motionTexts = timeline.motionTexts,
            evidence = timeline.evidenceOverlays,
            watermark = if (config.isDraftReview) {
                "${config.draftWatermarkLabel} • ${MagneticTimelineEngine.formatTimecode(chunkStartOffsetMs)}"
            } else {
                brandWatermark
            },
            isDraft = config.isDraftReview
        )
        if (overlayBitmap != null) {
            globalEffects.add(
                OverlayEffect(listOf(BitmapOverlay.createStaticBitmapOverlay(overlayBitmap)))
            )
        }

        return Composition.Builder(sequences)
            .setEffects(Effects(emptyList(), globalEffects))
            .build()
    }

    private fun buildEditedVisualItem(
        clip: VisualClip,
        timeline: DocumentaryTimeline,
        outW: Int,
        outH: Int
    ): EditedMediaItem {
        val uri = Uri.parse(clip.uriString)
        val mediaItemBuilder = MediaItem.Builder().setUri(uri)
        if (clip.isImage) {
            mediaItemBuilder.setImageDurationMs(clip.durationMs)
        } else {
            val endPos = (clip.trimStartMs + clip.durationMs).coerceAtMost(clip.originalDurationMs)
            mediaItemBuilder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.trimStartMs)
                    .setEndPositionMs(endPos.coerceAtLeast(clip.trimStartMs + 500L))
                    .build()
            )
        }

        val videoEffects = mutableListOf<Effect>()

        // Color grade / RgbAdjustment
        val effectiveGrade = if (clip.colorGrade == ClipColorGrade.THEME_DEFAULT) {
            timeline.theme.defaultGrade
        } else {
            clip.colorGrade
        }
        buildColorAdjustment(effectiveGrade, clip)?.let { videoEffects.add(it) }

        // Ken Burns Motion (Hardware GPU MatrixTransformation)
        if (clip.kenBurns != KenBurnsPreset.NONE || clip.voiceFitMode == VoiceFitMode.FREEZE_KEN_BURNS) {
            val durationUs = (clip.durationMs * 1000f).coerceAtLeast(1_000_000f)
            val kbPreset = clip.kenBurns
            val kbTransform = MatrixTransformation { presentationTimeUs ->
                val p = (presentationTimeUs / durationUs).coerceIn(0f, 1f)
                val m = Matrix()
                when (kbPreset) {
                    KenBurnsPreset.SLOW_ZOOM_IN, KenBurnsPreset.NONE -> {
                        val s = 1.0f + 0.14f * p
                        m.postScale(s, s)
                    }
                    KenBurnsPreset.SLOW_ZOOM_OUT -> {
                        val s = 1.16f - 0.14f * p
                        m.postScale(s, s)
                    }
                    KenBurnsPreset.PAN_LEFT_RIGHT -> {
                        m.postScale(1.12f, 1.12f)
                        m.postTranslate(-0.06f + 0.12f * p, 0f)
                    }
                    KenBurnsPreset.DRAMATIC_PUSH -> {
                        val s = 1.0f + 0.24f * p * p
                        m.postScale(s, s)
                    }
                }
                m
            }
            videoEffects.add(kbTransform)
        }

        val editedBuilder = EditedMediaItem.Builder(mediaItemBuilder.build())
            .setEffects(Effects(emptyList(), videoEffects))
        if (clip.isImage) {
            editedBuilder.setFrameRate(30)
        }
        return editedBuilder.build()
    }

    private fun buildColorAdjustment(grade: ClipColorGrade, clip: VisualClip): RgbAdjustment? {
        val (rScale, gScale, bScale) = when (grade) {
            ClipColorGrade.DARK_NOIR -> Triple(0.86f, 0.88f, 0.96f)
            ClipColorGrade.COLD_CASE -> Triple(0.84f, 0.94f, 1.08f)
            ClipColorGrade.VINTAGE_ARCHIVE -> Triple(1.08f, 0.97f, 0.78f)
            ClipColorGrade.HIGH_CONTRAST -> Triple(1.10f, 1.10f, 1.10f)
            else -> Triple(1.0f, 1.0f, 1.0f)
        }
        val mult = clip.contrast.coerceIn(0.6f, 1.6f)
        return RgbAdjustment.Builder()
            .setRedScale((rScale * mult).coerceIn(0.2f, 2f))
            .setGreenScale((gScale * mult).coerceIn(0.2f, 2f))
            .setBlueScale((bScale * mult).coerceIn(0.2f, 2f))
            .build()
    }

    private fun computeTargetDimensions(
        timeline: DocumentaryTimeline,
        config: ExportConfig
    ): Pair<Int, Int> {
        val h = config.resolution.heightPx
        return when (timeline.aspectRatio) {
            com.example.model.AspectRatioPreset.RATIO_16_9 -> ((h * 16) / 9) to h
            com.example.model.AspectRatioPreset.RATIO_9_16 -> ((h * 9) / 16) to h
            com.example.model.AspectRatioPreset.RATIO_1_1 -> h to h
        }
    }

    private fun renderDocumentaryOverlayBitmap(
        width: Int,
        height: Int,
        motionTexts: List<MotionTextOverlay>,
        evidence: List<EvidenceOverlay>,
        watermark: String?,
        isDraft: Boolean
    ): Bitmap? {
        if (motionTexts.isEmpty() && evidence.isEmpty() && watermark.isNullOrBlank()) return null
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Draw primary active documentary citation / watermark cleanly
        if (!watermark.isNullOrBlank()) {
            val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDraft) Color.argb(180, 245, 158, 11) else Color.argb(130, 255, 255, 255)
                textSize = height * 0.028f
                typeface = Typeface.MONOSPACE
            }
            val bgPaint = Paint().apply {
                color = Color.argb(130, 10, 12, 16)
            }
            val pad = 16f
            val textW = wmPaint.measureText(watermark)
            val rect = RectF(width - textW - pad * 3, pad, width - pad, pad * 2.8f + wmPaint.textSize)
            canvas.drawRoundRect(rect, 10f, 10f, bgPaint)
            canvas.drawText(watermark, rect.left + pad, rect.bottom - pad * 0.7f, wmPaint)
        }

        // Draw first source citation if present
        motionTexts.firstOrNull { it.style == MotionTextStyle.SOURCE_CITATION }?.let { citation ->
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(210, 230, 236, 244)
                textSize = height * 0.024f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(citation.text, width * 0.04f, height * 0.08f, p)
        }

        // Draw classified stamp if present
        evidence.firstOrNull { it.type == EvidenceOverlayType.CLASSIFIED_STAMP }?.let { stamp ->
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(190, 239, 68, 68)
                textSize = height * 0.036f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.save()
            canvas.rotate(-12f, width * 0.8f, height * 0.22f)
            canvas.drawRect(width * 0.68f, height * 0.16f, width * 0.93f, height * 0.24f, p)
            p.style = Paint.Style.FILL
            canvas.drawText(stamp.label.ifBlank { "CLASSIFIED" }, width * 0.70f, height * 0.215f, p)
            canvas.restore()
        }

        return bmp
    }
}
