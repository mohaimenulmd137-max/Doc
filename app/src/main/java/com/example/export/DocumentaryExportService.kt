package com.example.export

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.R
import com.example.data.DocuCutDatabase
import com.example.data.DocuCutRepository
import com.example.data.ExportJobEntity
import com.example.engine.ExportChunkSpec
import com.example.engine.MagneticTimelineEngine
import com.example.engine.Media3CompositionBuilder
import com.example.model.ExportConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@UnstableApi
class DocumentaryExportService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeTransformer: Transformer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isCancelled = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL_EXPORT) {
            isCancelled = true
            activeTransformer?.cancel()
            ExportStateHolder.update { it.copy(phase = ExportPhase.CANCELLED) }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val projectId = intent?.getStringExtra(EXTRA_PROJECT_ID) ?: return START_NOT_STICKY
        val configJson = intent.getStringExtra(EXTRA_EXPORT_CONFIG_JSON) ?: return START_NOT_STICKY

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val config = moshi.adapter(ExportConfig::class.java).fromJson(configJson) ?: ExportConfig()

        startForeground(NOTIFICATION_ID, buildNotification("Preparing hardware export...", 0))
        acquireWakeLock()
        isCancelled = false

        serviceScope.launch {
            runChunkedExport(projectId, config)
        }
        return START_NOT_STICKY
    }

    private suspend fun runChunkedExport(projectId: String, config: ExportConfig) {
        val startTime = System.currentTimeMillis()
        val repo = DocuCutRepository(DocuCutDatabase.getInstance(applicationContext))
        val project = repo.getProject(projectId)
        if (project == null) {
            failExport("Project not found in local database.")
            return
        }

        val timeline = repo.decodeTimeline(project.timelineJson)
        val brandKit = timeline.brandKitId?.let { repo.getBrandKit(it) }
        val chunks = Media3CompositionBuilder.planExportChunks(timeline, config)

        val exportDir = File(filesDir, "exports").apply { mkdirs() }
        val safeTitle = project.title.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "Documentary" }
        val jobId = UUID.randomUUID().toString()

        // Generate YouTube Chapters .txt file automatically
        val chaptersTxt = File(exportDir, "${safeTitle}_chapters.txt")
        chaptersTxt.writeText(MagneticTimelineEngine.generateYoutubeChapters(timeline.chapters))

        ExportStateHolder.update {
            ExportProgressState(
                phase = ExportPhase.RUNNING,
                projectId = projectId,
                projectTitle = project.title,
                currentChunkIndex = 0,
                totalChunks = chunks.size,
                currentChunkTitle = chunks.firstOrNull()?.title ?: "",
                overallPercent = 0,
                chaptersTxtPath = chaptersTxt.absolutePath
            )
        }

        val producedFiles = mutableListOf<String>()
        processChunkSequentially(
            chunks = chunks,
            index = 0,
            exportDir = exportDir,
            safeTitle = safeTitle,
            timeline = timeline,
            config = config,
            brandWatermark = brandKit?.watermarkText,
            producedFiles = producedFiles,
            startTime = startTime,
            onAllFinished = {
                serviceScope.launch {
                    val elapsed = System.currentTimeMillis() - startTime
                    ExportStateHolder.update {
                        it.copy(
                            phase = ExportPhase.COMPLETED,
                            overallPercent = 100,
                            outputFiles = producedFiles.toList(),
                            elapsedMs = elapsed
                        )
                    }
                    repo.saveExportJob(
                        ExportJobEntity(
                            id = jobId,
                            projectId = projectId,
                            projectTitle = project.title,
                            outputPath = producedFiles.firstOrNull().orEmpty(),
                            chaptersTxtPath = chaptersTxt.absolutePath,
                            resolutionLabel = config.resolution.label,
                            isDraftReview = config.isDraftReview,
                            status = "COMPLETED",
                            progressPercent = 100,
                            completedChunks = chunks.size,
                            totalChunks = chunks.size
                        )
                    )
                    updateNotification("Export Complete: ${project.title}", 100)
                    releaseWakeLock()
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                }
            }
        )
    }

    private fun processChunkSequentially(
        chunks: List<ExportChunkSpec>,
        index: Int,
        exportDir: File,
        safeTitle: String,
        timeline: com.example.model.DocumentaryTimeline,
        config: ExportConfig,
        brandWatermark: String?,
        producedFiles: MutableList<String>,
        startTime: Long,
        onAllFinished: () -> Unit
    ) {
        if (isCancelled) return
        if (index >= chunks.size) {
            onAllFinished()
            return
        }

        val chunk = chunks[index]
        val suffix = if (chunks.size > 1) "_part${index + 1}" else ""
        val draftTag = if (config.isDraftReview) "_DRAFT" else ""
        val outFile = File(exportDir, "${safeTitle}${draftTag}${suffix}.mp4")
        if (outFile.exists()) outFile.delete()

        val composition = Media3CompositionBuilder.buildComposition(
            context = applicationContext,
            timeline = chunk.timelineSlice,
            config = config,
            brandWatermark = brandWatermark,
            chunkStartOffsetMs = chunk.startMs
        )

        val transformerBuilder = Transformer.Builder(applicationContext)
        if (config.enableSmartPassThrough) {
            transformerBuilder.experimentalSetTrimOptimizationEnabled(true)
        }

        val transformer = transformerBuilder
            .addListener(object : Transformer.Listener {
                override fun onCompleted(comp: Composition, exportResult: ExportResult) {
                    producedFiles.add(outFile.absolutePath)
                    val completedPct = (((index + 1).toFloat() / chunks.size) * 100).toInt()
                    ExportStateHolder.update {
                        it.copy(
                            currentChunkIndex = index + 1,
                            overallPercent = completedPct,
                            outputFiles = producedFiles.toList(),
                            elapsedMs = System.currentTimeMillis() - startTime
                        )
                    }
                    updateNotification("Chunk ${index + 1}/${chunks.size} complete", completedPct)
                    processChunkSequentially(
                        chunks, index + 1, exportDir, safeTitle, timeline, config,
                        brandWatermark, producedFiles, startTime, onAllFinished
                    )
                }

                override fun onError(
                    comp: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    failExport(exportException.localizedMessage ?: "Hardware codec export failed.")
                }
            })
            .build()

        activeTransformer = transformer
        transformer.start(composition, outFile.absolutePath)

        val progressHolder = ProgressHolder()
        val poller = object : Runnable {
            override fun run() {
                if (isCancelled) return
                val state = transformer.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    val chunkFrac = progressHolder.progress / 100f
                    val overall = (((index + chunkFrac) / chunks.size) * 100).toInt().coerceIn(0, 99)
                    ExportStateHolder.update {
                        it.copy(
                            currentChunkIndex = index,
                            currentChunkTitle = chunk.title,
                            overallPercent = overall,
                            elapsedMs = System.currentTimeMillis() - startTime
                        )
                    }
                    updateNotification("Exporting ${chunk.title} ($overall%)", overall)
                }
                if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    mainHandler.postDelayed(this, 450L)
                }
            }
        }
        mainHandler.post(poller)
    }

    private fun failExport(message: String) {
        ExportStateHolder.update {
            it.copy(phase = ExportPhase.FAILED, errorMessage = message)
        }
        updateNotification("Export Failed: $message", 0)
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.export_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.export_channel_desc)
            }
            nm.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(progress < 100)
            .setProgress(100, progress, progress == 0)
            .build()
    }

    private fun updateNotification(text: String, progress: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text, progress))
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DocuCut:ExportWakeLock").apply {
            acquire(6 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
        wakeLock = null
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "docucut_export_channel"
        private const val NOTIFICATION_ID = 4021
        const val EXTRA_PROJECT_ID = "extra_project_id"
        const val EXTRA_EXPORT_CONFIG_JSON = "extra_export_config_json"
        const val ACTION_CANCEL_EXPORT = "com.example.action.CANCEL_EXPORT"

        fun startExport(context: Context, projectId: String, config: ExportConfig) {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val json = moshi.adapter(ExportConfig::class.java).toJson(config)
            val intent = Intent(context, DocumentaryExportService::class.java).apply {
                putExtra(EXTRA_PROJECT_ID, projectId)
                putExtra(EXTRA_EXPORT_CONFIG_JSON, json)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancelExport(context: Context) {
            val intent = Intent(context, DocumentaryExportService::class.java).apply {
                action = ACTION_CANCEL_EXPORT
            }
            context.startService(intent)
        }
    }
}
