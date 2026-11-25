package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DocumentaryAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EvidenceRed
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorToolSheet
import com.example.viewmodel.TimelineSelection

private data class ToolbarAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val tint: Color = TextPrimary,
    val onClick: () -> Unit
)

@Composable
fun BottomToolbar(
    selection: TimelineSelection,
    onDeselect: () -> Unit,
    onOpenSheet: (EditorToolSheet) -> Unit,
    onSplitClip: () -> Unit,
    onDuplicateClip: () -> Unit,
    onDeleteSelected: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onAddVoiceover: () -> Unit
) {
    val actions = when (selection) {
        TimelineSelection.None -> listOf(
            ToolbarAction("text", "Motion Text", Icons.Default.TextFields, DocumentaryAmber) {
                onOpenSheet(EditorToolSheet.MOTION_TEXT_ADD)
            },
            ToolbarAction("evidence", "Evidence FX", Icons.Default.Highlight, EvidenceRed) {
                onOpenSheet(EditorToolSheet.EVIDENCE_ADD)
            },
            ToolbarAction("sfx", "SFX Vault", Icons.Default.MusicNote, ElectricCyan) {
                onOpenSheet(EditorToolSheet.SFX_VAULT)
            },
            ToolbarAction("silence", "Silence", Icons.Default.GraphicEq) {
                onOpenSheet(EditorToolSheet.SMART_SILENCE)
            },
            ToolbarAction("color", "Color & Noir", Icons.Default.Palette) {
                onOpenSheet(EditorToolSheet.COLOR_AND_FX)
            },
            ToolbarAction("chapters", "Chapters", Icons.Default.BookmarkAdd) {
                onOpenSheet(EditorToolSheet.CHAPTERS)
            },
            ToolbarAction("brand", "Brand Kit", Icons.Default.Badge) {
                onOpenSheet(EditorToolSheet.BRAND_KIT_APPLY)
            },
            ToolbarAction("ratio", "Format", Icons.Default.AspectRatio, onClick = onCycleAspectRatio),
            ToolbarAction("voice", "+ Voice", Icons.AutoMirrored.Filled.VolumeUp, onClick = onAddVoiceover)
        )

        is TimelineSelection.Clip -> listOf(
            ToolbarAction("back", "Done", Icons.Default.Close, TextSecondary, onDeselect),
            ToolbarAction("split", "Split", Icons.Default.ContentCut, ElectricCyan, onSplitClip),
            ToolbarAction("voicefit", "Voice Fit", Icons.Default.Speed, DocumentaryAmber) {
                onOpenSheet(EditorToolSheet.VOICE_FIT)
            },
            ToolbarAction("kenburns", "Motion / FX", Icons.Default.CenterFocusStrong) {
                onOpenSheet(EditorToolSheet.COLOR_AND_FX)
            },
            ToolbarAction("dup", "Duplicate", Icons.Default.ContentCopy, onClick = onDuplicateClip),
            ToolbarAction("del", "Delete", Icons.Default.DeleteOutline, EvidenceRed, onDeleteSelected)
        )

        is TimelineSelection.Text -> listOf(
            ToolbarAction("back", "Done", Icons.Default.Close, TextSecondary, onDeselect),
            ToolbarAction("add_txt", "New Text", Icons.Default.TextFields, DocumentaryAmber) {
                onOpenSheet(EditorToolSheet.MOTION_TEXT_ADD)
            },
            ToolbarAction("del", "Delete", Icons.Default.DeleteOutline, EvidenceRed, onDeleteSelected)
        )

        is TimelineSelection.Evidence -> listOf(
            ToolbarAction("back", "Done", Icons.Default.Close, TextSecondary, onDeselect),
            ToolbarAction("add_ev", "Evidence FX", Icons.Default.Highlight, DocumentaryAmber) {
                onOpenSheet(EditorToolSheet.EVIDENCE_ADD)
            },
            ToolbarAction("del", "Delete", Icons.Default.DeleteOutline, EvidenceRed, onDeleteSelected)
        )

        is TimelineSelection.Voice -> listOf(
            ToolbarAction("back", "Done", Icons.Default.Close, TextSecondary, onDeselect),
            ToolbarAction("silence", "Smart Silence", Icons.Default.GraphicEq, ElectricCyan) {
                onOpenSheet(EditorToolSheet.SMART_SILENCE)
            },
            ToolbarAction("sfx", "Audio Mix", Icons.Default.Tune, DocumentaryAmber) {
                onOpenSheet(EditorToolSheet.SFX_VAULT)
            },
            ToolbarAction("del", "Delete", Icons.Default.DeleteOutline, EvidenceRed, onDeleteSelected)
        )

        is TimelineSelection.AudioSfx -> listOf(
            ToolbarAction("back", "Done", Icons.Default.Close, TextSecondary, onDeselect),
            ToolbarAction("sfx", "Auto-Duck", Icons.Default.Tune, ElectricCyan) {
                onOpenSheet(EditorToolSheet.SFX_VAULT)
            },
            ToolbarAction("del", "Delete", Icons.Default.DeleteOutline, EvidenceRed, onDeleteSelected)
        )
    }

    Surface(
        color = StudioObsidian,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(actions, key = { it.id }) { action ->
                Column(
                    modifier = Modifier
                        .widthIn(min = 60.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (action.id == "back") StudioCard else Color.Transparent)
                        .clickable(onClick = action.onClick)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("toolbar_${action.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.label,
                        tint = action.tint,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = action.label,
                        color = if (action.tint == TextSecondary) TextSecondary else TextPrimary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
