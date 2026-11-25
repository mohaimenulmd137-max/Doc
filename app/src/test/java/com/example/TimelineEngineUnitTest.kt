package com.example

import com.example.engine.MagneticTimelineEngine
import com.example.engine.SilenceDetectorEngine
import com.example.model.SilenceCondenserConfig
import com.example.model.SilenceGap
import com.example.model.VisualClip
import com.example.model.VoiceFitMode
import com.example.model.VoiceTrackItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineEngineUnitTest {

    @Test
    fun `magnetic track prevents clip overlap after resize`() {
        val clips = listOf(
            VisualClip(id = "c1", uriString = "", title = "A", isImage = true, originalDurationMs = 4000L, startMs = 0L, durationMs = 4000L),
            VisualClip(id = "c2", uriString = "", title = "B", isImage = true, originalDurationMs = 5000L, startMs = 4000L, durationMs = 5000L)
        )
        val resized = MagneticTimelineEngine.resizeClipWithRipple(clips, "c1", 7000L)
        assertEquals(0L, resized[0].startMs)
        assertEquals(7000L, resized[0].durationMs)
        assertEquals(7000L, resized[1].startMs)
        assertEquals(5000L, resized[1].durationMs)
    }

    @Test
    fun `voice fit stretches 8s video clip to 10s via smooth slow motion`() {
        val clips = listOf(
            VisualClip(id = "v1", uriString = "", title = "B-Roll", isImage = false, originalDurationMs = 8000L, startMs = 0L, durationMs = 8000L)
        )
        val fitted = MagneticTimelineEngine.applyVoiceFit(clips, "v1", 10_000L, VoiceFitMode.SMOOTH_SLOW_MO)
        assertEquals(10_000L, fitted[0].durationMs)
        assertEquals(0.8f, fitted[0].playbackSpeed, 0.01f)
        assertEquals(VoiceFitMode.SMOOTH_SLOW_MO, fitted[0].voiceFitMode)
    }

    @Test
    fun `smart silence condenser protects dramatic pauses over 3s`() {
        val voice = VoiceTrackItem(
            id = "vt1",
            uriString = "",
            title = "Narration",
            startMs = 0L,
            durationMs = 14_000L,
            originalDurationMs = 14_000L,
            silenceGaps = listOf(
                SilenceGap(id = "g1", startMs = 3000L, durationMs = 1500L, isProtected = false),
                SilenceGap(id = "g2", startMs = 7000L, durationMs = 3500L, isProtected = false)
            )
        )
        val result = SilenceDetectorEngine.applySilenceCondenser(
            voice,
            SilenceCondenserConfig(
                enabled = true,
                minGapToTrimMs = 500L,
                maxGapToTrimMs = 2500L,
                protectDramaticPausesOverMs = 3000L,
                keepBreathingGapMs = 250L
            )
        )
        assertTrue(result.silenceGaps[0].isTrimmed)
        assertFalse(result.silenceGaps[1].isTrimmed)
        assertEquals(12_750L, result.durationMs)
    }
}
