package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.example.engine.MagneticTimelineEngine
import com.example.export.DocumentaryExportService
import com.example.export.ExportPhase
import com.example.export.ExportStateHolder
import com.example.model.ExportConfig
import com.example.model.ExportResolution
import com.example.ui.theme.DocumentaryAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.EvidenceRed
import com.example.ui.theme.MonoTimecodeFamily
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.StudioViewModel
import java.io.File

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    projectId: String,
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val exportProgress by ExportStateHolder.state.collectAsStateWithLifecycle()

    var resolution by remember { mutableStateOf(ExportResolution.RES_1080P) }
    var isDraftReview by remember { mutableStateOf(false) }
    var enablePassThrough by remember { mutableStateOf(true) }
    var chunkMinutes by remember { mutableIntStateOf(12) }
    var exportChapterSplit by remember { mutableStateOf(false) }
    var copiedMessage by remember { mutableStateOf(false) }

    val timeline = editorState.timeline
    val totalMinutes = (timeline.totalDurationMs / 60_000f).coerceAtLeast(0.2f)
    val estSizeMb = (totalMinutes * resolution.estMbPerMinute).toInt().coerceAtLeast(4)
    val freeStorageMb = remember { getAvailableStorageMb(context) }
    val hasEnoughSpace = freeStorageMb > estSizeMb * 1.2f

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val config = ExportConfig(
            resolution = resolution,
            isDraftReview = isDraftReview,
            enableSmartPassThrough = enablePassThrough,
            chunkMinutes = chunkMinutes,
            exportChapterSplit = exportChapterSplit
        )
        DocumentaryExportService.startExport(context, projectId, config)
    }

    fun launchExport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val config = ExportConfig(
                resolution = resolution,
                isDraftReview = isDraftReview,
                enableSmartPassThrough = enablePassThrough,
                chunkMinutes = chunkMinutes,
                exportChapterSplit = exportChapterSplit
            )
            DocumentaryExportService.startExport(context, projectId, config)
        }
    }

    Scaffold(
        containerColor = StudioObsidian,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioObsidian),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("export_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                title = {
                    Column {
                        Text("Hardware Turbo Export", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            text = "Media3 MediaCodec + Chunked Zero-Hang Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().widthIn(max = 680.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Storage & Hardware Pre-flight Check
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioCard),
                        border = BorderStroke(1.dp, if (hasEnoughSpace) EmeraldSafe.copy(alpha = 0.5f) else EvidenceRed)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hasEnoughSpace) Icons.Default.Memory else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (hasEnoughSpace) EmeraldSafe else EvidenceRed
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Duration: ${MagneticTimelineEngine.formatTimecode(timeline.totalDurationMs)} • Est. ~$estSizeMb MB",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Free Device Storage: ${freeStorageMb / 1024} GB (${freeStorageMb} MB)",
                                    color = if (hasEnoughSpace) TextSecondary else EvidenceRed,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                // Resolution
                item {
                    Text("1. Video Resolution", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportResolution.entries.forEach { res ->
                            FilterChip(
                                selected = resolution == res,
                                onClick = { resolution = res },
                                label = { Text(res.label) }
                            )
                        }
                    }
                }

                // 3-4h Optimization & Client Draft Settings
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioCard),
                        border = BorderStroke(1.dp, StudioBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("2. 3–4 Hour Safe Export & Client Mode", style = MaterialTheme.typography.titleSmall, color = DocumentaryAmber)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Client Draft Review Copy", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                    Text("ওয়াটারমার্ক + রানিং টাইমকোড সহ ড্রাফট প্রিভিউ", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                                Switch(
                                    checked = isDraftReview,
                                    onCheckedChange = { isDraftReview = it },
                                    modifier = Modifier.testTag("export_draft_switch")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Smart Pass-Through Optimization", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                    Text("অপরিবর্তিত অংশ রি-এনকোড না করে দ্রুত কপি করে", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                                Switch(checked = enablePassThrough, onCheckedChange = { enablePassThrough = it })
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Split Export by Chapter", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                    Text("প্রতিটি অধ্যায় আলাদা নিরাপদ MP4 হিসেবে তৈরি হবে", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                                Switch(checked = exportChapterSplit, onCheckedChange = { exportChapterSplit = it })
                            }

                            if (!exportChapterSplit) {
                                Text("Memory-Safe Chunk Size:", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(10, 12, 15).forEach { mins ->
                                        FilterChip(
                                            selected = chunkMinutes == mins,
                                            onClick = { chunkMinutes = mins },
                                            label = { Text("$mins min blocks") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Export Progress / Completed Output
                if (exportProgress.phase != ExportPhase.IDLE && exportProgress.projectId == projectId) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioCardElevated),
                            border = BorderStroke(1.dp, ElectricCyan)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (exportProgress.phase) {
                                            ExportPhase.RUNNING -> "Exporting ${exportProgress.currentChunkTitle}..."
                                            ExportPhase.COMPLETED -> "Export Completed!"
                                            ExportPhase.FAILED -> "Export Failed"
                                            ExportPhase.CANCELLED -> "Export Cancelled"
                                            else -> "Preparing..."
                                        },
                                        color = if (exportProgress.phase == ExportPhase.COMPLETED) EmeraldSafe else ElectricCyan,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${exportProgress.overallPercent}%",
                                        fontFamily = MonoTimecodeFamily,
                                        color = TextPrimary
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { exportProgress.overallPercent / 100f },
                                    color = ElectricCyan,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    text = "Chunk ${exportProgress.currentChunkIndex}/${exportProgress.totalChunks}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )

                                if (exportProgress.errorMessage != null) {
                                    Text(exportProgress.errorMessage!!, color = EvidenceRed, style = MaterialTheme.typography.bodyMedium)
                                }

                                if (exportProgress.phase == ExportPhase.RUNNING) {
                                    OutlinedButton(
                                        onClick = { DocumentaryExportService.cancelExport(context) },
                                        border = BorderStroke(1.dp, EvidenceRed)
                                    ) {
                                        Text("Cancel Export", color = EvidenceRed)
                                    }
                                }

                                if (exportProgress.phase == ExportPhase.COMPLETED) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val firstFile = exportProgress.outputFiles.firstOrNull()
                                        if (firstFile != null) {
                                            Button(
                                                onClick = { shareExportedFile(context, File(firstFile)) },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe, contentColor = StudioObsidian),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null)
                                                Spacer(Modifier.width(6.dp))
                                                Text("Share MP4")
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val txt = MagneticTimelineEngine.generateYoutubeChapters(timeline.chapters)
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cm.setPrimaryClip(ClipData.newPlainText("YouTube Chapters", txt))
                                                copiedMessage = true
                                            },
                                            border = BorderStroke(1.dp, DocumentaryAmber),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                if (copiedMessage) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                tint = DocumentaryAmber
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (copiedMessage) "Copied!" else "Chapters", color = DocumentaryAmber)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Start Export CTA
                item {
                    val isRunning = exportProgress.phase == ExportPhase.RUNNING
                    Button(
                        onClick = { launchExport() },
                        enabled = !isRunning && timeline.visualClips.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = StudioObsidian),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_export_btn")
                    ) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "Hardware Export Running..." else "Start Hardware Export",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

private fun getAvailableStorageMb(context: Context): Long {
    val path = context.filesDir ?: Environment.getDataDirectory()
    val stat = StatFs(path.path)
    return (stat.availableBlocksLong * stat.blockSizeLong) / (1024L * 1024L)
}

private fun shareExportedFile(context: Context, file: File) {
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Documentary Video"))
}
