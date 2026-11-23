package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TruckDarkColorScheme = darkColorScheme(
    primary = RacingOrange,
    onPrimary = TextPrimary,
    primaryContainer = RacingOrangeLight,
    onPrimaryContainer = SimBackground,
    secondary = SkyCyan,
    onSecondary = SimBackground,
    tertiary = EmeraldGo,
    onTertiary = SimBackground,
    background = SimBackground,
    onBackground = TextPrimary,
    surface = SimSurface,
    onSurface = TextPrimary,
    surfaceVariant = SimSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SimOutline,
    error = CrimsonAlert,
    onError = TextPrimary
)

private val TruckShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun TruckSimulatorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TruckDarkColorScheme,
        typography = Typography,
        shapes = TruckShapes,
        content = content
    )
}
