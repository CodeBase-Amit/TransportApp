package com.example.transportapp.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Custom chrome colors that have no standard Material 3 role slot.
 * Exposed separately from the color scheme so they can never be reached by accident.
 */
data class TransportColors(
    // Money still in motion / pending
    val haulAmber: Color,
    val haulAmberContainer: Color,
    val onHaulAmber: Color,
    // Delivered chip
    val deliveredContainer: Color,
    // S20 (D57) — the celebratory accent + tinted shadow system
    val sunrise: Color,
    val onSunrise: Color,
    val sunriseContainer: Color,
    val onSunriseContainer: Color,
    val shadowTint: Color,
    val paperShadow: Color,
    // Paper — never in chrome, never inverts in dark theme
    val paperWhite: Color,
    val paperPink: Color,
    val paperYellow: Color,
    val paperGreen: Color,
    val paperInk: Color,
    val paperRule: Color,
    val stampViolet: Color,
    val stampRed: Color
)

val LocalTransportColors = staticCompositionLocalOf {
    TransportColors(
        haulAmber = Color.Unspecified,
        haulAmberContainer = Color.Unspecified,
        onHaulAmber = Color.Unspecified,
        deliveredContainer = Color.Unspecified,
        sunrise = Color.Unspecified,
        onSunrise = Color.Unspecified,
        sunriseContainer = Color.Unspecified,
        onSunriseContainer = Color.Unspecified,
        shadowTint = Color.Unspecified,
        paperShadow = Color.Unspecified,
        paperWhite = Color.Unspecified,
        paperPink = Color.Unspecified,
        paperYellow = Color.Unspecified,
        paperGreen = Color.Unspecified,
        paperInk = Color.Unspecified,
        paperRule = Color.Unspecified,
        stampViolet = Color.Unspecified,
        stampRed = Color.Unspecified
    )
}

private val LightColorScheme = lightColorScheme(
    primary = LightColors.primary,
    onPrimary = LightColors.onPrimary,
    primaryContainer = LightColors.primaryContainer,
    onPrimaryContainer = LightColors.onPrimaryContainer,
    secondary = LightColors.secondary,
    onSecondary = LightColors.onSecondary,
    secondaryContainer = LightColors.secondaryContainer,
    onSecondaryContainer = LightColors.onSecondaryContainer,
    tertiary = LightColors.tertiary,
    onTertiary = LightColors.onTertiary,
    tertiaryContainer = LightColors.tertiaryContainer,
    onTertiaryContainer = LightColors.onTertiaryContainer,
    error = LightColors.error,
    onError = LightColors.onError,
    errorContainer = LightColors.errorContainer,
    onErrorContainer = LightColors.onErrorContainer,
    background = LightColors.background,
    onBackground = LightColors.onBackground,
    surface = LightColors.surface,
    onSurface = LightColors.onSurface,
    surfaceVariant = LightColors.surfaceVariant,
    onSurfaceVariant = LightColors.onSurfaceVariant,
    surfaceContainerLowest = LightColors.surfaceContainerLowest,
    surfaceContainerLow = LightColors.surfaceContainerLow,
    surfaceContainer = LightColors.surfaceContainer,
    surfaceContainerHigh = LightColors.surfaceContainerHigh,
    surfaceContainerHighest = LightColors.surfaceContainerHighest,
    outline = LightColors.outline,
    outlineVariant = LightColors.outlineVariant,
    inverseSurface = LightColors.inverseSurface,
    inverseOnSurface = LightColors.inverseOnSurface,
    inversePrimary = LightColors.inversePrimary,
    surfaceTint = LightColors.surfaceTint
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkColors.primary,
    onPrimary = DarkColors.onPrimary,
    primaryContainer = DarkColors.primaryContainer,
    onPrimaryContainer = DarkColors.onPrimaryContainer,
    secondary = DarkColors.secondary,
    onSecondary = DarkColors.onSecondary,
    secondaryContainer = DarkColors.secondaryContainer,
    onSecondaryContainer = DarkColors.onSecondaryContainer,
    tertiary = DarkColors.tertiary,
    onTertiary = DarkColors.onTertiary,
    tertiaryContainer = DarkColors.tertiaryContainer,
    onTertiaryContainer = DarkColors.onTertiaryContainer,
    error = DarkColors.error,
    onError = DarkColors.onError,
    errorContainer = DarkColors.errorContainer,
    onErrorContainer = DarkColors.onErrorContainer,
    background = DarkColors.background,
    onBackground = DarkColors.onBackground,
    surface = DarkColors.surface,
    onSurface = DarkColors.onSurface,
    surfaceVariant = DarkColors.surfaceVariant,
    onSurfaceVariant = DarkColors.onSurfaceVariant,
    surfaceContainerLowest = DarkColors.surfaceContainerLowest,
    surfaceContainerLow = DarkColors.surfaceContainerLow,
    surfaceContainer = DarkColors.surfaceContainer,
    surfaceContainerHigh = DarkColors.surfaceContainerHigh,
    surfaceContainerHighest = DarkColors.surfaceContainerHighest,
    outline = DarkColors.outline,
    outlineVariant = DarkColors.outlineVariant,
    inverseSurface = DarkColors.inverseSurface,
    inverseOnSurface = DarkColors.inverseOnSurface,
    inversePrimary = DarkColors.inversePrimary,
    surfaceTint = DarkColors.surfaceTint
)

