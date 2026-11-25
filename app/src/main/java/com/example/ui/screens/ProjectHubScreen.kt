package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ProjectEntity
import com.example.engine.MagneticTimelineEngine
import com.example.model.AspectRatioPreset
import com.example.model.DocumentaryTheme
import com.example.ui.theme.DocumentaryAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectHubScreen(
    projects: List<ProjectEntity>,
    onNewProject: (DocumentaryTheme) -> Unit,
    onOpenAutoDocWizard: () -> Unit,
    onOpenBrandKits: () -> Unit,
    onLoadSampleCase: () -> Unit,
    onOpenProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit
) {
    Scaffold(
        containerColor = StudioObsidian,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioObsidian),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(ElectricCyan, DocumentaryAmber)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieCreation,
                                contentDescription = stringResource(R.string.app_name),
                                tint = StudioObsidian,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.studio_tagline),
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenBrandKits,
                        modifier = Modifier.testTag("brand_kits_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = stringResource(R.string.brand_kits),
                            tint = DocumentaryAmber
                        )
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // CapCut-style Hero "+ New Project" Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(ElectricCyan, ElectricCyan.copy(alpha = 0.72f), DocumentaryAmber.copy(alpha = 0.85f))
                                )
                            )
                            .clickable { onNewProject(DocumentaryTheme.INVESTIGATIVE_DOSSIER) }
                            .padding(vertical = 24.dp, horizontal = 20.dp)
                            .testTag("new_project_hero_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.new_project),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = StudioObsidian,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "ডকুমেন্টারি ও ইনভেস্টিগেটিভ টাইমলাইন • 3-4h Chunk Safe",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StudioObsidian.copy(alpha = 0.85f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(StudioObsidian),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.new_project),
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                // Quick Actions: Auto-Doc Wizard + Sample Case + Client Brand Kits
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickStudioActionCard(
                            title = stringResource(R.string.auto_doc_wizard),
                            subtitle = "অটো সাজানো ও ডাকিং",
                            icon = Icons.Default.AutoAwesome,
                            accent = DocumentaryAmber,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("auto_doc_wizard_card"),
                            onClick = onOpenAutoDocWizard
                        )
                        QuickStudioActionCard(
                            title = stringResource(R.string.load_sample_case),
                            subtitle = "রেডি ডেমো প্রজেক্ট",
                            icon = Icons.Default.Science,
                            accent = ElectricCyan,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("load_sample_case_card"),
                            onClick = onLoadSampleCase
                        )
                    }
                }

                // Documentary Theme Presets Strip
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "One-Tap Documentary Styles",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(DocumentaryTheme.entries) { theme ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = StudioCard),
                                    border = BorderStroke(1.dp, StudioBorder),
                                    modifier = Modifier
                                        .width(172.dp)
                                        .clickable { onNewProject(theme) }
                                        .testTag("preset_${theme.name.lowercase()}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = theme.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = theme.bengaliTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DocumentaryAmber
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "${theme.defaultGrade.label} • ${theme.defaultTransition.label}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Saved Documentary Projects
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.recent_projects),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${projects.size} saved",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                }

                if (projects.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioSurface),
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MovieCreation,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = stringResource(R.string.empty_projects_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.empty_projects_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = onLoadSampleCase,
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = StudioObsidian),
                                        modifier = Modifier.testTag("empty_load_sample_btn")
                                    ) {
                                        Text("Load Sample Case")
                                    }
                                    OutlinedButton(
                                        onClick = onOpenAutoDocWizard,
                                        border = BorderStroke(1.dp, DocumentaryAmber),
                                        modifier = Modifier.testTag("empty_wizard_btn")
                                    ) {
                                        Text("Auto-Doc Wizard", color = DocumentaryAmber)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        ProjectItemCard(
                            project = project,
                            onClick = { onOpenProject(project.id) },
                            onDelete = { onDeleteProject(project.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStudioActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = StudioCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ProjectItemCard(
    project: ProjectEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val ratioLabel = runCatching { AspectRatioPreset.valueOf(project.aspectRatioName).label.substringBefore(" ") }
        .getOrDefault("16:9")
    val themeLabel = runCatching { DocumentaryTheme.valueOf(project.themeName).displayName }
        .getOrDefault("Documentary")

    Card(
        colors = CardDefaults.cardColors(containerColor = StudioCardElevated),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("project_card_${project.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioObsidian),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Open ${project.title}",
                    tint = ElectricCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(themeLabel)
                        if (project.clientName.isNotBlank()) append(" • ${project.clientName}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioMetaChip(
                        text = MagneticTimelineEngine.formatTimecode(project.totalDurationMs),
                        color = ElectricCyan
                    )
                    StudioMetaChip(text = "${project.chapterCount} Ch", color = DocumentaryAmber)
                    StudioMetaChip(text = ratioLabel, color = TextSecondary)
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_project_${project.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete project",
                    tint = TextMuted
                )
            }
        }
    }
}

@Composable
private fun StudioMetaChip(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        color = StudioObsidian,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
