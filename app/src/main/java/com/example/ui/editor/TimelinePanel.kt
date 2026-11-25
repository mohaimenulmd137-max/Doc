package com.example.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.MagneticTimelineEngine
import com.example.model.DocumentaryTimeline
import com.example.model.VoiceFitMode
import com.example.ui.theme.DocumentaryAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.EvidenceRed
import com.example.ui.theme.MonoTimecodeFamily
import com.example.ui.theme.PlayheadWhite
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrackAudioBg
import com.example.ui.theme.TrackAudioBorder
import com.example.ui.theme.TrackTextBg
import com.example.ui.theme.TrackTextBorder
import com.example.ui.theme.TrackVideoBg
import com.example.ui.theme.TrackVideoBorder
import com.example.ui.theme.TrackVoiceBg
import com.example.ui.theme.TrackVoiceBorder
import com.example.viewmodel.TimelineSelection
import kotlin.math.roundToLong

@Composable
fun TimelinePanel(
    timeline: DocumentaryTimeline,
    playheadMs: Long,
    isPlaying: Boolean,
    pixelsPerSecond: Float,
    selection: TimelineSelection,
    onSeek: (Long) -> Unit,
    onZoomChange: (Float) -> Unit,
    onSelect: (TimelineSelection) -> Unit,
    onResizeClip: (String, Long) -> Unit,
    onResizeText: (String, Long) -> Unit,
    onAddVisualClick: () -> Unit,
    onAddVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    val totalMs = timeline.totalDurationMs.coerceAtLeast(15_000L)
    val timelineContentWidthDp: Dp = ((totalMs / 1000f) * pixelsPerSecond).dp + 120.dp

    // Sync scroll position when playing or seeking programmatically
    LaunchedEffect(playheadMs, isPlaying, pixelsPerSecond) {
        val targetPx = with(density) { ((playheadMs / 1000f) * pixelsPerSecond).dp.toPx() }.toInt()
        if (isPlaying || !scrollState.isScrollInProgress) {
            scrollState.scrollTo(targetPx.coerceIn(0, scrollState.maxValue))
        }
    }

    // Update playhead when user scrolls timeline manually under central playhead
    LaunchedEffect(scrollState.value, scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress && !isPlaying) {
            val pxPerSec = with(density) { pixelsPerSecond.dp.toPx() }.coerceAtLeast(1f)
            val newMs = ((scrollState.value / pxPerSec) * 1000f).roundToLong().coerceIn(0L, totalMs)
            onSeek(newMs)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
    ) {
        // Timeline Zoom & Magnetic Mode Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioObsidian)
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = ElectricCyan.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "MAGNETIC • NO OVERLAP",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (timeline.silenceConfig.enabled) {
                    Surface(
                        color = EmeraldSafe.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "SILENCE TRIM ON",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSafe,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onZoomChange(pixelsPerSecond - 10f) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out Timeline", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { onZoomChange(pixelsPerSecond + 10f) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In Timeline", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Scrollable Multi-Track Canvas with Fixed Central CapCut Playhead
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val halfViewport = maxWidth / 2

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
            ) {
                // Leading spacer so 00:00 sits under center playhead
                Spacer(Modifier.width(halfViewport))

                Box(modifier = Modifier.width(timelineContentWidthDp).fillMaxHeight()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // 0. Time Ruler + Chapter Pins
                        TimeRulerBar(
                            totalMs = totalMs,
                            pixelsPerSecond = pixelsPerSecond,
                            chapters = timeline.chapters
                        )

                        // 1. Track 1: Main Visual Track (Magnetic Non-Overlapping with Drag Handles!)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            timeline.visualClips.forEach { clip ->
                                val widthDp = ((clip.durationMs / 1000f) * pixelsPerSecond).dp.coerceAtLeast(36.dp)
                                val isSelected = selection is TimelineSelection.Clip && selection.clipId == clip.id
                                MagneticClipBlock(
                                    title = clip.title,
                                    durationLabel = "${"%.1f".format(clip.durationMs / 1000f)}s",
                                    fitBadge = if (clip.voiceFitMode != VoiceFitMode.NONE) clip.voiceFitMode.shortTag else null,
                                    widthDp = widthDp,
                                    isSelected = isSelected,
                                    pixelsPerSecond = pixelsPerSecond,
                                    currentDurationMs = clip.durationMs,
                                    onSelect = { onSelect(TimelineSelection.Clip(clip.id)) },
                                    onResizeDuration = { newDur -> onResizeClip(clip.id, newDur) },
                                    testTag = "clip_block_${clip.id}"
                                )
                            }
                            // Inline '+' Add Media button at end of Magnetic Track
                            Surface(
                                color = StudioCard,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(width = 48.dp, height = 48.dp)
                                    .clickable(onClick = onAddVisualClick)
                                    .testTag("timeline_add_visual_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Video or Photo", tint = ElectricCyan)
                                }
                            }
                        }

                        // 2. Track 2: Motion Text & Evidence Overlays
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                        ) {
                            timeline.motionTexts.forEach { txt ->
                                val xDp = ((txt.startMs / 1000f) * pixelsPerSecond).dp
                                val wDp = ((txt.durationMs / 1000f) * pixelsPerSecond).dp.coerceAtLeast(48.dp)
                                val isSelected = selection is TimelineSelection.Text && selection.textId == txt.id
                                OverlayTimelinePill(
                                    label = txt.text,
                                    icon = Icons.Default.TextFields,
                                    bg = TrackTextBg,
                                    border = TrackTextBorder,
                                    isSelected = isSelected,
                                    xOffsetDp = xDp,
                                    widthDp = wDp,
                                    pixelsPerSecond = pixelsPerSecond,
                                    durationMs = txt.durationMs,
                                    onSelect = { onSelect(TimelineSelection.Text(txt.id)) },
                                    onResize = { newDur -> onResizeText(txt.id, newDur) }
                                )
                            }
                            timeline.evidenceOverlays.forEach { ev ->
                                val xDp = ((ev.startMs / 1000f) * pixelsPerSecond).dp
                                val wDp = ((ev.durationMs / 1000f) * pixelsPerSecond).dp.coerceAtLeast(44.dp)
                                val isSelected = selection is TimelineSelection.Evidence && selection.evidenceId == ev.id
                                Surface(
                                    color = EvidenceRed.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) PlayheadWhite else EvidenceRed),
                                    modifier = Modifier
                                        .offset(x = xDp)
                                        .width(wDp)
                                        .height(34.dp)
                                        .clickable { onSelect(TimelineSelection.Evidence(ev.id)) }
                                ) {
                                    Text(
                                        text = ev.type.label,
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        // 3. Track 3: Voiceover Narration + Waveform + Silence Gaps
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            if (timeline.voiceTracks.isEmpty()) {
                                Surface(
                                    color = TrackVoiceBg,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, TrackVoiceBorder.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .width(136.dp)
                                        .height(38.dp)
                                        .clickable(onClick = onAddVoiceClick)
                                        .testTag("timeline_add_voice_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = TrackVoiceBorder, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("+ Add Voiceover", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                                    }
                                }
                            } else {
                                timeline.voiceTracks.forEach { vt ->
                                    val xDp = ((vt.startMs / 1000f) * pixelsPerSecond).dp
                                    val wDp = ((vt.durationMs / 1000f) * pixelsPerSecond).dp.coerceAtLeast(60.dp)
                                    val isSelected = selection is TimelineSelection.Voice && selection.voiceId == vt.id
                                    Surface(
                                        color = TrackVoiceBg,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(
                                            if (isSelected) 2.dp else 1.dp,
                                            if (isSelected) PlayheadWhite else TrackVoiceBorder
                                        ),
                                        modifier = Modifier
                                            .offset(x = xDp)
                                            .width(wDp)
                                            .height(40.dp)
                                            .clickable { onSelect(TimelineSelection.Voice(vt.id)) }
                                            .testTag("voice_track_${vt.id}")
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            // Real PCM Waveform Canvas
                                            Canvas(modifier = Modifier.fillMaxSize().padding(vertical = 4.dp)) {
                                                val samples = vt.waveformSamples
                                                if (samples.isNotEmpty()) {
                                                    val barW = size.width / samples.size
                                                    samples.forEachIndexed { idx, amp ->
                                                        val h = (amp * size.height).coerceAtLeast(3f)
                                                        val x = idx * barW + barW * 0.5f
                                                        val yTop = (size.height - h) / 2f
                                                        drawLine(
                                                            color = if (amp < 0.12f) DocumentaryAmber.copy(alpha = 0.45f) else ElectricCyan.copy(alpha = 0.75f),
                                                            start = Offset(x, yTop),
                                                            end = Offset(x, yTop + h),
                                                            strokeWidth = (barW * 0.55f).coerceAtLeast(1.5f)
                                                        )
                                                    }
                                                }
                                            }
                                            // Protected Dramatic Pause Badges
                                            vt.silenceGaps.filter { it.isProtected }.forEach { gap ->
                                                val gx = ((gap.startMs / 1000f) * pixelsPerSecond).dp
                                                Icon(
                                                    Icons.Default.Lock,
                                                    contentDescription = "Protected Pause",
                                                    tint = DocumentaryAmber,
                                                    modifier = Modifier
                                                        .offset(x = gx, y = 2.dp)
                                                        .size(12.dp)
                                                )
                                            }
                                            Text(
                                                text = vt.title,
                                                color = TextPrimary,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Track 4: BGM (Auto-Duck) + Documentary SFX
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                        ) {
                            timeline.audioSfxItems.forEach { audio ->
                                val xDp = ((audio.startMs / 1000f) * pixelsPerSecond).dp
                                val wDp = ((audio.durationMs / 1000f) * pixelsPerSecond).dp.coerceAtLeast(48.dp)
                                val isSelected = selection is TimelineSelection.AudioSfx && selection.audioId == audio.id
                                Surface(
                                    color = TrackAudioBg,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) PlayheadWhite else TrackAudioBorder
                                    ),
                                    modifier = Modifier
                                        .offset(x = xDp)
                                        .width(wDp)
                                        .height(32.dp)
                                        .clickable { onSelect(TimelineSelection.AudioSfx(audio.id)) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = if (audio.isBgm) ElectricCyan else DocumentaryAmber,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (audio.autoDuckEnabled) "${audio.title} (Auto-Duck)" else audio.title,
                                            color = TextPrimary,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Trailing spacer so end of timeline scrolls under center playhead
                Spacer(Modifier.width(halfViewport))
            }

            // Fixed Central CapCut Playhead Line
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(PlayheadWhite)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 10.dp, height = 8.dp)
                    .background(ElectricCyan, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            )
        }
    }
}

@Composable
private fun TimeRulerBar(
    totalMs: Long,
    pixelsPerSecond: Float,
    chapters: List<com.example.model.ChapterMarker>
) {
    val totalSec = (totalMs / 1000L).toInt() + 2
    val stepSec = if (pixelsPerSecond < 22f) 10 else if (pixelsPerSecond < 40f) 5 else 2
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        for (s in 0..totalSec step stepSec) {
            val xDp = (s * pixelsPerSecond).dp
            Text(
                text = MagneticTimelineEngine.formatTimecode(s * 1000L),
                color = TextMuted,
                fontFamily = MonoTimecodeFamily,
                fontSize = 9.sp,
                modifier = Modifier.offset(x = xDp)
            )
        }
        chapters.forEach { ch ->
            val xDp = ((ch.startMs / 1000f) * pixelsPerSecond).dp
            Box(
                modifier = Modifier
                    .offset(x = xDp, y = 13.dp)
                    .size(6.dp)
                    .background(Color(ch.colorHex), RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun MagneticClipBlock(
    title: String,
    durationLabel: String,
    fitBadge: String?,
    widthDp: Dp,
    isSelected: Boolean,
    pixelsPerSecond: Float,
    currentDurationMs: Long,
    onSelect: () -> Unit,
    onResizeDuration: (Long) -> Unit,
    testTag: String
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .padding(end = 1.dp)
            .width(widthDp)
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(TrackVideoBg)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) PlayheadWhite else TrackVideoBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onSelect)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = durationLabel,
                    color = ElectricCyan,
                    fontFamily = MonoTimecodeFamily,
                    fontSize = 10.sp
                )
                if (fitBadge != null) {
                    Surface(
                        color = DocumentaryAmber.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = DocumentaryAmber, modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(fitBadge, color = DocumentaryAmber, fontSize = 8.sp)
                        }
                    }
                }
            }
        }

        // Right Drag Handle (CapCut-style handle to stretch/trim clip without overlapping neighbors!)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(16.dp)
                    .fillMaxHeight()
                    .background(PlayheadWhite)
                    .pointerInput(currentDurationMs, pixelsPerSecond) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            val pxPerSec = with(density) { pixelsPerSecond.dp.toPx() }.coerceAtLeast(1f)
                            val deltaMs = ((dragAmount / pxPerSec) * 1000f).roundToLong()
                            onResizeDuration(currentDurationMs + deltaMs)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(20.dp)
                        .background(StudioObsidian)
                )
            }
        }
    }
}

@Composable
private fun OverlayTimelinePill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    border: Color,
    isSelected: Boolean,
    xOffsetDp: Dp,
    widthDp: Dp,
    pixelsPerSecond: Float,
    durationMs: Long,
    onSelect: () -> Unit,
    onResize: (Long) -> Unit
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset(x = xOffsetDp)
            .width(widthDp)
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PlayheadWhite else border,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = border, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = TextPrimary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(14.dp)
                    .fillMaxHeight()
                    .background(PlayheadWhite)
                    .pointerInput(durationMs, pixelsPerSecond) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            val pxPerSec = with(density) { pixelsPerSecond.dp.toPx() }.coerceAtLeast(1f)
                            val deltaMs = ((dragAmount / pxPerSec) * 1000f).roundToLong()
                            onResize(durationMs + deltaMs)
                        }
                    }
            )
        }
    }
}
