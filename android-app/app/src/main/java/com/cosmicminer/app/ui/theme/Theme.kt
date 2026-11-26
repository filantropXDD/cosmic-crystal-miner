package com.cosmicminer.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object CosmicColors {
    val DeepSpaceTop = Color(0xFF060814)
    val DeepSpaceBottom = Color(0xFF0D132B)
    val SurfaceCard = Color(0xFF131936)
    val SurfaceElevated = Color(0xFF1B234A)
    val BorderSubtle = Color(0x28A78BFA)

    val NeonPurple = Color(0xFF8B5CF6)
    val NeonPurpleBright = Color(0xFFA78BFA)
    val NeonCyan = Color(0xFF22D3EE)
    val NeonGold = Color(0xFFFBBF24)
    val EmeraldSuccess = Color(0xFF34D399)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
}

private val CosmicDarkColorScheme = darkColorScheme(
    primary = CosmicColors.NeonPurple,
    onPrimary = Color.White,
    secondary = CosmicColors.NeonCyan,
    onSecondary = Color(0xFF05131F),
    tertiary = CosmicColors.NeonGold,
    background = CosmicColors.DeepSpaceTop,
    onBackground = CosmicColors.TextPrimary,
    surface = CosmicColors.SurfaceCard,
    onSurface = CosmicColors.TextPrimary,
    surfaceVariant = CosmicColors.SurfaceElevated,
    onSurfaceVariant = CosmicColors.TextSecondary,
    outline = CosmicColors.BorderSubtle
)

@Composable
fun CosmicMinerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CosmicDarkColorScheme,
        content = content
    )
}
