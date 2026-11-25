package com.example.ui.editor

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.engine.MagneticTimelineEngine
import com.example.engine.SfxSynthesizer
import com.example.model.AspectRatioPreset
import com.example.model.ClipColorGrade
import com.example.model.DocumentaryTimeline
import com.example.model.EvidenceOverlay
import com.example.model.EvidenceOverlayType
import com.example.model.KenBurnsPreset
import com.example.model.MotionTextOverlay
import com.example.model.MotionTextStyle
import com.example.model.VisualClip
import com.example.model.VoiceFitMode
import com.example.ui.theme.BengaliDocFontFamily
import com.example.ui.theme.DocumentaryAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EvidenceRed
import com.example.ui.theme.MonoTimecodeFamily
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun PreviewViewport(
    timeline: DocumentaryTimeline,
    playheadMs: Long,
    isPlaying: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    brandWatermark: String?,
    onTogglePlay: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onJumpToChapter: (Long) -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val activeClip = remember(timeline.visualClips, playheadMs) {
        timeline.visualClips.firstOrNull { playheadMs in it.startMs until (it.startMs + it.durationMs) }
            ?: timeline.visualClips.lastOrNull()
    }
    val activeTexts = remember(timeline.motionTexts, playheadMs) {
        timeline.motionTexts.filter { playheadMs in it.startMs..(it.startMs + it.durationMs) }
    }
    val activeEvidence = remember(timeline.evidenceOverlays, playheadMs) {
        timeline.evidenceOverlays.filter { playheadMs in it.startMs..(it.startMs + it.durationMs) }
    }

    // Real Media3 ExoPlayer for Video Clips, Voiceover, and Auto-Ducked BGM
    val videoPlayer = remember { ExoPlayer.Builder(context).build() }
    val voicePlayer = remember { ExoPlayer.Builder(context).build() }
    val bgmPlayer = remember {
        ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ALL }
    }

    var loadedVideoUri by remember { mutableStateOf<String?>(null) }
    var loadedVoiceUri by remember { mutableStateOf<String?>(null) }
    var loadedBgmUri by remember { mutableStateOf<String?>(null) }
    var lastTriggeredSfxId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            videoPlayer.release()
            voicePlayer.release()
            bgmPlayer.release()
        }
    }

    // Sync active video clip to ExoPlayer
    LaunchedEffect(activeClip?.id, activeClip?.uriString, activeClip?.isImage) {
        if (activeClip != null && !activeClip.isImage && activeClip.uriString.isNotBlank()) {
            if (loadedVideoUri != activeClip.uriString) {
                loadedVideoUri = activeClip.uriString
                videoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(activeClip.uriString)))
                videoPlayer.prepare()
            }
            videoPlayer.setPlaybackSpeed(activeClip.playbackSpeed.coerceIn(0.4f, 2f))
        } else {
            videoPlayer.pause()
        }
    }

    // Sync Voiceover & BGM with live Auto-Ducking
    val activeVoice = remember(timeline.voiceTracks, playheadMs) {
        timeline.voiceTracks.firstOrNull { playheadMs in it.startMs..(it.startMs + it.durationMs) }
    }
    val activeBgm = remember(timeline.audioSfxItems) {
        timeline.audioSfxItems.firstOrNull { it.isBgm }
    }

    LaunchedEffect(activeVoice?.uriString) {
        val uriStr = activeVoice?.uriString
        if (!uriStr.isNullOrBlank() && loadedVoiceUri != uriStr) {
            loadedVoiceUri = uriStr
            voicePlayer.setMediaItem(MediaItem.fromUri(Uri.parse(uriStr)))
            voicePlayer.prepare()
        }
    }
    LaunchedEffect(activeBgm?.uriString) {
        val uriStr = activeBgm?.uriString
        if (!uriStr.isNullOrBlank() && loadedBgmUri != uriStr) {
            loadedBgmUri = uriStr
            bgmPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(uriStr)))
            bgmPlayer.prepare()
        }
    }

    // Play/Pause + Auto-Ducking + SFX trigger loop
    LaunchedEffect(isPlaying, playheadMs, activeVoice?.id, activeBgm?.id) {
        if (activeClip != null && !activeClip.isImage) {
            val offsetInClip = (playheadMs - activeClip.startMs + activeClip.trimStartMs).coerceAtLeast(0L)
            if (abs(videoPlayer.currentPosition - offsetInClip) > 350L) {
                videoPlayer.seekTo(offsetInClip)
            }
            if (isPlaying && !videoPlayer.isPlaying) videoPlayer.play() else if (!isPlaying) videoPlayer.pause()
        }

        if (activeVoice != null && activeVoice.uriString.isNotBlank()) {
            val vOffset = (playheadMs - activeVoice.startMs).coerceAtLeast(0L)
            if (abs(voicePlayer.currentPosition - vOffset) > 350L) {
                voicePlayer.seekTo(vOffset)
            }
            voicePlayer.volume = activeVoice.volume
            if (isPlaying && !voicePlayer.isPlaying) voicePlayer.play() else if (!isPlaying) voicePlayer.pause()
        } else {
            voicePlayer.pause()
        }

        if (activeBgm != null && activeBgm.uriString.isNotBlank()) {
            val isSpeechSpeaking = activeVoice != null && run {
                val bin = if (activeVoice.durationMs > 0 && activeVoice.waveformSamples.isNotEmpty()) {
                    (((playheadMs - activeVoice.startMs).toFloat() / activeVoice.durationMs) * activeVoice.waveformSamples.size)
                        .toInt().coerceIn(0, activeVoice.waveformSamples.lastIndex)
                } else -1
                if (bin >= 0) activeVoice.waveformSamples[bin] > 0.14f else true
            }
            bgmPlayer.volume = if (activeBgm.autoDuckEnabled && isSpeechSpeaking) {
                (activeBgm.volume * activeBgm.duckLevel).coerceIn(0f, 1f)
            } else {
                activeBgm.volume.coerceIn(0f, 1f)
            }
            if (isPlaying && !bgmPlayer.isPlaying) bgmPlayer.play() else if (!isPlaying) bgmPlayer.pause()
        } else {
            bgmPlayer.pause()
        }

        if (isPlaying) {
            val sfxHit = timeline.audioSfxItems.firstOrNull { item ->
                !item.isBgm && item.sfxPresetId != null && abs(playheadMs - item.startMs) < 75L
            }
            if (sfxHit != null && lastTriggeredSfxId != sfxHit.id) {
                lastTriggeredSfxId = sfxHit.id
                sfxHit.sfxPresetId?.let { SfxSynthesizer.previewPreset(context, it) }
            }
        } else {
            lastTriggeredSfxId = null
        }
    }

    val aspectFloat = when (timeline.aspectRatio) {
        AspectRatioPreset.RATIO_16_9 -> 16f / 9f
        AspectRatioPreset.RATIO_9_16 -> 9f / 16f
        AspectRatioPreset.RATIO_1_1 -> 1f
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Video Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .aspectRatio(aspectFloat)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioObsidian)
                    .clipToBounds()
            ) {
                val boxW = maxWidth
                val boxH = maxHeight

                // 1. Visual Layer (Video ExoPlayer OR Coil Image with Live Ken Burns Motion)
                if (activeClip != null) {
                    val progress = ((playheadMs - activeClip.startMs).toFloat() /
                        activeClip.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                    val (kbScale, kbTx) = computeKenBurnsTransform(activeClip, progress)
                    val colorMatrix = buildComposeColorMatrix(activeClip, timeline.theme.defaultGrade)

                    if (activeClip.isImage) {
                        AsyncImage(
                            model = Uri.parse(activeClip.uriString),
                            contentDescription = activeClip.title,
                            contentScale = ContentScale.Crop,
                            colorFilter = ColorFilter.colorMatrix(colorMatrix),
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = kbScale
                                    scaleY = kbScale
                                    translationX = kbTx * size.width
                                }
                        )
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = videoPlayer
                                    useController = false
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = kbScale
                                    scaleY = kbScale
                                    translationX = kbTx * size.width
                                }
                        )
                    }
                } else {
                    Text(
                        text = "+ Add Video or Evidence Photo below",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // 2. Noir Vignette & Film Grain Shader Preview
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (timeline.vignetteIntensity > 0.02f) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = (timeline.vignetteIntensity * 0.85f).coerceIn(0f, 0.9f))
                                ),
                                center = center,
                                radius = size.maxDimension * 0.72f
                            )
                        )
                    }
                    if (timeline.filmGrainIntensity > 0.02f) {
                        val grainAlpha = (timeline.filmGrainIntensity * 0.16f).coerceIn(0f, 0.25f)
                        val seed = (playheadMs / 60L).toInt()
                        for (i in 0 until 24) {
                            val gx = ((seed * 37 + i * 97) % 100) / 100f * size.width
                            val gy = ((seed * 53 + i * 71) % 100) / 100f * size.height
                            drawCircle(Color.White.copy(alpha = grainAlpha), radius = 1.6f, center = Offset(gx, gy))
                        }
                    }

                    // Safe Zone Margin Guide (YouTube / Shorts Safe Area)
                    if (timeline.safeZoneEnabled) {
                        val marginX = size.width * 0.08f
                        val marginY = size.height * 0.10f
                        drawRect(
                            color = ElectricCyan.copy(alpha = 0.55f),
                            topLeft = Offset(marginX, marginY),
                            size = Size(size.width - marginX * 2, size.height - marginY * 2),
                            style = Stroke(width = 1.5f)
                        )
                    }
                }

                // 3. Evidence Overlays (Highlighter, Red Box, Redact, Stamp)
                activeEvidence.forEach { ev ->
                    val evProgress = ((playheadMs - ev.startMs).toFloat() / ev.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                    EvidenceOverlayLayer(ev, evProgress, boxW.value, boxH.value)
                }

                // 4. Kinetic Motion Text Overlays
                activeTexts.forEach { txt ->
                    val txtProgress = ((playheadMs - txt.startMs).toFloat() / txt.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                    MotionTextPreviewLayer(txt, txtProgress)
                }

                // 5. Client Brand Watermark OR Draft Review Timecode Burn-In
                if (timeline.showDraftWatermark) {
                    Surface(
                        color = StudioObsidian.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, DocumentaryAmber),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "DRAFT REVIEW • ${MagneticTimelineEngine.formatTimecode(playheadMs, includeMillis = true)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = DocumentaryAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (!brandWatermark.isNullOrBlank()) {
                    Text(
                        text = brandWatermark,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }
        }

        // CapCut Transport Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioObsidian)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = MagneticTimelineEngine.formatTimecode(playheadMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan,
                    fontFamily = MonoTimecodeFamily
                )
                Text(
                    text = " / ${MagneticTimelineEngine.formatTimecode(timeline.totalDurationMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    fontFamily = MonoTimecodeFamily
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.testTag("editor_undo_btn")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) TextPrimary else TextMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan)
                        .clickable(onClick = onTogglePlay)
                        .testTag("editor_play_pause_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = StudioObsidian,
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.testTag("editor_redo_btn")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) TextPrimary else TextMuted
                    )
                }
            }

            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier.testTag("editor_fullscreen_btn")
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = "Toggle Preview Size", tint = TextSecondary)
            }
        }

        // Chapter Jump Strip (1-tap navigation for 3-4 hour timelines)
        if (timeline.chapters.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioObsidian)
            ) {
                items(timeline.chapters, key = { it.id }) { chapter ->
                    val isNear = abs(playheadMs - chapter.startMs) < 1500L
                    Surface(
                        color = if (isNear) ElectricCyan.copy(alpha = 0.2f) else StudioCard,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isNear) ElectricCyan else StudioBorder),
                        modifier = Modifier
                            .clickable { onJumpToChapter(chapter.startMs) }
                            .testTag("chapter_pill_${chapter.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = Color(chapter.colorHex),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${MagneticTimelineEngine.formatTimecode(chapter.startMs)} ${chapter.title}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isNear) ElectricCyan else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EvidenceOverlayLayer(
    ev: EvidenceOverlay,
    progress: Float,
    boxW: Float,
    boxH: Float
) {
    val sweep = (progress * 3.5f).coerceIn(0f, 1f)
    Box(modifier = Modifier.fillMaxSize()) {
        when (ev.type) {
            EvidenceOverlayType.YELLOW_HIGHLIGHT -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(ev.widthFraction * sweep)
                        .height(26.dp)
                        .background(DocumentaryAmber.copy(alpha = 0.48f))
                )
            }
            EvidenceOverlayType.RED_CIRCLE_CALLOUT -> {
                Surface(
                    color = Color.Transparent,
                    border = BorderStroke(2.5.dp, EvidenceRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(ev.widthFraction)
                        .height(48.dp)
                ) {}
            }
            EvidenceOverlayType.REDACTION_BLUR -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(ev.widthFraction)
                        .height(28.dp)
                        .background(Color.Black.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("REDACTED", color = EvidenceRed, style = MaterialTheme.typography.labelSmall)
                }
            }
            EvidenceOverlayType.CLASSIFIED_STAMP -> {
                Surface(
                    color = EvidenceRed.copy(alpha = 0.14f),
                    border = BorderStroke(2.dp, EvidenceRed),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 28.dp, end = 16.dp)
                        .graphicsLayer { rotationZ = -10f }
                ) {
                    Text(
                        text = ev.label.ifBlank { "CLASSIFIED" },
                        color = EvidenceRed,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            EvidenceOverlayType.SPOTLIGHT_DIM -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.40f))
                )
            }
        }
    }
}

