package com.example.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.BrandKitEntity
import com.example.engine.MagneticTimelineEngine
import com.example.engine.SfxPreset
import com.example.engine.SfxSynthesizer
import com.example.model.ClipColorGrade
import com.example.model.ClipTransition
import com.example.model.DocumentaryTimeline
import com.example.model.EvidenceOverlayType
import com.example.model.KenBurnsPreset
import com.example.model.MotionTextStyle
import com.example.model.SilenceCondenserConfig
import com.example.model.VoiceFitMode
import com.example.ui.theme.DocumentaryAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.EvidenceRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorToolSheet
import com.example.viewmodel.TimelineSelection

@Composable
fun InspectorSheetContainer(
    activeSheet: EditorToolSheet,
    timeline: DocumentaryTimeline,
    selection: TimelineSelection,
    playheadMs: Long,
    brandKits: List<BrandKitEntity>,
    onDismiss: () -> Unit,
    onAddMotionText: (String, String, MotionTextStyle, Long, String?, Long) -> Unit,
    onAddEvidence: (EvidenceOverlayType, String) -> Unit,
    onInsertSfx: (SfxPreset) -> Unit,
    onPreviewSfx: (String) -> Unit,
    onUpdateAudioDuck: (String, Boolean, Float, Float) -> Unit,
    onUpdateSilenceConfig: (SilenceCondenserConfig) -> Unit,
    onToggleSilenceLock: (String, String) -> Unit,
    onUpdateVoiceEnhance: (String, Boolean, Boolean) -> Unit,
    onApplyVoiceFit: (String, Long, VoiceFitMode) -> Unit,
    onUpdateClipKenBurns: (String, KenBurnsPreset) -> Unit,
    onUpdateClipTransition: (String, ClipTransition) -> Unit,
    onUpdateClipColor: (String, ClipColorGrade, Float, Float, Float, Boolean) -> Unit,
    onUpdateGlobalLook: (Float, Float, Boolean, Boolean) -> Unit,
    onAddChapter: (String) -> Unit,
    onApplyBrandKit: (BrandKitEntity) -> Unit
) {
    if (activeSheet == EditorToolSheet.NONE) return

    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 390.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (activeSheet) {
                        EditorToolSheet.MOTION_TEXT_ADD -> "Motion Text Studio (কাইনেটিক টেক্সট)"
                        EditorToolSheet.EVIDENCE_ADD -> "Evidence Callout & Highlighter"
                        EditorToolSheet.SFX_VAULT -> "Documentary SFX Vault & Auto-Duck"
                        EditorToolSheet.SMART_SILENCE -> "Smart Silence Condenser"
                        EditorToolSheet.VOICE_FIT -> "Voice-to-Footage Smart Fit"
                        EditorToolSheet.COLOR_AND_FX -> "Color Grade, Ken Burns & Film FX"
                        EditorToolSheet.CHAPTERS -> "Chapters & Safe Zones"
                        EditorToolSheet.BRAND_KIT_APPLY -> "Apply Client Brand Kit"
                        EditorToolSheet.NONE -> ""
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_inspector_sheet_btn")) {
                    Icon(Icons.Default.Close, contentDescription = "Close Sheet", tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(6.dp))

            when (activeSheet) {
                EditorToolSheet.MOTION_TEXT_ADD -> MotionTextSheetContent(
                    onAdd = { txt, sub, style, colorHex, sfxId ->
                        onAddMotionText(txt, sub, style, colorHex, sfxId, 3800L)
                        onDismiss()
                    }
                )

                EditorToolSheet.EVIDENCE_ADD -> EvidenceSheetContent(
                    onAdd = { type, label ->
                        onAddEvidence(type, label)
                        onDismiss()
                    }
                )

                EditorToolSheet.SFX_VAULT -> SfxVaultSheetContent(
                    timeline = timeline,
                    onPreview = onPreviewSfx,
                    onInsert = onInsertSfx,
                    onUpdateAudioDuck = onUpdateAudioDuck
                )

                EditorToolSheet.SMART_SILENCE -> SmartSilenceSheetContent(
                    timeline = timeline,
                    onUpdateConfig = onUpdateSilenceConfig,
                    onToggleLock = onToggleSilenceLock,
                    onUpdateVoiceEnhance = onUpdateVoiceEnhance
                )

                EditorToolSheet.VOICE_FIT -> VoiceFitSheetContent(
                    timeline = timeline,
                    selection = selection,
                    onApply = { clipId, targetMs, mode ->
                        onApplyVoiceFit(clipId, targetMs, mode)
                        onDismiss()
                    }
                )

                EditorToolSheet.COLOR_AND_FX -> ColorAndFxSheetContent(
                    timeline = timeline,
                    selection = selection,
                    onUpdateKenBurns = onUpdateClipKenBurns,
                    onUpdateTransition = onUpdateClipTransition,
                    onUpdateColor = onUpdateClipColor,
                    onUpdateGlobal = onUpdateGlobalLook
                )

                EditorToolSheet.CHAPTERS -> ChaptersSheetContent(
                    timeline = timeline,
                    playheadMs = playheadMs,
                    onAddChapter = onAddChapter,
                    onUpdateGlobal = onUpdateGlobalLook
                )

                EditorToolSheet.BRAND_KIT_APPLY -> BrandKitApplyContent(
                    brandKits = brandKits,
                    onApply = {
                        onApplyBrandKit(it)
                        onDismiss()
                    }
                )

                EditorToolSheet.NONE -> {}
            }
        }
    }
}

