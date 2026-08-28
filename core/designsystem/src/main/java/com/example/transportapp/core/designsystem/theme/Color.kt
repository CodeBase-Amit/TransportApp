package com.example.transportapp.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ── Day Shift (light) ──────────────────────────────────────────────────
// Exact hex values from Design.md §A3. The neutrals are green-cast on purpose.
object LightColors {
    val primary = Color(0xFF0E4D38)
    val onPrimary = Color(0xFFFFFFFF)
    val primaryContainer = Color(0xFFC3DDCF)
    val onPrimaryContainer = Color(0xFF04281B)

    val secondary = Color(0xFF42504A)
    val onSecondary = Color(0xFFFFFFFF)
    val secondaryContainer = Color(0xFFD3E2DA)
    val onSecondaryContainer = Color(0xFF16241E)

    val tertiary = Color(0xFF0B3424)
    val onTertiary = Color(0xFFFFFFFF)
    val tertiaryContainer = Color(0xFF254B3A)
    val onTertiaryContainer = Color(0xFF91BAA4)

    val error = Color(0xFFA32A1F)
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFFFADAD5)
    val onErrorContainer = Color(0xFF3B0A05)

    val surface = Color(0xFFF1F5F1)
    val onSurface = Color(0xFF1B2620)
    val surfaceVariant = Color(0xFFDFE3E0)
    val onSurfaceVariant = Color(0xFF42504A)

    val surfaceContainerLowest = Color(0xFFFFFFFF)
    val surfaceContainerLow = Color(0xFFE9EEEA)
    val surfaceContainer = Color(0xFFE2E8E3)
    val surfaceContainerHigh = Color(0xFFDAE1DC)
    val surfaceContainerHighest = Color(0xFFD3DBD5)

    val outline = Color(0xFF71807A)
    val outlineVariant = Color(0xFFBFC9C3)

    val inverseSurface = Color(0xFF2B3831)
    val inverseOnSurface = Color(0xFFEDF2EE)
    val inversePrimary = Color(0xFF7FD8AE)

    val surfaceTint = Color(0xFF0E4D38)
    val background = surface
    val onBackground = onSurface

    // Custom chrome colors
    val haulAmber = Color(0xFF8A5A00)
    val haulAmberContainer = Color(0xFFF7DFA6)
    val onHaulAmber = Color(0xFF4A3200)
    val deliveredContainer = Color(0xFFA9D3BC)
}

// ── Night Haul (dark) ──────────────────────────────────────────────────
// Exact hex values from Design.md §A4. Still green-cast. Never pure black.
object DarkColors {
    val primary = Color(0xFF7FD8AE)
    val onPrimary = Color(0xFF003824)
    val primaryContainer = Color(0xFF0B5A41)
    val onPrimaryContainer = Color(0xFF9BF4C9)

    val secondary = Color(0xFFBAC5BE)
    val onSecondary = Color(0xFF16241E)
    val secondaryContainer = Color(0xFF33463D)
    val onSecondaryContainer = Color(0xFFCFE9DC)

    val tertiary = Color(0xFF91BAA4)
    val onTertiary = Color(0xFF002114)
    val tertiaryContainer = Color(0xFF254B3A)
    val onTertiaryContainer = Color(0xFF91BAA4)

    val error = Color(0xFFFFB4A8)
    val onError = Color(0xFF3B0A05)
    val errorContainer = Color(0xFF7A1B12)
    val onErrorContainer = Color(0xFFFFDAD4)

    val surface = Color(0xFF101512)
    val onSurface = Color(0xFFE1E7E2)
    val surfaceVariant = Color(0xFF2D362F)
    val onSurfaceVariant = Color(0xFFBAC5BE)

    val surfaceContainerLowest = Color(0xFF0B0F0D)
    val surfaceContainerLow = Color(0xFF171D1A)
    val surfaceContainer = Color(0xFF1B221E)
    val surfaceContainerHigh = Color(0xFF242C27)
    val surfaceContainerHighest = Color(0xFF2D362F)

    val outline = Color(0xFF849089)
    val outlineVariant = Color(0xFF3C4741)

    val inverseSurface = Color(0xFFE1E7E2)
    val inverseOnSurface = Color(0xFF2B3831)
    val inversePrimary = Color(0xFF0E4D38)

    val surfaceTint = Color(0xFF7FD8AE)
    val background = surface
    val onBackground = onSurface

    val haulAmber = Color(0xFFF0C46A)
    val haulAmberContainer = Color(0xFF4A3200)
    val onHaulAmber = Color(0xFFF7DFA6)
    val deliveredContainer = Color(0xFF0F6B48)
}

// ── Paper palette ──────────────────────────────────────────────────────
// Used only inside frames depicting printed documents. Never in chrome.
// Per Design.md §A2: paper does not invert in dark theme.
object PaperColors {
    val paperWhite = Color(0xFFFFFFFF)
    val paperPink = Color(0xFFFCE7EA)
    val paperYellow = Color(0xFFFBF3D6)
    val paperGreen = Color(0xFFE9F3E6)
    val paperInk = Color(0xFF111111)
    val paperRule = Color(0xFF9A9A9A)
    val stampViolet = Color(0xFF5B2A86)
    val stampRed = Color(0xFFA81E1E)
}