@Composable
private fun MotionTextPreviewLayer(txt: MotionTextOverlay, progress: Float) {
    val accent = Color(txt.accentColorHex)
    val enter = (progress * 4f).coerceIn(0f, 1f)
    val alignment = when (txt.style) {
        MotionTextStyle.IMPACT_CHAPTER_TITLE -> Alignment.Center
        MotionTextStyle.SOURCE_CITATION -> Alignment.TopStart
        else -> Alignment.BottomStart
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        contentAlignment = alignment
    ) {
        when (txt.style) {
            MotionTextStyle.TYPEWRITER_CASE_FILE -> {
                val visibleCount = (txt.text.length * (progress * 1.8f).coerceIn(0f, 1f)).toInt()
                val shown = txt.text.take(visibleCount)
                Surface(
                    color = StudioObsidian.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = shown + if ((progress * 10).toInt() % 2 == 0) "_" else "",
                            color = TextPrimary,
                            fontFamily = BengaliDocFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (txt.subText.isNotBlank()) {
                            Text(
                                text = txt.subText,
                                color = accent,
                                fontFamily = MonoTimecodeFamily,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            MotionTextStyle.LOWER_THIRD_IDENTITY -> {
                Row(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = enter
                            translationX = (1f - enter) * -36f
                        }
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioObsidian.copy(alpha = 0.86f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(28.dp)
                            .background(accent)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = txt.text,
                            color = TextPrimary,
                            fontFamily = BengaliDocFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (txt.subText.isNotBlank()) {
                            Text(text = txt.subText, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            MotionTextStyle.MARKER_HIGHLIGHT_QUOTE -> {
                Surface(
                    color = StudioObsidian.copy(alpha = 0.80f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Box(modifier = Modifier.padding(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(enter)
                                .height(20.dp)
                                .background(accent.copy(alpha = 0.42f))
                        )
                        Column {
                            Text(
                                text = "“${txt.text}”",
                                color = TextPrimary,
                                fontFamily = BengaliDocFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (txt.subText.isNotBlank()) {
                                Text(text = txt.subText, color = accent, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            MotionTextStyle.KINETIC_WORD_POP -> {
                val words = txt.text.split(" ")
                val count = (words.size * (progress * 1.5f).coerceIn(0f, 1f)).toInt().coerceAtLeast(1)
                val shown = words.take(count).joinToString(" ")
                Surface(
                    color = StudioObsidian.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = shown,
                        color = accent,
                        fontFamily = BengaliDocFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            MotionTextStyle.NUMBER_DATE_COUNTER -> {
                val targetNum = txt.text.filter { it.isDigit() }.toIntOrNull() ?: 2026
                val currentNum = (targetNum * (progress * 1.6f).coerceIn(0f, 1f)).toInt()
                val display = if (txt.text.any { it.isDigit() }) {
                    txt.text.replaceFirst(Regex("\\d+"), currentNum.toString())
                } else txt.text
                Surface(
                    color = StudioObsidian.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accent)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(
                            text = display,
                            color = accent,
                            fontFamily = MonoTimecodeFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (txt.subText.isNotBlank()) {
                            Text(text = txt.subText, color = TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }

            MotionTextStyle.IMPACT_CHAPTER_TITLE -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioObsidian.copy(alpha = 0.65f))
                        .padding(12.dp)
                        .graphicsLayer {
                            val s = 0.92f + 0.08f * enter
                            scaleX = s
                            scaleY = s
                            alpha = enter
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = txt.text,
                            color = TextPrimary,
                            fontFamily = BengaliDocFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        if (txt.subText.isNotBlank()) {
                            Text(
                                text = txt.subText,
                                color = accent,
                                fontFamily = MonoTimecodeFamily,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            MotionTextStyle.SOURCE_CITATION -> {
                Surface(
                    color = StudioObsidian.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = txt.text,
                        color = TextSecondary,
                        fontFamily = MonoTimecodeFamily,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

private fun computeKenBurnsTransform(clip: VisualClip, progress: Float): Pair<Float, Float> {
    val kb = if (clip.voiceFitMode == VoiceFitMode.FREEZE_KEN_BURNS && clip.kenBurns == KenBurnsPreset.NONE) {
        KenBurnsPreset.SLOW_ZOOM_IN
    } else clip.kenBurns

    return when (kb) {
        KenBurnsPreset.NONE -> 1.0f to 0f
        KenBurnsPreset.SLOW_ZOOM_IN -> (1.0f + 0.14f * progress) to 0f
        KenBurnsPreset.SLOW_ZOOM_OUT -> (1.15f - 0.14f * progress) to 0f
        KenBurnsPreset.PAN_LEFT_RIGHT -> 1.12f to (-0.05f + 0.10f * progress)
        KenBurnsPreset.DRAMATIC_PUSH -> (1.0f + 0.22f * progress * progress) to 0f
    }
}

private fun buildComposeColorMatrix(
    clip: VisualClip,
    themeDefault: ClipColorGrade
): ColorMatrix {
    val grade = if (clip.colorGrade == ClipColorGrade.THEME_DEFAULT) themeDefault else clip.colorGrade
    val matrix = ColorMatrix()
    when (grade) {
        ClipColorGrade.DARK_NOIR -> {
            matrix.setToSaturation(0.45f * clip.saturation)
        }
        ClipColorGrade.COLD_CASE -> {
            matrix.setToSaturation(0.75f * clip.saturation)
            matrix *= ColorMatrix(
                floatArrayOf(
                    0.88f, 0f, 0f, 0f, 0f,
                    0f, 0.96f, 0f, 0f, 0f,
                    0f, 0f, 1.08f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        ClipColorGrade.VINTAGE_ARCHIVE -> {
            matrix.setToSaturation(0.65f * clip.saturation)
            matrix *= ColorMatrix(
                floatArrayOf(
                    1.08f, 0f, 0f, 0f, 0f,
                    0f, 0.96f, 0f, 0f, 0f,
                    0f, 0f, 0.78f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        ClipColorGrade.HIGH_CONTRAST -> {
            matrix.setToSaturation(1.15f * clip.saturation)
        }
        else -> {
            matrix.setToSaturation(clip.saturation)
        }
    }
    return matrix
}
