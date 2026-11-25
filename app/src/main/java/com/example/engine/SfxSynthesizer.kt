package com.example.engine

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

data class SfxPreset(
    val id: String,
    val title: String,
    val bengaliDesc: String,
    val category: String,
    val durationMs: Long,
    val isBgm: Boolean = false
)

/**
 * Generates real 44.1kHz 16-bit PCM WAV files locally on-device for documentary SFX & ambience
 * so every sound effect can be previewed immediately and mixed via Media3 Transformer.
 */
object SfxSynthesizer {

    val builtInPresets = listOf(
        SfxPreset("whoosh", "Cinematic Whoosh", "ট্রানজিশন ও টেক্সট এন্ট্রি", "Transition", 900L),
        SfxPreset("bass_boom", "Sub Bass Boom", "রহস্য ও চ্যাপ্টার ধাক্কা", "Impact", 1600L),
        SfxPreset("shutter", "Camera Shutter", "ছবি বা প্রমাণ স্লাইড", "Evidence", 350L),
        SfxPreset("typewriter", "Typewriter Click", "কেস-ফাইল টাইপিং", "Motion Text", 450L),
        SfxPreset("riser", "Suspense Riser", "টেনশন বাড়ানোর ইফেক্ট", "Tension", 2800L),
        SfxPreset("heartbeat", "Slow Heartbeat", "উত্তেজনা ও থ্রিলার আবহ", "Tension", 1500L),
        SfxPreset("night_ambience", "City Night Ambience", "রাতের শহর ও বৃষ্টির আবহ", "Ambience", 5000L, isBgm = true),
        SfxPreset("noir_drone", "Noir Tension Drone", "সাসপেন্স ব্যাকগ্রাউন্ড ড্রোন", "BGM", 8000L, isBgm = true)
    )

    private var activePlayer: MediaPlayer? = null

    suspend fun ensurePresetWavFile(context: Context, presetId: String): File =
        withContext(Dispatchers.IO) {
            val sfxDir = File(context.filesDir, "sfx").apply { mkdirs() }
            val target = File(sfxDir, "$presetId.wav")
            if (target.exists() && target.length() > 44L) return@withContext target

            val sampleRate = 44100
            val preset = builtInPresets.find { it.id == presetId } ?: builtInPresets.first()
            val numSamples = ((preset.durationMs / 1000.0) * sampleRate).toInt().coerceAtLeast(1024)
            val samples = ShortArray(numSamples)
            val rng = Random(presetId.hashCode())

            var lpState = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val norm = i.toDouble() / numSamples
                val sample = when (presetId) {
                    "whoosh" -> {
                        val env = sin(PI * norm)
                        val noise = rng.nextDouble(-1.0, 1.0)
                        val alpha = 0.03 + 0.25 * env
                        lpState += alpha * (noise - lpState)
                        lpState * env * 0.85
                    }
                    "bass_boom" -> {
                        val freq = 95.0 * exp(-t * 3.5) + 34.0
                        val env = exp(-t * 2.4)
                        sin(2.0 * PI * freq * t) * env * 0.92
                    }
                    "shutter" -> {
                        val click1 = if (t < 0.03) exp(-t * 140.0) * rng.nextDouble(-1.0, 1.0) else 0.0
                        val click2 = if (t in 0.09..0.14) exp(-(t - 0.09) * 120.0) * rng.nextDouble(-1.0, 1.0) else 0.0
                        (click1 + click2) * 0.9
                    }
                    "typewriter" -> {
                        val pulse1 = exp(-t * 95.0) * sin(2.0 * PI * 1850.0 * t)
                        val pulse2 = if (t > 0.16) exp(-(t - 0.16) * 95.0) * sin(2.0 * PI * 2100.0 * t) else 0.0
                        val noise = rng.nextDouble(-0.3, 0.3) * exp(-t * 70.0)
                        (pulse1 + pulse2 + noise) * 0.75
                    }
                    "riser" -> {
                        val env = norm * norm
                        val freq = 55.0 + 380.0 * norm * norm
                        val tone = sin(2.0 * PI * freq * t)
                        val shimmer = sin(2.0 * PI * (freq * 1.5) * t) * 0.35
                        (tone + shimmer) * env * 0.75
                    }
                    "heartbeat" -> {
                        val beat1 = exp(-t * 18.0) * sin(2.0 * PI * 58.0 * t)
                        val beat2 = if (t > 0.26) exp(-(t - 0.26) * 20.0) * sin(2.0 * PI * 52.0 * t) else 0.0
                        (beat1 + beat2 * 0.8) * 0.9
                    }
                    "night_ambience" -> {
                        val noise = rng.nextDouble(-1.0, 1.0)
                        lpState += 0.04 * (noise - lpState)
                        val swell = 0.7 + 0.3 * sin(2.0 * PI * 0.25 * t)
                        lpState * swell * 0.45
                    }
                    else -> { // noir_drone
                        val fade = minOf(1.0, minOf(norm * 6.0, (1.0 - norm) * 6.0))
                        val f1 = sin(2.0 * PI * 65.4 * t) * 0.55
                        val f2 = sin(2.0 * PI * 98.0 * t) * 0.30
                        val f3 = sin(2.0 * PI * 131.2 * t) * 0.15
                        (f1 + f2 + f3) * fade * 0.65
                    }
                }
                samples[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }

            writeWav(target, sampleRate, samples)
            target
        }

