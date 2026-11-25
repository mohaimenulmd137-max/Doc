package com.example.viewmodel

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BrandKitEntity
import com.example.data.DocuCutDatabase
import com.example.data.DocuCutRepository
import com.example.data.ExportJobEntity
import com.example.data.ProjectEntity
import com.example.data.SampleDocumentaryFactory
import com.example.engine.MagneticTimelineEngine
import com.example.engine.SfxPreset
import com.example.engine.SfxSynthesizer
import com.example.engine.SilenceDetectorEngine
import com.example.model.AspectRatioPreset
import com.example.model.AudioSfxItem
import com.example.model.ChapterMarker
import com.example.model.ClipColorGrade
import com.example.model.ClipTransition
import com.example.model.DocumentaryTheme
import com.example.model.DocumentaryTimeline
import com.example.model.EvidenceOverlay
import com.example.model.EvidenceOverlayType
import com.example.model.KenBurnsPreset
import com.example.model.MotionTextOverlay
import com.example.model.MotionTextStyle
import com.example.model.SilenceCondenserConfig
import com.example.model.VisualClip
import com.example.model.VoiceFitMode
import com.example.model.VoiceTrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class TimelineSelection {
    data object None : TimelineSelection()
    data class Clip(val clipId: String) : TimelineSelection()
    data class Text(val textId: String) : TimelineSelection()
    data class Evidence(val evidenceId: String) : TimelineSelection()
    data class Voice(val voiceId: String) : TimelineSelection()
    data class AudioSfx(val audioId: String) : TimelineSelection()
}

enum class EditorToolSheet {
    NONE,
    MOTION_TEXT_ADD,
    EVIDENCE_ADD,
    SFX_VAULT,
    SMART_SILENCE,
    VOICE_FIT,
    COLOR_AND_FX,
    CHAPTERS,
    BRAND_KIT_APPLY
}

