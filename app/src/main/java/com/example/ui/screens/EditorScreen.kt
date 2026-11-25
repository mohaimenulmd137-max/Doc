package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.AspectRatioPreset
import com.example.ui.editor.BottomToolbar
import com.example.ui.editor.InspectorSheetContainer
import com.example.ui.editor.PreviewViewport
import com.example.ui.editor.TimelinePanel
import com.example.ui.theme.DocumentaryAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorToolSheet
import com.example.viewmodel.StudioViewModel
import com.example.viewmodel.TimelineSelection

@Composable
fun EditorScreen(
    projectId: String,
    viewModel: StudioViewModel,
    onBack: () -> Unit,
    onOpenExport: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.editorState.collectAsStateWithLifecycle()
    val brandKits by viewModel.brandKits.collectAsStateWithLifecycle()
    var isFullscreenPreview by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        if (uiState.projectId != projectId) {
            viewModel.openProject(projectId)
        }
    }

    val activeWatermark = remember(uiState.timeline.brandKitId, brandKits) {
        brandKits.firstOrNull { it.id == uiState.timeline.brandKitId }?.watermarkText
    }

    val visualPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addVisualMedia(uris)
        }
    }

    val voicePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.addVoiceoverUri(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidian)
            .statusBarsPadding()
    ) {
        // CapCut Top Studio Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = {
                        viewModel.pausePlayback()
                        onBack()
                    },
                    modifier = Modifier.testTag("editor_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Hub", tint = TextPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.projectTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${uiState.timeline.theme.displayName} • ${uiState.timeline.aspectRatio.label.substringBefore(" ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = StudioCard,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.clickable {
                        val next = when (uiState.timeline.aspectRatio) {
                            AspectRatioPreset.RATIO_16_9 -> AspectRatioPreset.RATIO_9_16
                            AspectRatioPreset.RATIO_9_16 -> AspectRatioPreset.RATIO_1_1
                            AspectRatioPreset.RATIO_1_1 -> AspectRatioPreset.RATIO_16_9
                        }
                        viewModel.updateGlobalLook(aspectRatio = next)
                    }
                ) {
                    Text(
                        text = uiState.timeline.aspectRatio.label.substringBefore(" "),
                        style = MaterialTheme.typography.labelMedium,
                        color = DocumentaryAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                Button(
                    onClick = {
                        viewModel.pausePlayback()
                        onOpenExport(projectId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = StudioObsidian
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("editor_export_btn")
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.export_video), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Optional Status Toast Banner
        AnimatedVisibility(visible = uiState.statusBanner != null) {
            uiState.statusBanner?.let { msg ->
                Surface(
                    color = ElectricCyan.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = msg,
                        color = ElectricCyan,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Top Half: Preview Viewport
        PreviewViewport(
            timeline = uiState.timeline,
            playheadMs = uiState.playheadMs,
            isPlaying = uiState.isPlaying,
            canUndo = uiState.canUndo,
            canRedo = uiState.canRedo,
            brandWatermark = activeWatermark,
            onTogglePlay = { viewModel.togglePlayback() },
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onJumpToChapter = { viewModel.seekTo(it) },
            onToggleFullscreen = { isFullscreenPreview = !isFullscreenPreview },
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (isFullscreenPreview) 1f else 0.46f)
        )

        // Bottom Half: Magnetic Timeline + Inspector Sheet + Contextual CapCut Toolbar
        if (!isFullscreenPreview) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.54f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TimelinePanel(
                        timeline = uiState.timeline,
                        playheadMs = uiState.playheadMs,
                        isPlaying = uiState.isPlaying,
                        pixelsPerSecond = uiState.pixelsPerSecond,
                        selection = uiState.selection,
                        onSeek = { viewModel.seekTo(it) },
                        onZoomChange = { viewModel.setTimelineZoom(it) },
                        onSelect = { viewModel.selectItem(it) },
                        onResizeClip = { clipId, newDur -> viewModel.resizeSelectedClip(clipId, newDur) },
                        onResizeText = { textId, newDur -> viewModel.resizeMotionText(textId, newDur) },
                        onAddVisualClick = {
                            visualPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        onAddVoiceClick = { voicePicker.launch(arrayOf("audio/*")) },
                        modifier = Modifier.weight(1f)
                    )

                    BottomToolbar(
                        selection = uiState.selection,
                        onDeselect = { viewModel.selectItem(TimelineSelection.None) },
                        onOpenSheet = { viewModel.openSheet(it) },
                        onSplitClip = { viewModel.splitSelectedClipAtPlayhead() },
                        onDuplicateClip = { viewModel.duplicateSelectedClip() },
                        onDeleteSelected = { viewModel.deleteSelectedItem() },
                        onCycleAspectRatio = {
                            val next = when (uiState.timeline.aspectRatio) {
                                AspectRatioPreset.RATIO_16_9 -> AspectRatioPreset.RATIO_9_16
                                AspectRatioPreset.RATIO_9_16 -> AspectRatioPreset.RATIO_1_1
                                AspectRatioPreset.RATIO_1_1 -> AspectRatioPreset.RATIO_16_9
                            }
                            viewModel.updateGlobalLook(aspectRatio = next)
                        },
                        onAddVoiceover = { voicePicker.launch(arrayOf("audio/*")) }
                    )
                }

                // Expandable Studio Inspector Sheet Overlay
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    InspectorSheetContainer(
                        activeSheet = uiState.activeSheet,
                        timeline = uiState.timeline,
                        selection = uiState.selection,
                        playheadMs = uiState.playheadMs,
                        brandKits = brandKits,
                        onDismiss = { viewModel.openSheet(EditorToolSheet.NONE) },
                        onAddMotionText = { t, s, style, hex, sfx, dur ->
                            viewModel.addMotionText(t, s, style, hex, sfx, dur)
                        },
                        onAddEvidence = { type, label -> viewModel.addEvidenceOverlay(type, label) },
                        onInsertSfx = { viewModel.insertSfxPreset(it) },
                        onPreviewSfx = { viewModel.previewSfx(it) },
                        onUpdateAudioDuck = { id, en, duck, vol ->
                            viewModel.updateAudioAutoDuck(id, en, duck, vol)
                        },
                        onUpdateSilenceConfig = { viewModel.updateSilenceConfig(it) },
                        onToggleSilenceLock = { vId, gId -> viewModel.toggleSilenceGapProtection(vId, gId) },
                        onUpdateVoiceEnhance = { vId, nr, norm ->
                            viewModel.updateVoiceEnhancement(vId, nr, norm)
                        },
                        onApplyVoiceFit = { cId, target, mode ->
                            viewModel.applyVoiceFitToClip(cId, target, mode)
                        },
                        onUpdateClipKenBurns = { cId, kb -> viewModel.updateClipKenBurns(cId, kb) },
                        onUpdateClipTransition = { cId, tr -> viewModel.updateClipTransition(cId, tr) },
                        onUpdateClipColor = { cId, gr, br, co, sa, all ->
                            viewModel.updateClipColor(cId, gr, br, co, sa, all)
                        },
                        onUpdateGlobalLook = { vig, gr, safe, draft ->
                            viewModel.updateGlobalLook(vignette = vig, grain = gr, safeZone = safe, draftWatermark = draft)
                        },
                        onAddChapter = { viewModel.addChapterAtPlayhead(it) },
                        onApplyBrandKit = { viewModel.applyBrandKit(it) }
                    )
                }
            }
        }
    }
}
