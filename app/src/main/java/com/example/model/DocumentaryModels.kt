package com.example.model

import com.squareup.moshi.JsonClass
import java.util.UUID

enum class DocumentaryTheme(
    val displayName: String,
    val bengaliTag: String,
    val defaultGrade: ClipColorGrade,
    val defaultVignette: Float,
    val defaultGrain: Float,
    val defaultTransition: ClipTransition,
    val defaultDuckLevel: Float
) {
    CRIME_NOIR(
        displayName = "Crime Noir",
        bengaliTag = "অপরাধ ও রহস্য",
        defaultGrade = ClipColorGrade.DARK_NOIR,
        defaultVignette = 0.55f,
        defaultGrain = 0.25f,
        defaultTransition = ClipTransition.CROSS_DISSOLVE,
        defaultDuckLevel = 0.18f
    ),
    HISTORICAL_ARCHIVE(
        displayName = "Historical Archive",
        bengaliTag = "ইতিহাস ও দলিল",
        defaultGrade = ClipColorGrade.VINTAGE_ARCHIVE,
        defaultVignette = 0.45f,
        defaultGrain = 0.40f,
        defaultTransition = ClipTransition.FILM_BURN,
        defaultDuckLevel = 0.20f
    ),
    INVESTIGATIVE_DOSSIER(
        displayName = "Investigative Dossier",
        bengaliTag = "অনুসন্ধানী প্রতিবেদন",
        defaultGrade = ClipColorGrade.COLD_CASE,
        defaultVignette = 0.35f,
        defaultGrain = 0.15f,
        defaultTransition = ClipTransition.PAPER_SLIDE,
        defaultDuckLevel = 0.16f
    ),
    MODERN_EXPLAINER(
        displayName = "Modern Explainer",
        bengaliTag = "আধুনিক তথ্যচিত্র",
        defaultGrade = ClipColorGrade.CLEAN_BROADCAST,
        defaultVignette = 0.15f,
        defaultGrain = 0.0f,
        defaultTransition = ClipTransition.GLITCH_CUT,
        defaultDuckLevel = 0.22f
    )
}

enum class AspectRatioPreset(val label: String, val width: Int, val height: Int) {
    RATIO_16_9("16:9 YouTube", 1920, 1080),
    RATIO_9_16("9:16 Shorts", 1080, 1920),
    RATIO_1_1("1:1 Square", 1080, 1080)
}

enum class KenBurnsPreset(val label: String) {
    NONE("Static"),
    SLOW_ZOOM_IN("Slow Zoom In"),
    SLOW_ZOOM_OUT("Slow Zoom Out"),
    PAN_LEFT_RIGHT("Pan L → R"),
    DRAMATIC_PUSH("Dramatic Push")
}

enum class ClipColorGrade(val label: String) {
    THEME_DEFAULT("Theme Auto"),
    DARK_NOIR("Dark Noir"),
    COLD_CASE("Cold Case"),
    VINTAGE_ARCHIVE("Vintage Archive"),
    CLEAN_BROADCAST("Clean Broadcast"),
    HIGH_CONTRAST("High Contrast")
}

enum class ClipTransition(val label: String) {
    NONE("Cut"),
    CROSS_DISSOLVE("Dissolve"),
    FILM_BURN("Film Burn"),
    GLITCH_CUT("Glitch"),
    PAPER_SLIDE("Paper Slide")
}

enum class VoiceFitMode(val label: String, val shortTag: String) {
    NONE("Original", "1.0x"),
    SMOOTH_SLOW_MO("Smooth Slow-Mo Fit", "Slow-Fit"),
    FREEZE_KEN_BURNS("Freeze + Ken Burns Hold", "Hold+Zoom"),
    SEAMLESS_LOOP("Seamless Ambient Loop", "Loop Fit")
}

enum class MotionTextStyle(val label: String, val bengaliHint: String) {
    TYPEWRITER_CASE_FILE("Typewriter Case-File", "অক্ষর টাইপ হয়ে আসবে"),
    LOWER_THIRD_IDENTITY("Lower-Third Identity", "নাম ও পরিচয় কার্ড"),
    MARKER_HIGHLIGHT_QUOTE("Marker Highlight Quote", "হলুদ/লাল হাইলাইটার দাগ"),
    KINETIC_WORD_POP("Kinetic Word-by-Word", "ভয়েসের তালে পপ-আপ লেখা"),
    NUMBER_DATE_COUNTER("Number / Year Counter", "সাল বা সংখ্যা গণনা"),
    IMPACT_CHAPTER_TITLE("Impact Chapter Title", "বড় অধ্যায় শিরোনাম"),
    SOURCE_CITATION("Source / Archive Tag", "সূত্র ও কপিরাইট লেবেল")
}

enum class EvidenceOverlayType(val label: String) {
    YELLOW_HIGHLIGHT("Yellow Highlighter"),
    RED_CIRCLE_CALLOUT("Red Evidence Box"),
    REDACTION_BLUR("Redact / Blur Info"),
    CLASSIFIED_STAMP("Classified Stamp"),
    SPOTLIGHT_DIM("Spotlight Focus")
}

