package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine.SfxSynthesizer
import com.example.model.AspectRatioPreset
import com.example.model.DocumentaryTheme
import com.example.model.VoiceFitMode
import com.example.ui.theme.DocumentaryAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoDocWizardScreen(
    onBack: () -> Unit,
    onAssemble: (
        title: String,
        clientName: String,
        theme: DocumentaryTheme,
        ratio: AspectRatioPreset,
        visualUris: List<Uri>,
        voiceUris: List<Uri>,
        bgmPresetId: String?,
        autoKenBurns: Boolean,
        autoDucking: Boolean,
        autoFitMode: VoiceFitMode
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("Investigative Case Report") }
    var clientName by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf(DocumentaryTheme.INVESTIGATIVE_DOSSIER) }
    var selectedRatio by remember { mutableStateOf(AspectRatioPreset.RATIO_16_9) }
    var visualUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var voiceUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedBgmId by remember { mutableStateOf<String?>("noir_drone") }
    var autoKenBurns by remember { mutableStateOf(true) }
    var autoDucking by remember { mutableStateOf(true) }
    var autoFitMode by remember { mutableStateOf(VoiceFitMode.SMOOTH_SLOW_MO) }

    val visualPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            visualUris = visualUris + uris
        }
    }

    val voicePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            voiceUris = voiceUris + uris
        }
    }

    Scaffold(
        containerColor = StudioObsidian,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioObsidian),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("wizard_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                title = {
                    Column {
                        Text("Auto-Doc Wizard", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("এক ক্লিকে ডকুমেন্টারি টাইমলাইন সাজান", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Documentary Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_title_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client / Channel Name (Optional)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_client_input")
                    )
                }

                item {
                    Text("1. Narrative Style & Color Preset", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DocumentaryTheme.entries) { theme ->
                            val selected = selectedTheme == theme
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) ElectricCyan.copy(alpha = 0.18f) else StudioCard
                                ),
                                border = BorderStroke(1.dp, if (selected) ElectricCyan else StudioBorder),
                                modifier = Modifier
                                    .width(160.dp)
                                    .clickable { selectedTheme = theme }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(theme.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text(theme.bengaliTag, style = MaterialTheme.typography.labelSmall, color = DocumentaryAmber)
                                }
                            }
                        }
                    }
                }

                item {
                    Text("2. Aspect Ratio", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AspectRatioPreset.entries.forEach { ratio ->
                            FilterChip(
                                selected = selectedRatio == ratio,
                                onClick = { selectedRatio = ratio },
                                label = { Text(ratio.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }
                }

                item {
                    Text("3. Import Voiceover & B-Roll Media", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { voicePicker.launch(arrayOf("audio/*")) },
                            border = BorderStroke(1.dp, ElectricCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wizard_pick_voice_btn")
                        ) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, tint = ElectricCyan)
                            Spacer(Modifier.width(6.dp))
                            Text("Voice (${voiceUris.size})", color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = {
                                visualPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            border = BorderStroke(1.dp, DocumentaryAmber),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wizard_pick_visual_btn")
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = DocumentaryAmber)
                            Spacer(Modifier.width(6.dp))
                            Text("Video/Photo (${visualUris.size})", color = TextPrimary)
                        }
                    }
                }

                item {
                    Text("4. Documentary Background Ambience", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    val bgms = SfxSynthesizer.builtInPresets.filter { it.isBgm }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedBgmId == null,
                            onClick = { selectedBgmId = null },
                            label = { Text("No BGM") }
                        )
                        bgms.forEach { bgm ->
                            FilterChip(
                                selected = selectedBgmId == bgm.id,
                                onClick = { selectedBgmId = bgm.id },
                                label = { Text(bgm.title) }
                            )
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioCard),
                        border = BorderStroke(1.dp, StudioBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Automation Controls (Editable Later)", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Auto Ken Burns Zoom/Pan", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                    Text("স্থির ছবিতে সিনেম্যাটিক মোশন", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                                Switch(checked = autoKenBurns, onCheckedChange = { autoKenBurns = it })
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Auto Audio Ducking", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                    Text("ভয়েস চললে মিউজিক অটো কমে যাবে", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                                Switch(checked = autoDucking, onCheckedChange = { autoDucking = it })
                            }
                            Text("Voice-to-Footage Fit (8s → 10s)", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(
                                    listOf(
                                        VoiceFitMode.SMOOTH_SLOW_MO,
                                        VoiceFitMode.FREEZE_KEN_BURNS,
                                        VoiceFitMode.SEAMLESS_LOOP
                                    )
                                ) { mode ->
                                    FilterChip(
                                        selected = autoFitMode == mode,
                                        onClick = { autoFitMode = mode },
                                        label = { Text(mode.label) }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            onAssemble(
                                title, clientName, selectedTheme, selectedRatio,
                                visualUris, voiceUris, selectedBgmId,
                                autoKenBurns, autoDucking, autoFitMode
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = StudioObsidian),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("wizard_assemble_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Assemble Documentary Timeline", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
