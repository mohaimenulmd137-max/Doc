package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DocuCutDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = StudioObsidian,
    primaryContainer = ElectricCyanDim,
    onPrimaryContainer = TextPrimary,
    secondary = DocumentaryAmber,
    onSecondary = StudioObsidian,
    secondaryContainer = DocumentaryAmberDim,
    onSecondaryContainer = TextPrimary,
    tertiary = EvidenceRed,
    onTertiary = TextPrimary,
    tertiaryContainer = EvidenceRedDim,
    onTertiaryContainer = TextPrimary,
    background = StudioObsidian,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioCard,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder,
    error = EvidenceRed,
    onError = TextPrimary
)

val DocuCutShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun DocuCutTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DocuCutDarkColorScheme,
        typography = Typography,
        shapes = DocuCutShapes,
        content = content
    )
}
