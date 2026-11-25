package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import com.example.engine.MagneticTimelineEngine
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
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SampleDocumentaryFactory {

    suspend fun createSampleCaseProject(
        context: Context,
        repository: DocuCutRepository
    ): String = withContext(Dispatchers.IO) {
        val demoDir = File(context.filesDir, "demo").apply { mkdirs() }
        val img1 = renderEvidenceSlide(
            File(demoDir, "evidence_1_dossier.png"),
            bgTop = Color.parseColor("#0D1520"),
            bgBottom = Color.parseColor("#182230"),
            tag = "CASE FILE #402 • 14 NOV 1998",
            headline = "THE MIDNIGHT ARCHIVE",
            bodyLines = listOf(
                "02:14 AM — Security logs from Sector 7 vanished.",
                "Witness reported an unmarked sedan near Dock 4.",
                "Classified ledger stamped: DO NOT RELEASE."
            ),
            accent = Color.parseColor("#F59E0B")
        )
        val img2 = renderEvidenceSlide(
            File(demoDir, "evidence_2_map.png"),
            bgTop = Color.parseColor("#0A1A1E"),
            bgBottom = Color.parseColor("#0F2A33"),
            tag = "SURVEILLANCE GRID • SECTOR 07",
            headline = "ROUTE RECONSTRUCTION",
            bodyLines = listOf(
                "Node A: Central Railway Terminal (01:48 AM)",
                "Node B: Old River Bridge Checkpoint (02:06 AM)",
                "Signal Lost: Underground Vault B2 (02:14 AM)"
            ),
            accent = Color.parseColor("#00D1FF")
        )
        val img3 = renderEvidenceSlide(
            File(demoDir, "evidence_3_newspaper.png"),
            bgTop = Color.parseColor("#1C1914"),
            bgBottom = Color.parseColor("#2A241B"),
            tag = "DAILY CHRONICLE • ARCHIVE SCAN",
            headline = "MISSING LEDGERS SPARK PROBE",
            bodyLines = listOf(
                "\"Four crates of financial transcripts were moved",
                "before the morning audit team arrived at dawn.\"",
                "— Official Inquiry Report, Page 19"
            ),
            accent = Color.parseColor("#EF4444")
        )
        val img4 = renderEvidenceSlide(
            File(demoDir, "evidence_4_timeline.png"),
            bgTop = Color.parseColor("#11131B"),
            bgBottom = Color.parseColor("#1A1D29"),
            tag = "FORENSIC SUMMARY • 2026",
            headline = "THE UNANSWERED QUESTION",
            bodyLines = listOf(
                "Who authorized the 02:10 AM vault override?",
                "Three signatures remain redacted in Exhibit C.",
                "Full investigation continues in Chapter 02."
            ),
            accent = Color.parseColor("#00D1FF")
        )

        val narrationWav = SfxSynthesizer.generateSampleNarrationWav(context)
        val droneWav = SfxSynthesizer.ensurePresetWavFile(context, "noir_drone")
        val whooshWav = SfxSynthesizer.ensurePresetWavFile(context, "whoosh")
        val shutterWav = SfxSynthesizer.ensurePresetWavFile(context, "shutter")

        val rawClips = listOf(
            VisualClip(
                uriString = Uri.fromFile(img1).toString(),
                title = "01 • Case Dossier Cover",
                isImage = true,
                originalDurationMs = 4000L,
                startMs = 0L,
                durationMs = 4000L,
                kenBurns = KenBurnsPreset.SLOW_ZOOM_IN,
                colorGrade = ClipColorGrade.DARK_NOIR,
                transitionIn = ClipTransition.NONE
            ),
            VisualClip(
                uriString = Uri.fromFile(img2).toString(),
                title = "02 • City Route Map",
                isImage = true,
                originalDurationMs = 3500L,
                startMs = 4000L,
                durationMs = 3500L,
                kenBurns = KenBurnsPreset.PAN_LEFT_RIGHT,
                colorGrade = ClipColorGrade.COLD_CASE,
                transitionIn = ClipTransition.CROSS_DISSOLVE
            ),
            VisualClip(
                uriString = Uri.fromFile(img3).toString(),
                title = "03 • Archive Clipping",
                isImage = true,
                originalDurationMs = 3500L,
                startMs = 7500L,
                durationMs = 3500L,
                kenBurns = KenBurnsPreset.DRAMATIC_PUSH,
                colorGrade = ClipColorGrade.VINTAGE_ARCHIVE,
                transitionIn = ClipTransition.PAPER_SLIDE,
                voiceFitMode = VoiceFitMode.FREEZE_KEN_BURNS
            ),
            VisualClip(
                uriString = Uri.fromFile(img4).toString(),
                title = "04 • Forensic Summary",
                isImage = true,
                originalDurationMs = 3000L,
                startMs = 11000L,
                durationMs = 3000L,
                kenBurns = KenBurnsPreset.SLOW_ZOOM_OUT,
                colorGrade = ClipColorGrade.DARK_NOIR,
                transitionIn = ClipTransition.FILM_BURN
            )
        )
        val magneticClips = MagneticTimelineEngine.repackMagneticTrack(rawClips)

        val voiceRaw = VoiceTrackItem(
            uriString = Uri.fromFile(narrationWav).toString(),
            title = "Bangla Narration Track 01",
            startMs = 0L,
            durationMs = 14_000L,
            originalDurationMs = 14_000L,
            noiseReductionEnabled = true,
            broadcastNormalizeEnabled = true
        )
        val analyzedVoice = SilenceDetectorEngine.analyzeVoiceTrack(context, voiceRaw)

        val timeline = DocumentaryTimeline(
            theme = DocumentaryTheme.INVESTIGATIVE_DOSSIER,
            aspectRatio = AspectRatioPreset.RATIO_16_9,
            vignetteIntensity = 0.45f,
            filmGrainIntensity = 0.20f,
            silenceConfig = SilenceCondenserConfig(
                enabled = false,
                minGapToTrimMs = 500L,
                maxGapToTrimMs = 2500L,
                protectDramaticPausesOverMs = 3000L,
                keepBreathingGapMs = 250L
            ),
            visualClips = magneticClips,
            motionTexts = listOf(
                MotionTextOverlay(
                    text = "অধ্যায় ১: মধ্যরাতের গোপন ফাইল",
                    subText = "CASE #402 • NOV 1998",
                    style = MotionTextStyle.TYPEWRITER_CASE_FILE,
                    startMs = 400L,
                    durationMs = 3400L,
                    accentColorHex = 0xFFF59E0B,
                    linkedSfxId = "typewriter",
                    yPositionFraction = 0.78f
                ),
                MotionTextOverlay(
                    text = "রাত ২:১৪ মিনিটে সিসিটিভি লগ মুছে ফেলা হয়",
                    subText = "তদন্ত রিপোর্ট • পৃষ্ঠা ১৯",
                    style = MotionTextStyle.MARKER_HIGHLIGHT_QUOTE,
                    startMs = 7600L,
                    durationMs = 3200L,
                    accentColorHex = 0xFFF59E0B,
                    linkedSfxId = "shutter",
                    yPositionFraction = 0.74f
                ),
                MotionTextOverlay(
                    text = "Source: National Archive Exhibit C",
                    subText = "",
                    style = MotionTextStyle.SOURCE_CITATION,
                    startMs = 4200L,
                    durationMs = 3000L,
                    accentColorHex = 0xFF00D1FF,
                    yPositionFraction = 0.14f
                )
            ),
            evidenceOverlays = listOf(
                EvidenceOverlay(
                    type = EvidenceOverlayType.YELLOW_HIGHLIGHT,
                    label = "02:14 AM OVERRIDE",
                    startMs = 7800L,
                    durationMs = 2800L,
                    xFraction = 0.50f,
                    yFraction = 0.52f,
                    widthFraction = 0.62f,
                    heightFraction = 0.10f
                ),
                EvidenceOverlay(
                    type = EvidenceOverlayType.CLASSIFIED_STAMP,
                    label = "CLASSIFIED",
                    startMs = 11200L,
                    durationMs = 2600L,
                    xFraction = 0.75f,
                    yFraction = 0.25f,
                    widthFraction = 0.28f,
                    heightFraction = 0.14f
                )
            ),
            voiceTracks = listOf(analyzedVoice),
            audioSfxItems = listOf(
                AudioSfxItem(
                    title = "Noir Tension Drone (BGM)",
                    uriString = Uri.fromFile(droneWav).toString(),
                    sfxPresetId = "noir_drone",
                    isBgm = true,
                    startMs = 0L,
                    durationMs = 14_000L,
                    volume = 0.65f,
                    autoDuckEnabled = true,
                    duckLevel = 0.18f
                ),
                AudioSfxItem(
                    title = "Cinematic Whoosh",
                    uriString = Uri.fromFile(whooshWav).toString(),
                    sfxPresetId = "whoosh",
                    isBgm = false,
                    startMs = 3800L,
                    durationMs = 900L,
                    volume = 0.85f
                ),
                AudioSfxItem(
                    title = "Camera Shutter",
                    uriString = Uri.fromFile(shutterWav).toString(),
                    sfxPresetId = "shutter",
                    isBgm = false,
                    startMs = 7400L,
                    durationMs = 350L,
                    volume = 0.90f
                )
            ),
            chapters = listOf(
                ChapterMarker(title = "01 • The Midnight Vault", startMs = 0L, colorHex = 0xFF00D1FF),
                ChapterMarker(title = "02 • Missing Ledger Evidence", startMs = 7500L, colorHex = 0xFFF59E0B)
            )
        )

        val projectId = "sample-case-402"
        repository.saveTimeline(
            projectId = projectId,
            title = "Case #402: Midnight Archive",
            clientName = "Chronicle Investigative",
            timeline = timeline
        )

        // Seed a sample Client Brand Kit if none exists
        repository.saveBrandKit(
            BrandKitEntity(
                id = "kit-chronicle-default",
                clientName = "Chronicle Investigative",
                channelTag = "@ChronicleBD",
                accentColorHex = 0xFFF59E0B,
                watermarkText = "CHRONICLE DOCS",
                defaultCitationPrefix = "Archive Source: ",
                safeZoneEnabled = true,
                aspectRatioName = AspectRatioPreset.RATIO_16_9.name
            )
        )
        projectId
    }

    private fun renderEvidenceSlide(
        file: File,
        bgTop: Int,
        bgBottom: Int,
        tag: String,
        headline: String,
        bodyLines: List<String>,
        accent: Int
    ): File {
        val width = 1280
        val height = 720
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                bgTop, bgBottom, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle investigative grid lines
        val gridPaint = Paint().apply {
            color = Color.argb(26, 255, 255, 255)
            strokeWidth = 1.5f
        }
        var gx = 80f
        while (gx < width) {
            canvas.drawLine(gx, 0f, gx, height.toFloat(), gridPaint)
            gx += 80f
        }
        var gy = 80f
        while (gy < height) {
            canvas.drawLine(0f, gy, width.toFloat(), gy, gridPaint)
            gy += 80f
        }

        // Evidence Dossier Card
        val cardPaint = Paint().apply {
            color = Color.argb(170, 12, 15, 22)
        }
        val borderPaint = Paint().apply {
            color = accent
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val rect = RectF(100f, 110f, width - 100f, height - 110f)
        canvas.drawRoundRect(rect, 20f, 20f, cardPaint)
        canvas.drawRoundRect(rect, 20f, 20f, borderPaint)

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 26f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.12f
        }
        canvas.drawText(tag, 150f, 195f, tagPaint)

        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 54f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(headline, 150f, 275f, headPaint)

        val linePaint = Paint().apply {
            color = accent
            strokeWidth = 5f
        }
        canvas.drawLine(150f, 305f, 430f, 305f, linePaint)

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(215, 224, 236)
            textSize = 32f
            typeface = Typeface.SERIF
        }
        var yPos = 385f
        bodyLines.forEach { line ->
            canvas.drawText(line, 150f, yPos, bodyPaint)
            yPos += 62f
        }

        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 92, out)
        }
        bmp.recycle()
        return file
    }
}