@Composable
private fun MotionTextSheetContent(
    onAdd: (String, String, MotionTextStyle, Long, String?) -> Unit
) {
    var mainText by remember { mutableStateOf("সত্য কখনো চাপা থাকে না") }
    var subText by remember { mutableStateOf("CASE FILE • 1998") }
    var style by remember { mutableStateOf(MotionTextStyle.MARKER_HIGHLIGHT_QUOTE) }
    var accentHex by remember { mutableLongStateOf(0xFFF59E0B) }
    var linkedSfx by remember { mutableStateOf<String?>("typewriter") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(
                value = mainText,
                onValueChange = { mainText = it },
                label = { Text("Primary Text (বাংলা / English)") },
                modifier = Modifier.fillMaxWidth().testTag("motion_text_input")
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = subText,
                onValueChange = { subText = it },
                label = { Text("Subtitle / Source / Date") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Text("Animation Preset (Zero-Keyframe)", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MotionTextStyle.entries) { entry ->
                    val selected = style == entry
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) DocumentaryAmber.copy(alpha = 0.2f) else StudioCard
                        ),
                        border = BorderStroke(1.dp, if (selected) DocumentaryAmber else StudioBorder),
                        modifier = Modifier.clickable { style = entry }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(entry.label, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                            Text(entry.bengaliHint, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Accent:", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                listOf(0xFFF59E0B, 0xFF00D1FF, 0xFFEF4444, 0xFFFFFFFF).forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .clickable { accentHex = hex }
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = linkedSfx != null,
                    onClick = { linkedSfx = if (linkedSfx == null) "typewriter" else null },
                    label = { Text(if (linkedSfx != null) "SFX: On" else "SFX: Off") }
                )
            }
        }
        item {
            Button(
                onClick = { if (mainText.isNotBlank()) onAdd(mainText, subText, style, accentHex, linkedSfx) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = StudioObsidian),
                modifier = Modifier.fillMaxWidth().testTag("insert_motion_text_btn")
            ) {
                Text("Insert Motion Text at Playhead")
            }
        }
    }
}

@Composable
private fun EvidenceSheetContent(onAdd: (EvidenceOverlayType, String) -> Unit) {
    var label by remember { mutableStateOf("CONFIDENTIAL EXHIBIT") }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Stamp / Callout Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(EvidenceOverlayType.entries) { ev ->
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth().clickable { onAdd(ev, label) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(ev.label, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Icon(Icons.Default.Add, contentDescription = "Add ${ev.label}", tint = DocumentaryAmber)
                }
            }
        }
    }
}

