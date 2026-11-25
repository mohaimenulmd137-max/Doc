package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.BrandKitEntity
import com.example.model.AspectRatioPreset
import com.example.ui.theme.DocumentaryAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandKitScreen(
    brandKits: List<BrandKitEntity>,
    onBack: () -> Unit,
    onSaveKit: (BrandKitEntity) -> Unit,
    onDeleteKit: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = StudioObsidian,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioObsidian),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("brand_kit_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                title = {
                    Column {
                        Text("Client Brand Kits", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("ক্লায়েন্টের লোগো, ওয়াটারমার্ক ও সেফ-জোন প্রোফাইল", style = MaterialTheme.typography.labelSmall, color = DocumentaryAmber)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = ElectricCyan,
                contentColor = StudioObsidian,
                modifier = Modifier.testTag("add_brand_kit_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Client Brand Kit")
            }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (brandKits.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioCard),
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("No Client Brand Kits", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Save each client's watermark, citation prefix, and safe-zone aspect ratio for instant 1-tap branding.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(brandKits, key = { it.id }) { kit ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioCard),
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(kit.clientName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text(
                                        "${kit.channelTag} • Watermark: ${kit.watermarkText}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ElectricCyan
                                    )
                                    Text(
                                        "Citation: \"${kit.defaultCitationPrefix}...\" • ${kit.aspectRatioName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                IconButton(onClick = { onDeleteKit(kit.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete kit", tint = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var clientName by remember { mutableStateOf("") }
        var channelTag by remember { mutableStateOf("@") }
        var watermark by remember { mutableStateOf("") }
        var citationPrefix by remember { mutableStateOf("Source: ") }
        var safeZone by remember { mutableStateOf(true) }
        var ratio by remember { mutableStateOf(AspectRatioPreset.RATIO_16_9) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = StudioCard,
            title = { Text("New Client Brand Kit", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = channelTag,
                        onValueChange = { channelTag = it },
                        label = { Text("Channel Handle") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = watermark,
                        onValueChange = { watermark = it },
                        label = { Text("Watermark Text") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = citationPrefix,
                        onValueChange = { citationPrefix = it },
                        label = { Text("Source Citation Prefix") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AspectRatioPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = ratio == preset,
                                onClick = { ratio = preset },
                                label = { Text(preset.label.substringBefore(" ")) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Safe Zone Guides", color = TextPrimary)
                        Switch(checked = safeZone, onCheckedChange = { safeZone = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clientName.isNotBlank()) {
                            onSaveKit(
                                BrandKitEntity(
                                    clientName = clientName.trim(),
                                    channelTag = channelTag.trim(),
                                    watermarkText = watermark.ifBlank { clientName.trim().uppercase() },
                                    defaultCitationPrefix = citationPrefix,
                                    safeZoneEnabled = safeZone,
                                    aspectRatioName = ratio.name
                                )
                            )
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = StudioObsidian)
                ) {
                    Text("Save Kit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}