data class EditorUiState(
    val projectId: String = "",
    val projectTitle: String = "Untitled Documentary",
    val clientName: String = "",
    val timeline: DocumentaryTimeline = DocumentaryTimeline(),
    val playheadMs: Long = 0L,
    val isPlaying: Boolean = false,
    val pixelsPerSecond: Float = 42f,
    val selection: TimelineSelection = TimelineSelection.None,
    val activeSheet: EditorToolSheet = EditorToolSheet.NONE,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val statusBanner: String? = null
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DocuCutDatabase.getInstance(application)
    val repository = DocuCutRepository(db)

    val projects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val brandKits: StateFlow<List<BrandKitEntity>> = repository.allBrandKits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exportJobs: StateFlow<List<ExportJobEntity>> = repository.exportJobs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editorState = MutableStateFlow(EditorUiState())
    val editorState: StateFlow<EditorUiState> = _editorState.asStateFlow()

    private val undoStack = ArrayDeque<DocumentaryTimeline>()
    private val redoStack = ArrayDeque<DocumentaryTimeline>()
    private var playbackJob: Job? = null
    private var saveJob: Job? = null

    fun createEmptyProject(
        title: String,
        theme: DocumentaryTheme,
        aspectRatio: AspectRatioPreset,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val timeline = DocumentaryTimeline(
                theme = theme,
                aspectRatio = aspectRatio,
                vignetteIntensity = theme.defaultVignette,
                filmGrainIntensity = theme.defaultGrain,
                chapters = listOf(ChapterMarker(title = "01 • Intro", startMs = 0L))
            )
            repository.saveTimeline(id, title.ifBlank { "Untitled Documentary" }, "", timeline)
            onCreated(id)
        }
    }

    fun loadSampleCaseProject(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val id = SampleDocumentaryFactory.createSampleCaseProject(getApplication(), repository)
            onReady(id)
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch { repository.deleteProject(id) }
    }

    fun saveBrandKit(kit: BrandKitEntity) {
        viewModelScope.launch { repository.saveBrandKit(kit) }
    }

    fun deleteBrandKit(id: String) {
        viewModelScope.launch { repository.deleteBrandKit(id) }
    }

    fun openProject(projectId: String) {
        viewModelScope.launch {
            val entity = repository.getProject(projectId) ?: return@launch
            val timeline = repository.decodeTimeline(entity.timelineJson)
            undoStack.clear()
            redoStack.clear()
            _editorState.value = EditorUiState(
                projectId = entity.id,
                projectTitle = entity.title,
                clientName = entity.clientName,
                timeline = timeline,
                playheadMs = 0L,
                isPlaying = false,
                selection = TimelineSelection.None
            )
        }
    }

    fun createFromAutoDocWizard(
        title: String,
        clientName: String,
        theme: DocumentaryTheme,
        aspectRatio: AspectRatioPreset,
        visualUris: List<Uri>,
        voiceUris: List<Uri>,
        bgmPresetId: String?,
        autoKenBurns: Boolean,
        autoDucking: Boolean,
        autoFitMode: VoiceFitMode,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val visualClips = withContext(Dispatchers.IO) {
                visualUris.map { uri -> probeVisualUri(app, uri) }
            }
            val voiceTracks = withContext(Dispatchers.IO) {
                voiceUris.map { uri ->
                    val dur = SilenceDetectorEngine.probeAudioDurationMs(app, uri)
                    val item = VoiceTrackItem(
                        uriString = uri.toString(),
                        title = queryDisplayName(app, uri) ?: "Narration",
                        startMs = 0L,
                        durationMs = dur,
                        originalDurationMs = dur
                    )
                    SilenceDetectorEngine.analyzeVoiceTrack(app, item)
                }
            }
            val bgmItem = if (bgmPresetId != null) {
                val wav = SfxSynthesizer.ensurePresetWavFile(app, bgmPresetId)
                val preset = SfxSynthesizer.builtInPresets.find { it.id == bgmPresetId }
                AudioSfxItem(
                    title = preset?.title ?: "Documentary BGM",
                    uriString = Uri.fromFile(wav).toString(),
                    sfxPresetId = bgmPresetId,
                    isBgm = true,
                    startMs = 0L,
                    durationMs = 60_000L,
                    autoDuckEnabled = autoDucking,
                    duckLevel = theme.defaultDuckLevel
                )
            } else {
                null
            }

            val timeline = MagneticTimelineEngine.autoAssembleDocumentary(
                theme = theme,
                visualInputs = visualClips,
                voiceTracks = voiceTracks,
                bgmItem = bgmItem,
                autoKenBurns = autoKenBurns,
                autoDucking = autoDucking,
                autoFitToVoice = autoFitMode,
                projectTitle = title.ifBlank { "Investigative Story" }
            ).copy(aspectRatio = aspectRatio)

            val id = UUID.randomUUID().toString()
            repository.saveTimeline(id, title.ifBlank { "Investigative Story" }, clientName, timeline)
            onCreated(id)
        }
    }

    fun addVisualMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val app = getApplication<Application>()
            val newClips = withContext(Dispatchers.IO) {
                uris.map { probeVisualUri(app, it) }
            }
            mutateTimeline { current ->
                val merged = MagneticTimelineEngine.repackMagneticTrack(current.visualClips + newClips)
                current.copy(visualClips = merged)
            }
            showBanner("Added ${newClips.size} clip(s) to Magnetic Track")
        }
    }

    fun addVoiceoverUri(uri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val analyzed = withContext(Dispatchers.IO) {
                val dur = SilenceDetectorEngine.probeAudioDurationMs(app, uri)
                val startMs = _editorState.value.timeline.voiceTracks.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
                val raw = VoiceTrackItem(
                    uriString = uri.toString(),
                    title = queryDisplayName(app, uri) ?: "Voiceover",
                    startMs = startMs,
                    durationMs = dur,
                    originalDurationMs = dur
                )
                SilenceDetectorEngine.analyzeVoiceTrack(app, raw)
            }
            mutateTimeline { current ->
                current.copy(voiceTracks = current.voiceTracks + analyzed)
            }
            showBanner("Voiceover analyzed for Smart Silence & Auto-Ducking")
        }
    }

    fun togglePlayback() {
        val current = _editorState.value
        if (current.isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        playbackJob?.cancel()
        val total = _editorState.value.timeline.totalDurationMs
        if (_editorState.value.playheadMs >= total) {
            _editorState.value = _editorState.value.copy(playheadMs = 0L)
        }
        _editorState.value = _editorState.value.copy(isPlaying = true)
        playbackJob = viewModelScope.launch {
            while (_editorState.value.isPlaying) {
                delay(40L)
                val next = _editorState.value.playheadMs + 40L
                val maxDur = _editorState.value.timeline.totalDurationMs
                if (next >= maxDur) {
                    _editorState.value = _editorState.value.copy(playheadMs = maxDur, isPlaying = false)
                } else {
                    _editorState.value = _editorState.value.copy(playheadMs = next)
                }
            }
        }
    }

    fun pausePlayback() {
        playbackJob?.cancel()
        _editorState.value = _editorState.value.copy(isPlaying = false)
    }

    fun seekTo(ms: Long) {
        val maxDur = _editorState.value.timeline.totalDurationMs
        _editorState.value = _editorState.value.copy(playheadMs = ms.coerceIn(0L, maxDur))
    }

    fun setTimelineZoom(pixelsPerSec: Float) {
        _editorState.value = _editorState.value.copy(
            pixelsPerSecond = pixelsPerSec.coerceIn(14f, 120f)
        )
    }

    fun selectItem(selection: TimelineSelection) {
        _editorState.value = _editorState.value.copy(selection = selection)
    }

    fun openSheet(sheet: EditorToolSheet) {
        _editorState.value = _editorState.value.copy(activeSheet = sheet)
    }

    fun resizeSelectedClip(clipId: String, newDurationMs: Long) {
        mutateTimeline { current ->
            current.copy(
                visualClips = MagneticTimelineEngine.resizeClipWithRipple(
                    clips = current.visualClips,
                    clipId = clipId,
                    newDurationMs = newDurationMs
                )
            )
        }
    }

    fun applyVoiceFitToClip(clipId: String, targetDurationMs: Long, mode: VoiceFitMode) {
        mutateTimeline { current ->
            current.copy(
                visualClips = MagneticTimelineEngine.applyVoiceFit(
                    clips = current.visualClips,
                    clipId = clipId,
                    targetDurationMs = targetDurationMs,
                    mode = mode
                )
            )
        }
        showBanner("Applied ${mode.label} (${MagneticTimelineEngine.formatTimecode(targetDurationMs)})")
    }

    fun splitSelectedClipAtPlayhead() {
        val sel = _editorState.value.selection as? TimelineSelection.Clip ?: return
        val pos = _editorState.value.playheadMs
        mutateTimeline { current ->
            current.copy(
                visualClips = MagneticTimelineEngine.splitClipAt(current.visualClips, sel.clipId, pos)
            )
        }
    }

    fun duplicateSelectedClip() {
        val sel = _editorState.value.selection as? TimelineSelection.Clip ?: return
        mutateTimeline { current ->
            current.copy(
                visualClips = MagneticTimelineEngine.duplicateClip(current.visualClips, sel.clipId)
            )
        }
    }

    fun deleteSelectedItem() {
        val sel = _editorState.value.selection
        mutateTimeline { current ->
            when (sel) {
                is TimelineSelection.Clip -> current.copy(
                    visualClips = MagneticTimelineEngine.deleteClipWithRipple(current.visualClips, sel.clipId)
                )
                is TimelineSelection.Text -> current.copy(
                    motionTexts = current.motionTexts.filterNot { it.id == sel.textId }
                )
                is TimelineSelection.Evidence -> current.copy(
                    evidenceOverlays = current.evidenceOverlays.filterNot { it.id == sel.evidenceId }
                )
                is TimelineSelection.Voice -> current.copy(
                    voiceTracks = current.voiceTracks.filterNot { it.id == sel.voiceId }
                )
                is TimelineSelection.AudioSfx -> current.copy(
                    audioSfxItems = current.audioSfxItems.filterNot { it.id == sel.audioId }
                )
                TimelineSelection.None -> current
            }
        }
        _editorState.value = _editorState.value.copy(selection = TimelineSelection.None)
    }

    fun updateClipKenBurns(clipId: String, kb: KenBurnsPreset) {
        mutateTimeline { current ->
            current.copy(
                visualClips = current.visualClips.map {
                    if (it.id == clipId) it.copy(kenBurns = kb) else it
                }
            )
        }
    }

    fun updateClipTransition(clipId: String, tr: ClipTransition) {
        mutateTimeline { current ->
            current.copy(
                visualClips = current.visualClips.map {
                    if (it.id == clipId) it.copy(transitionIn = tr) else it
                }
            )
        }
    }

    fun updateClipColor(
        clipId: String,
        grade: ClipColorGrade,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        applyToAll: Boolean
    ) {
        mutateTimeline { current ->
            val updated = if (applyToAll) {
                MagneticTimelineEngine.applyColorToAll(
                    current.visualClips, grade, brightness, contrast, saturation
                )
            } else {
                current.visualClips.map {
                    if (it.id == clipId) {
                        it.copy(
                            colorGrade = grade,
                            brightness = brightness,
                            contrast = contrast,
                            saturation = saturation
                        )
                    } else it
                }
            }
            current.copy(visualClips = updated)
        }
    }

    fun updateGlobalLook(
        theme: DocumentaryTheme? = null,
        vignette: Float? = null,
        grain: Float? = null,
        aspectRatio: AspectRatioPreset? = null,
        safeZone: Boolean? = null,
        draftWatermark: Boolean? = null
    ) {
        mutateTimeline { current ->
            current.copy(
                theme = theme ?: current.theme,
                vignetteIntensity = vignette ?: current.vignetteIntensity,
                filmGrainIntensity = grain ?: current.filmGrainIntensity,
                aspectRatio = aspectRatio ?: current.aspectRatio,
                safeZoneEnabled = safeZone ?: current.safeZoneEnabled,
                showDraftWatermark = draftWatermark ?: current.showDraftWatermark
            )
        }
    }

    fun addMotionText(
        text: String,
        subText: String,
        style: MotionTextStyle,
        accentHex: Long,
        linkedSfxId: String?,
        durationMs: Long = 3800L
    ) {
        val playhead = _editorState.value.playheadMs
        val newOverlay = MotionTextOverlay(
            text = text,
            subText = subText,
            style = style,
            startMs = playhead,
            durationMs = durationMs,
            accentColorHex = accentHex,
            linkedSfxId = linkedSfxId,
            yPositionFraction = when (style) {
                MotionTextStyle.IMPACT_CHAPTER_TITLE -> 0.50f
                MotionTextStyle.SOURCE_CITATION -> 0.14f
                else -> 0.78f
            }
        )
        mutateTimeline { current ->
            val extraSfx = if (linkedSfxId != null) {
                val preset = SfxSynthesizer.builtInPresets.find { it.id == linkedSfxId }
                if (preset != null) {
                    listOf(
                        AudioSfxItem(
                            title = preset.title,
                            uriString = "",
                            sfxPresetId = preset.id,
                            isBgm = false,
                            startMs = playhead,
                            durationMs = preset.durationMs,
                            volume = 0.85f
                        )
                    )
                } else emptyList()
            } else emptyList()

            current.copy(
                motionTexts = current.motionTexts + newOverlay,
                audioSfxItems = current.audioSfxItems + extraSfx
            )
        }
        if (linkedSfxId != null) {
            viewModelScope.launch {
                val wav = SfxSynthesizer.ensurePresetWavFile(getApplication(), linkedSfxId)
                mutateTimeline(recordUndo = false) { current ->
                    current.copy(
                        audioSfxItems = current.audioSfxItems.map {
                            if (it.sfxPresetId == linkedSfxId && it.uriString.isBlank()) {
                                it.copy(uriString = Uri.fromFile(wav).toString())
                            } else it
                        }
                    )
                }
            }
        }
    }

    fun resizeMotionText(textId: String, newDurationMs: Long) {
        mutateTimeline { current ->
            current.copy(
                motionTexts = current.motionTexts.map {
                    if (it.id == textId) it.copy(durationMs = newDurationMs.coerceIn(1000L, 30_000L)) else it
                }
            )
        }
    }

    fun addEvidenceOverlay(type: EvidenceOverlayType, label: String) {
        val playhead = _editorState.value.playheadMs
        mutateTimeline { current ->
            current.copy(
                evidenceOverlays = current.evidenceOverlays + EvidenceOverlay(
                    type = type,
                    label = label,
                    startMs = playhead,
                    durationMs = 3500L
                )
            )
        }
    }

    fun insertSfxPreset(preset: SfxPreset) {
        val playhead = _editorState.value.playheadMs
        viewModelScope.launch {
            val wav = SfxSynthesizer.ensurePresetWavFile(getApplication(), preset.id)
            SfxSynthesizer.previewPreset(getApplication(), preset.id)
            mutateTimeline { current ->
                val item = AudioSfxItem(
                    title = preset.title,
                    uriString = Uri.fromFile(wav).toString(),
                    sfxPresetId = preset.id,
                    isBgm = preset.isBgm,
                    startMs = if (preset.isBgm) 0L else playhead,
                    durationMs = if (preset.isBgm) current.totalDurationMs else preset.durationMs,
                    autoDuckEnabled = preset.isBgm,
                    duckLevel = current.theme.defaultDuckLevel
                )
                current.copy(audioSfxItems = current.audioSfxItems + item)
            }
            showBanner("Added ${preset.title} at ${MagneticTimelineEngine.formatTimecode(playhead)}")
        }
    }

    fun previewSfx(presetId: String) {
        viewModelScope.launch {
            SfxSynthesizer.previewPreset(getApplication(), presetId)
        }
    }

    fun updateAudioAutoDuck(audioId: String, enabled: Boolean, duckLevel: Float, volume: Float) {
        mutateTimeline { current ->
            current.copy(
                audioSfxItems = current.audioSfxItems.map {
                    if (it.id == audioId) {
                        it.copy(autoDuckEnabled = enabled, duckLevel = duckLevel, volume = volume)
                    } else it
                }
            )
        }
    }

    fun updateSilenceConfig(config: SilenceCondenserConfig) {
        mutateTimeline { current ->
            val updatedVoice = current.voiceTracks.map { vt ->
                SilenceDetectorEngine.applySilenceCondenser(vt, config)
            }
            current.copy(
                silenceConfig = config,
                voiceTracks = updatedVoice
            )
        }
    }

    fun toggleSilenceGapProtection(voiceId: String, gapId: String) {
        mutateTimeline { current ->
            val updated = current.voiceTracks.map { vt ->
                if (vt.id == voiceId) {
                    val toggled = SilenceDetectorEngine.toggleGapProtection(vt, gapId)
                    SilenceDetectorEngine.applySilenceCondenser(toggled, current.silenceConfig)
                } else vt
            }
            current.copy(voiceTracks = updated)
        }
    }

    fun updateVoiceEnhancement(voiceId: String, noiseReduction: Boolean, broadcastNorm: Boolean) {
        mutateTimeline { current ->
            current.copy(
                voiceTracks = current.voiceTracks.map {
                    if (it.id == voiceId) {
                        it.copy(
                            noiseReductionEnabled = noiseReduction,
                            broadcastNormalizeEnabled = broadcastNorm
                        )
                    } else it
                }
            )
        }
    }

    fun addChapterAtPlayhead(title: String) {
        val pos = _editorState.value.playheadMs
        mutateTimeline { current ->
            val next = (current.chapters + ChapterMarker(title = title.ifBlank { "Chapter" }, startMs = pos))
                .sortedBy { it.startMs }
            current.copy(chapters = next)
        }
    }

    fun applyBrandKit(kit: BrandKitEntity) {
        mutateTimeline { current ->
            val ratio = runCatching { AspectRatioPreset.valueOf(kit.aspectRatioName) }
                .getOrDefault(current.aspectRatio)
            current.copy(
                brandKitId = kit.id,
                aspectRatio = ratio,
                safeZoneEnabled = kit.safeZoneEnabled
            )
        }
        _editorState.value = _editorState.value.copy(clientName = kit.clientName)
        showBanner("Applied Brand Kit: ${kit.clientName}")
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val prev = undoStack.removeLast()
        redoStack.addLast(_editorState.value.timeline)
        applyTimelineSnapshot(prev)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeLast()
        undoStack.addLast(_editorState.value.timeline)
        applyTimelineSnapshot(next)
    }

    private fun mutateTimeline(
        recordUndo: Boolean = true,
        transform: (DocumentaryTimeline) -> DocumentaryTimeline
    ) {
        val current = _editorState.value.timeline
        if (recordUndo) {
            undoStack.addLast(current)
            if (undoStack.size > 30) undoStack.removeFirst()
            redoStack.clear()
        }
        val updated = transform(current)
        applyTimelineSnapshot(updated)
    }

    private fun applyTimelineSnapshot(timeline: DocumentaryTimeline) {
        _editorState.value = _editorState.value.copy(
            timeline = timeline,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        saveJob?.cancel()
        val snap = _editorState.value
        if (snap.projectId.isBlank()) return
        saveJob = viewModelScope.launch {
            delay(350L)
            repository.saveTimeline(
                projectId = snap.projectId,
                title = snap.projectTitle,
                clientName = snap.clientName,
                timeline = snap.timeline
            )
        }
    }

    private fun showBanner(message: String) {
        _editorState.value = _editorState.value.copy(statusBanner = message)
        viewModelScope.launch {
            delay(2600L)
            if (_editorState.value.statusBanner == message) {
                _editorState.value = _editorState.value.copy(statusBanner = null)
            }
        }
    }

    private fun probeVisualUri(app: Application, uri: Uri): VisualClip {
        val mime = app.contentResolver.getType(uri) ?: ""
        val isImage = mime.startsWith("image/")
        val name = queryDisplayName(app, uri) ?: if (isImage) "Evidence Photo" else "B-Roll Clip"
        val durationMs = if (isImage) {
            4500L
        } else {
            runCatching {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(app, uri)
                val d = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 8000L
                mmr.release()
                d.coerceAtLeast(1500L)
            }.getOrElse { 8000L }
        }
        return VisualClip(
            uriString = uri.toString(),
            title = name,
            isImage = isImage,
            originalDurationMs = durationMs,
            startMs = 0L,
            durationMs = durationMs,
            kenBurns = if (isImage) KenBurnsPreset.SLOW_ZOOM_IN else KenBurnsPreset.NONE
        )
    }

    private fun queryDisplayName(app: Application, uri: Uri): String? {
        return runCatching {
            app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0)?.substringBeforeLast('.') else null
            }
        }.getOrNull()
    }

    override fun onCleared() {
        SfxSynthesizer.stopPreview()
        super.onCleared()
    }
}