@JsonClass(generateAdapter = true)
data class VisualClip(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val title: String,
    val isImage: Boolean,
    val originalDurationMs: Long,
    val startMs: Long,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val kenBurns: KenBurnsPreset = KenBurnsPreset.SLOW_ZOOM_IN,
    val colorGrade: ClipColorGrade = ClipColorGrade.THEME_DEFAULT,
    val transitionIn: ClipTransition = ClipTransition.CROSS_DISSOLVE,
    val voiceFitMode: VoiceFitMode = VoiceFitMode.NONE,
    val playbackSpeed: Float = 1.0f,
    val brightness: Float = 0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val volume: Float = 1.0f,
    val chapterId: String? = null
)

@JsonClass(generateAdapter = true)
data class MotionTextOverlay(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val subText: String = "",
    val style: MotionTextStyle = MotionTextStyle.LOWER_THIRD_IDENTITY,
    val startMs: Long,
    val durationMs: Long = 4000L,
    val accentColorHex: Long = 0xFFF59E0B,
    val hasBackdropBlur: Boolean = true,
    val linkedSfxId: String? = null,
    val yPositionFraction: Float = 0.78f,
    val fontScale: Float = 1.0f
)

@JsonClass(generateAdapter = true)
data class EvidenceOverlay(
    val id: String = UUID.randomUUID().toString(),
    val type: EvidenceOverlayType,
    val label: String = "",
    val startMs: Long,
    val durationMs: Long = 3500L,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.45f,
    val widthFraction: Float = 0.55f,
    val heightFraction: Float = 0.14f
)

@JsonClass(generateAdapter = true)
data class SilenceGap(
    val id: String = UUID.randomUUID().toString(),
    val startMs: Long,
    val durationMs: Long,
    val isProtected: Boolean = false,
    val isTrimmed: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SilenceCondenserConfig(
    val enabled: Boolean = false,
    val minGapToTrimMs: Long = 500L,
    val maxGapToTrimMs: Long = 2500L,
    val protectDramaticPausesOverMs: Long = 3000L,
    val keepBreathingGapMs: Long = 250L
)

@JsonClass(generateAdapter = true)
data class VoiceTrackItem(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val title: String,
    val startMs: Long,
    val durationMs: Long,
    val originalDurationMs: Long = durationMs,
    val volume: Float = 1.0f,
    val noiseReductionEnabled: Boolean = true,
    val broadcastNormalizeEnabled: Boolean = true,
    val silenceGaps: List<SilenceGap> = emptyList(),
    val waveformSamples: List<Float> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AudioSfxItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val uriString: String,
    val sfxPresetId: String? = null,
    val isBgm: Boolean = false,
    val startMs: Long,
    val durationMs: Long,
    val volume: Float = if (isBgm) 0.65f else 0.85f,
    val autoDuckEnabled: Boolean = isBgm,
    val duckLevel: Float = 0.18f
)

@JsonClass(generateAdapter = true)
data class ChapterMarker(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startMs: Long,
    val colorHex: Long = 0xFF00D1FF
)

@JsonClass(generateAdapter = true)
data class DocumentaryTimeline(
    val theme: DocumentaryTheme = DocumentaryTheme.INVESTIGATIVE_DOSSIER,
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.RATIO_16_9,
    val vignetteIntensity: Float = 0.35f,
    val filmGrainIntensity: Float = 0.15f,
    val safeZoneEnabled: Boolean = false,
    val showDraftWatermark: Boolean = false,
    val silenceConfig: SilenceCondenserConfig = SilenceCondenserConfig(),
    val visualClips: List<VisualClip> = emptyList(),
    val motionTexts: List<MotionTextOverlay> = emptyList(),
    val evidenceOverlays: List<EvidenceOverlay> = emptyList(),
    val voiceTracks: List<VoiceTrackItem> = emptyList(),
    val audioSfxItems: List<AudioSfxItem> = emptyList(),
    val chapters: List<ChapterMarker> = emptyList(),
    val brandKitId: String? = null
) {
    val totalDurationMs: Long
        get() {
            val videoEnd = visualClips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
            val voiceEnd = voiceTracks.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
            val textEnd = motionTexts.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
            return maxOf(videoEnd, voiceEnd, textEnd, 1000L)
        }
}

enum class ExportResolution(val label: String, val heightPx: Int, val estMbPerMinute: Float) {
    RES_720P("720p Fast Draft", 720, 14f),
    RES_1080P("1080p YouTube HD", 1080, 28f),
    RES_4K("4K Master", 2160, 72f)
}

@JsonClass(generateAdapter = true)
data class ExportConfig(
    val resolution: ExportResolution = ExportResolution.RES_1080P,
    val isDraftReview: Boolean = false,
    val draftWatermarkLabel: String = "DRAFT REVIEW",
    val enableSmartPassThrough: Boolean = true,
    val chunkMinutes: Int = 12,
    val exportChapterSplit: Boolean = false,
    val targetFps: Int = 30
)