    suspend fun previewPreset(context: Context, presetId: String) {
        val file = ensurePresetWavFile(context, presetId)
        withContext(Dispatchers.Main) {
            stopPreview()
            activePlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                setOnCompletionListener {
                    it.release()
                    if (activePlayer === it) activePlayer = null
                }
                prepare()
                start()
            }
        }
    }

    fun stopPreview() {
        runCatching {
            activePlayer?.stop()
            activePlayer?.release()
        }
        activePlayer = null
    }

    /**
     * Generates a realistic spoken-cadence sample narration WAV with natural pauses
     * (including a 1.4s breath gap and a 3.4s dramatic pause) for testing Smart Silence Condenser!
     */
    suspend fun generateSampleNarrationWav(context: Context): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "demo").apply { mkdirs() }
            val target = File(dir, "sample_narration.wav")
            if (target.exists() && target.length() > 44L) return@withContext target

            val sampleRate = 22050
            val totalSec = 14
            val samples = ShortArray(sampleRate * totalSec)

            // Speech segments: 0.0-3.2s, (gap 3.2-4.7s = 1.5s), 4.7-7.4s, (dramatic gap 7.4-10.9s = 3.5s), 10.9-13.8s
            for (i in samples.indices) {
                val t = i.toDouble() / sampleRate
                val inSpeech = (t in 0.2..3.2) || (t in 4.7..7.4) || (t in 10.9..13.8)
                val valSample = if (inSpeech) {
                    val syllableMod = 0.5 + 0.5 * sin(2.0 * PI * 4.5 * t)
                    val base = sin(2.0 * PI * (135.0 + 18.0 * sin(2.0 * PI * 1.4 * t)) * t)
                    val harmonic = sin(2.0 * PI * 270.0 * t) * 0.4
                    (base + harmonic) * syllableMod * 0.45
                } else {
                    0.0
                }
                samples[i] = (valSample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }
            writeWav(target, sampleRate, samples)
            target
        }

    private fun writeWav(file: File, sampleRate: Int, pcmData: ShortArray) {
        val dataSize = pcmData.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16) // PCM chunk size
            putShort(1) // PCM format
            putShort(1) // Mono
            putInt(sampleRate)
            putInt(sampleRate * 2) // Byte rate
            putShort(2) // Block align
            putShort(16) // Bits per sample
            put("data".toByteArray())
            putInt(dataSize)
        }
        FileOutputStream(file).use { out ->
            out.write(header.array())
            val pcmBytes = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            pcmData.forEach { pcmBytes.putShort(it) }
            out.write(pcmBytes.array())
        }
    }
}