private val LightTransportColors = TransportColors(
    haulAmber = LightColors.haulAmber,
    haulAmberContainer = LightColors.haulAmberContainer,
    onHaulAmber = LightColors.onHaulAmber,
    deliveredContainer = LightColors.deliveredContainer,
    sunrise = LightColors.sunrise,
    onSunrise = LightColors.onSunrise,
    sunriseContainer = LightColors.sunriseContainer,
    onSunriseContainer = LightColors.onSunriseContainer,
    shadowTint = LightColors.shadowTint,
    paperShadow = LightColors.paperShadow,
    paperWhite = PaperColors.paperWhite,
    paperPink = PaperColors.paperPink,
    paperYellow = PaperColors.paperYellow,
    paperGreen = PaperColors.paperGreen,
    paperInk = PaperColors.paperInk,
    paperRule = PaperColors.paperRule,
    stampViolet = PaperColors.stampViolet,
    stampRed = PaperColors.stampRed
)

private val DarkTransportColors = TransportColors(
    // Custom chrome remaps from Design.md §A4
    haulAmber = DarkColors.haulAmber,
    haulAmberContainer = DarkColors.haulAmberContainer,
    onHaulAmber = DarkColors.onHaulAmber,
    deliveredContainer = DarkColors.deliveredContainer,
    sunrise = DarkColors.sunrise,
    onSunrise = DarkColors.onSunrise,
    sunriseContainer = DarkColors.sunriseContainer,
    onSunriseContainer = DarkColors.onSunriseContainer,
    shadowTint = DarkColors.shadowTint,
    paperShadow = DarkColors.paperShadow,
    // Paper does NOT invert in dark theme (Design.md §A2.3)
    paperWhite = PaperColors.paperWhite,
    paperPink = PaperColors.paperPink,
    paperYellow = PaperColors.paperYellow,
    paperGreen = PaperColors.paperGreen,
    paperInk = PaperColors.paperInk,
    paperRule = PaperColors.paperRule,
    stampViolet = PaperColors.stampViolet,
    stampRed = PaperColors.stampRed
)

/**
 * The single app theme. Fixed light/dark schemes — never dynamic color
 * (the design forbids it: "never ask for Material 3 dynamic colour").
 */
@Composable
fun TransportAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val transportColors = if (darkTheme) DarkTransportColors else LightTransportColors

    CompositionLocalProvider(LocalTransportColors provides transportColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TransportM3Typography,
            shapes = TransportShapes,
            content = content
        )
    }
}

/** Convenience accessor for the custom transport colors inside a theme. */
@Composable
fun transportColors(): TransportColors = LocalTransportColors.current