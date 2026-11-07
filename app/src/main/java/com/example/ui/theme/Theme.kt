package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CrimsonMarker,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEE2E2),
    onPrimaryContainer = Color(0xFF7F1D1D),
    secondary = BlueprintNavy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = ForestGreenMarker,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCFCE7),
    onTertiaryContainer = Color(0xFF14532D),
    background = NotebookPaperBackground,
    onBackground = CharcoalInk,
    surface = Color(0xFFFFFDF8),
    onSurface = CharcoalInk,
    surfaceVariant = Color(0xFFF3ECE0),
    onSurfaceVariant = Color(0xFF3B3734),
    outline = Color(0xFF57534E),
    outlineVariant = Color(0xFFD6CEBF)
)

private val DarkColorScheme = darkColorScheme(
    primary = CrimsonMarkerDark,
    onPrimary = Color(0xFF450A0A),
    primaryContainer = Color(0xFF7F1D1D),
    onPrimaryContainer = Color(0xFFFECACA),
    secondary = BlueprintNavyDark,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFBFDBFE),
    tertiary = ForestGreenMarkerDark,
    onTertiary = Color(0xFF052E16),
    tertiaryContainer = Color(0xFF14532D),
    onTertiaryContainer = Color(0xFFBBF7D0),
    background = NotebookPaperDark,
    onBackground = CharcoalInkDark,
    surface = Color(0xFF27272A),
    onSurface = CharcoalInkDark,
    surfaceVariant = Color(0xFF3F3F46),
    onSurfaceVariant = Color(0xFFE4E4E7),
    outline = Color(0xFFA1A1AA),
    outlineVariant = Color(0xFF52525B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