@Composable
private fun SfxVaultSheetContent(
    timeline: DocumentaryTimeline,
    onPreview: (String) -> Unit,
    onInsert: (SfxPreset) -> Unit,
    onUpdateAudioDuck: (String, Boolean, Float, Float) -> Unit
) {
    val bgm = timeline.audioSfxItems.firstOrNull { it.isBgm }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (bgm != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioCardElevated),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto-Ducking (${bgm.title})", color = ElectricCyan, style = MaterialTheme.typography.titleSmall)
                            Switch(
                                checked = bgm.autoDuckEnabled,
                                onCheckedChange = { onUpdateAudioDuck(bgm.id, it, bgm.duckLevel, bgm.volume) }
                            )
                        }
                        Text("Duck Level under Voice: ${(bgm.duckLevel * 100).toInt()}%", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = bgm.duckLevel,
                            onValueChange = { onUpdateAudioDuck(bgm.id, bgm.autoDuckEnabled, it, bgm.volume) },
                            valueRange = 0.05f..0.55f
                        )
                    }
                }
            }
        }
        items(SfxSynthesizer.builtInPresets, key = { it.id }) { preset ->
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                border = BorderStroke(1.dp, StudioBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onPreview(preset.id) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Preview ${preset.title}", tint = ElectricCyan)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(preset.title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                        Text("${preset.category} • ${preset.bengaliDesc}", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { onInsert(preset) },
                        border = BorderStroke(1.dp, DocumentaryAmber),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Add", color = DocumentaryAmber)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartSilenceSheetContent(
    timeline: DocumentaryTimeline,
    onUpdateConfig: (SilenceCondenserConfig) -> Unit,
    onToggleLock: (String, String) -> Unit,
    onUpdateVoiceEnhance: (String, Boolean, Boolean) -> Unit
) {
    val cfg = timeline.silenceConfig
    val voice = timeline.voiceTracks.firstOrNull()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCardElevated),
                border = BorderStroke(1.dp, if (cfg.enabled) EmeraldSafe else StudioBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Enable Smart Silence Condenser", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                            Text("কথার মাঝের বাড়তি শ্বাস/ফাঁকা কমায়, বড় সাসপেন্স পজ রক্ষা করে", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(
                            checked = cfg.enabled,
                            onCheckedChange = { onUpdateConfig(cfg.copy(enabled = it)) },
                            modifier = Modifier.testTag("silence_condenser_switch")
                        )
                    }
                    Text(
                        "Protect Dramatic Pauses ≥ ${"%.1f".format(cfg.protectDramaticPausesOverMs / 1000f)}s",
                        color = DocumentaryAmber,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = cfg.protectDramaticPausesOverMs.toFloat(),
                        onValueChange = { onUpdateConfig(cfg.copy(protectDramaticPausesOverMs = it.toLong())) },
                        valueRange = 1500f..6000f
                    )
                }
            }
        }

        if (voice != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Broadcast EQ & -14 LUFS Normalizer", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = voice.broadcastNormalizeEnabled,
                        onCheckedChange = { onUpdateVoiceEnhance(voice.id, voice.noiseReductionEnabled, it) }
                    )
                }
            }

            if (voice.silenceGaps.isEmpty()) {
                item {
                    Text("No long silence gaps detected in this voice track.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                item {
                    Text("Detected Pauses (Tap lock icon to protect entry pause):", color = ElectricCyan, style = MaterialTheme.typography.labelMedium)
                }
                items(voice.silenceGaps, key = { it.id }) { gap ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioCard),
                        border = BorderStroke(1.dp, if (gap.isProtected) DocumentaryAmber else StudioBorder),
                        modifier = Modifier.fillMaxWidth().clickable { onToggleLock(voice.id, gap.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "At ${MagneticTimelineEngine.formatTimecode(gap.startMs)} • ${"%.1f".format(gap.durationMs / 1000f)}s gap",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = when {
                                        gap.isProtected -> "Protected (Dramatic / Entry Pause kept)"
                                        gap.isTrimmed -> "Trimmed to ${cfg.keepBreathingGapMs}ms"
                                        else -> "Speech gap candidate"
                                    },
                                    color = if (gap.isProtected) DocumentaryAmber else TextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Icon(
                                imageVector = if (gap.isProtected) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Toggle Pause Lock",
                                tint = if (gap.isProtected) DocumentaryAmber else TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceFitSheetContent(
    timeline: DocumentaryTimeline,
    selection: TimelineSelection,
    onApply: (String, Long, VoiceFitMode) -> Unit
) {
    val clip = (selection as? TimelineSelection.Clip)?.let { sel ->
        timeline.visualClips.find { it.id == sel.clipId }
    } ?: timeline.visualClips.firstOrNull()

    if (clip == null) {
        Text("Select a video or image clip on the timeline first.", color = TextSecondary)
        return
    }

    var targetSeconds by remember(clip.id) {
        mutableFloatStateOf((clip.durationMs / 1000f).coerceAtLeast(2f) + 2f)
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                text = "Clip: ${clip.title} (Original: ${"%.1f".format(clip.originalDurationMs / 1000f)}s)",
                color = ElectricCyan,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Target Voice Segment Duration: ${"%.1f".format(targetSeconds)}s",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = targetSeconds,
                onValueChange = { targetSeconds = it },
                valueRange = 2f..30f
            )
        }
        items(
            listOf(
                VoiceFitMode.SMOOTH_SLOW_MO to "উদাহরণ: ৮ সেকেন্ডের ক্লিপ স্মুদ স্লো-মো হয়ে ১০ সেকেন্ড পূরণ করবে",
                VoiceFitMode.FREEZE_KEN_BURNS to "৮ সেকেন্ড পর শেষ ফ্রেম ধরে রেখে সূক্ষ্ম জুম-ইন হবে",
                VoiceFitMode.SEAMLESS_LOOP to "অ্যাম্বিয়েন্ট ফুটেজ (বৃষ্টি/শহর) স্মুদ লুপ হয়ে সময় মেলাবে"
            )
        ) { (mode, desc) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onApply(clip.id, (targetSeconds * 1000f).toLong(), mode) }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(mode.label, color = DocumentaryAmber, style = MaterialTheme.typography.titleSmall)
                    Text(desc, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ColorAndFxSheetContent(
    timeline: DocumentaryTimeline,
    selection: TimelineSelection,
    onUpdateKenBurns: (String, KenBurnsPreset) -> Unit,
    onUpdateTransition: (String, ClipTransition) -> Unit,
    onUpdateColor: (String, ClipColorGrade, Float, Float, Float, Boolean) -> Unit,
    onUpdateGlobal: (Float, Float, Boolean, Boolean) -> Unit
) {
    val clip = (selection as? TimelineSelection.Clip)?.let { sel ->
        timeline.visualClips.find { it.id == sel.clipId }
    } ?: timeline.visualClips.firstOrNull()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Global Vignette: ${(timeline.vignetteIntensity * 100).toInt()}%", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
            Slider(
                value = timeline.vignetteIntensity,
                onValueChange = { onUpdateGlobal(it, timeline.filmGrainIntensity, timeline.safeZoneEnabled, timeline.showDraftWatermark) },
                valueRange = 0f..1f
            )
            Text("Film Grain Texture: ${(timeline.filmGrainIntensity * 100).toInt()}%", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
            Slider(
                value = timeline.filmGrainIntensity,
                onValueChange = { onUpdateGlobal(timeline.vignetteIntensity, it, timeline.safeZoneEnabled, timeline.showDraftWatermark) },
                valueRange = 0f..1f
            )
        }

        if (clip != null) {
            item {
                Text("Ken Burns Motion (${clip.title})", color = ElectricCyan, style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(KenBurnsPreset.entries) { kb ->
                        FilterChip(
                            selected = clip.kenBurns == kb,
                            onClick = { onUpdateKenBurns(clip.id, kb) },
                            label = { Text(kb.label) }
                        )
                    }
                }
            }
            item {
                Text("Transition In", color = DocumentaryAmber, style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ClipTransition.entries) { tr ->
                        FilterChip(
                            selected = clip.transitionIn == tr,
                            onClick = { onUpdateTransition(clip.id, tr) },
                            label = { Text(tr.label) }
                        )
                    }
                }
            }
            item {
                Text("Color Grade (Tap Apply All for full video)", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ClipColorGrade.entries) { grade ->
                        FilterChip(
                            selected = clip.colorGrade == grade,
                            onClick = { onUpdateColor(clip.id, grade, clip.brightness, clip.contrast, clip.saturation, false) },
                            label = { Text(grade.label) }
                        )
                    }
                }
                OutlinedButton(
                    onClick = { onUpdateColor(clip.id, clip.colorGrade, clip.brightness, clip.contrast, clip.saturation, true) },
                    border = BorderStroke(1.dp, ElectricCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply This Color Grade to All Clips", color = ElectricCyan)
                }
            }
        }
    }
}

@Composable
private fun ChaptersSheetContent(
    timeline: DocumentaryTimeline,
    playheadMs: Long,
    onAddChapter: (String) -> Unit,
    onUpdateGlobal: (Float, Float, Boolean, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Chapter Title at ${MagneticTimelineEngine.formatTimecode(playheadMs)}") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        onAddChapter(title)
                        title = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = StudioObsidian)
                ) {
                    Text("Add")
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("YouTube / Shorts Safe Zone Grid", color = TextPrimary)
                Switch(
                    checked = timeline.safeZoneEnabled,
                    onCheckedChange = { onUpdateGlobal(timeline.vignetteIntensity, timeline.filmGrainIntensity, it, timeline.showDraftWatermark) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Client Draft Review Watermark & Timecode", color = DocumentaryAmber)
                Switch(
                    checked = timeline.showDraftWatermark,
                    onCheckedChange = { onUpdateGlobal(timeline.vignetteIntensity, timeline.filmGrainIntensity, timeline.safeZoneEnabled, it) }
                )
            }
        }
    }
}

@Composable
private fun BrandKitApplyContent(
    brandKits: List<BrandKitEntity>,
    onApply: (BrandKitEntity) -> Unit
) {
    if (brandKits.isEmpty()) {
        Text("No Client Brand Kits saved yet. Create one from the Home screen.", color = TextSecondary)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(brandKits, key = { it.id }) { kit ->
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth().clickable { onApply(kit) }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(kit.clientName, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Text("Watermark: ${kit.watermarkText} • ${kit.channelTag}", color = ElectricCyan, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
