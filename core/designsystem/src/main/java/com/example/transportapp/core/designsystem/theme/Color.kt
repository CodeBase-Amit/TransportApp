package com.example.transportapp.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ── Day Shift (light) ──────────────────────────────────────────────────
// Material 3 refined palette with enhanced vibrancy and better contrast.
// Green base maintains the transport/logistics identity while improving visual hierarchy.
object LightColors {
    // Primary — refined deep forest green with better vibrancy
    val primary = Color(0xFF0D5F43)
    val onPrimary = Color(0xFFFFFFFF)
    val primaryContainer = Color(0xFFA8E6C8)
    val onPrimaryContainer = Color(0xFF001E14)

    // Secondary — balanced warm gray-green for supporting accents
    val secondary = Color(0xFF3F5147)
    val onSecondary = Color(0xFFFFFFFF)
    val secondaryContainer = Color(0xFFC1DACB)
    val onSecondaryContainer = Color(0xFF0D1F17)

    // Tertiary — complementary accent for alternative actions
    val tertiary = Color(0xFF106B54)
    val onTertiary = Color(0xFFFFFFFF)
    val tertiaryContainer = Color(0xFF7FF8D8)
    val onTertiaryContainer = Color(0xFF002018)

    // Error — warm red maintained for critical states
    val error = Color(0xFFA32A1F)
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFFFADAD5)
    val onErrorContainer = Color(0xFF3B0A05)

    // Surface system — refined with better depth separation
    val surface = Color(0xFFF6F9F7)
    val onSurface = Color(0xFF191D1B)
    val surfaceVariant = Color(0xFFDCE5DE)
    val onSurfaceVariant = Color(0xFF3F5147)

    val surfaceContainerLowest = Color(0xFFFFFFFF)
    val surfaceContainerLow = Color(0xFFF0F4F1)
    val surfaceContainer = Color(0xFFE8ECE9)
    val surfaceContainerHigh = Color(0xFFE2E6E3)
    val surfaceContainerHighest = Color(0xFFDCE0DD)

    val outline = Color(0xFF6B7A73)
    val outlineVariant = Color(0xFFC0C9C1)

    val inverseSurface = Color(0xFF2D3230)
    val inverseOnSurface = Color(0xFFEFF1EE)
    val inversePrimary = Color(0xFF8DDCB0)

    val surfaceTint = Color(0xFF0D5F43)
    val background = surface
    val onBackground = onSurface

    // Custom chrome colors — transport-specific
    val haulAmber = Color(0xFF995500)
    val haulAmberContainer = Color(0xFFFFDDB5)
    val onHaulAmber = Color(0xFFFFFFFF)
    val deliveredContainer = Color(0xFF9ECDB8)

    // ── Expressive accents ─────────────────────────────────────────────
    // Sunrise coral — celebratory moments (booking saved, payment collected)
    val sunrise = Color(0xFFF05632)
    val onSunrise = Color(0xFFFFFFFF)
    val sunriseContainer = Color(0xFFFFECE7)
    val onSunriseContainer = Color(0xFF4A1000)
    // Tinted shadow — green-based for refined depth
    val shadowTint = Color(0xFF0D5F43)
    // Warmed paper shadow — printed documents read as real sheets
    val paperShadow = Color(0x1A2A2520)
}

// ── Night Shift (dark) ──────────────────────────────────────────────────
// Material 3 dark scheme with enhanced depth and refined contrast ratios.
// Rich green undertones maintained while avoiding pure black.
object DarkColors {
    // Primary — luminous mint for dark backgrounds
    val primary = Color(0xFF8DDCB0)
    val onPrimary = Color(0xFF003824)
    val primaryContainer = Color(0xFF0B5A41)
    val onPrimaryContainer = Color(0xFF9BF4C9)

    // Secondary — softer sage for supporting elements
    val secondary = Color(0xFFC5DACB)
    val onSecondary = Color(0xFF16241E)
    val secondaryContainer = Color(0xFF3A544A)
    val onSecondaryContainer = Color(0xFFDCF0E2)

    // Tertiary — vibrant emerald for alternative accents
    val tertiary = Color(0xFF82E5BD)
    val onTertiary = Color(0xFF002018)
    val tertiaryContainer = Color(0xFF004D3A)
    val onTertiaryContainer = Color(0xFF91F8D0)

    // Error — vibrant red for critical states
    val error = Color(0xFFFFB4A8)
    val onError = Color(0xFF3B0A05)
    val errorContainer = Color(0xFF8C1D12)
    val onErrorContainer = Color(0xFFFFDAD4)

    // Surface system — rich depth with subtle green undertones
    val surface = Color(0xFF111513)
    val onSurface = Color(0xFFE2E8E3)
    val surfaceVariant = Color(0xFF314237)
    val onSurfaceVariant = Color(0xFFC5DACB)

    val surfaceContainerLowest = Color(0xFF0C100E)
    val surfaceContainerLow = Color(0xFF1A1F1C)
    val surfaceContainer = Color(0xFF1E2420)
    val surfaceContainerHigh = Color(0xFF283029)
    val surfaceContainerHighest = Color(0xFF334237)

    val outline = Color(0xFF889A90)
    val outlineVariant = Color(0xFF425448)

    val inverseSurface = Color(0xFFE2E8E3)
    val inverseOnSurface = Color(0xFF2D3230)
    val inversePrimary = Color(0xFF0D5F43)

    val surfaceTint = Color(0xFF8DDCB0)
    val background = surface
    val onBackground = onSurface

    // Custom chrome — transport-specific
    val haulAmber = Color(0xFFFFB940)
    val haulAmberContainer = Color(0xFF4A3200)
    val onHaulAmber = Color(0xFFFFFFFF)
    val deliveredContainer = Color(0xFF0F7B58)

    // ── Expressive accents ─────────────────────────────────────────────
    val sunrise = Color(0xFFFF8A66)
    val onSunrise = Color(0xFF3B0B00)
    val sunriseContainer = Color(0xFF5A2A18)
    val onSunriseContainer = Color(0xFFFFD9CC)
    // Deep green shadow for rich depth in dark mode
    val shadowTint = Color(0xFF0A0E0C)
    val paperShadow = Color(0x402A2520)
}

// ── Paper palette ──────────────────────────────────────────────────────
// Used only inside frames depicting printed documents. Never in chrome.
// Paper does not invert in dark theme — maintains printed document realism.
object PaperColors {
    val paperWhite = Color(0xFFFDFCF9)
    val paperPink = Color(0xFFFCE7EA)
    val paperYellow = Color(0xFFFBF3D6)
    val paperGreen = Color(0xFFE9F3E6)
    val paperInk = Color(0xFF111111)
    val paperRule = Color(0xFF9A9A9A)
    val stampViolet = Color(0xFF5B2A86)
    val stampRed = Color(0xFFA81E1E)
}