package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class SolarSemanticColors(
    val blueprintBg: Color,
    val blueprintGrid: Color,
    val okText: Color,
    val okContainer: Color,
    val warningText: Color,
    val warningContainer: Color,
    val dangerText: Color,
    val dangerContainer: Color,
    val solarAccent: Color
)

val LocalSolarColors = staticCompositionLocalOf {
    SolarSemanticColors(
        blueprintBg = BlueprintGridBg,
        blueprintGrid = BlueprintGridLine,
        okText = EmeraldOk,
        okContainer = EmeraldOkBg,
        warningText = WarningAmber,
        warningContainer = WarningAmberBg,
        dangerText = DangerRed,
        dangerContainer = DangerRedBg,
        solarAccent = SolarAmber
    )
}

private val LightColors = lightColorScheme(
    primary = BlueprintCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = SolarAmberDark,
    onSecondary = Color.White,
    secondaryContainer = SolarAmberLight,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = EmeraldOk,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldOkBg,
    onTertiaryContainer = Color(0xFF065F46),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = Color(0xFFE2E8F0),
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerRedBg,
    onErrorContainer = Color(0xFF991B1B)
)

private val DarkColors = darkColorScheme(
    primary = BlueprintCyanBright,
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = SolarAmber,
    onSecondary = OnSolarAmber,
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = SolarAmberLight,
    tertiary = EmeraldOk,
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = EmeraldOkDarkBg,
    onTertiaryContainer = EmeraldOkBg,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = Color(0xFF1E293B),
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerRedDarkBg,
    onErrorContainer = DangerRedBg
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SonceDimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val semanticColors = if (darkTheme) {
        SolarSemanticColors(
            blueprintBg = Color(0xFF071021),
            blueprintGrid = Color(0xFF172C4B),
            okText = Color(0xFF34D399),
            okContainer = EmeraldOkDarkBg,
            warningText = SolarAmber,
            warningContainer = WarningAmberDarkBg,
            dangerText = Color(0xFFF87171),
            dangerContainer = DangerRedDarkBg,
            solarAccent = SolarAmber
        )
    } else {
        SolarSemanticColors(
            blueprintBg = BlueprintGridBg,
            blueprintGrid = BlueprintGridLine,
            okText = Color(0xFF047857),
            okContainer = EmeraldOkBg,
            warningText = Color(0xFFB45309),
            warningContainer = WarningAmberBg,
            dangerText = Color(0xFFB91C1C),
            dangerContainer = DangerRedBg,
            solarAccent = SolarAmber
        )
    }

    CompositionLocalProvider(LocalSolarColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
