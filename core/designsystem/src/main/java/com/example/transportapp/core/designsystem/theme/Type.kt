package com.example.transportapp.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.transportapp.core.designsystem.R

/**
 * Three faces, three jobs (Design.md §A6):
 *  - Anek Latin for display/headings
 *  - IBM Plex Sans for body/labels
 *  - IBM Plex Mono for figures and identifiers
 *
 * Fonts are delivered via the Google Fonts provider (Google Play Services) with the system
 * face as fallback. Devanagari gets extra line height via the fonts' own metrics.
 */
private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private fun gf(name: String, weight: FontWeight) = Font(
    googleFont = GoogleFont(name),
    fontProvider = fontProvider,
    weight = weight
)

/** Anek Latin — display and headings. Falls back to the system sans when unavailable. */
val AnekFamily = FontFamily(
    gf("Anek Latin", FontWeight.Normal),
    gf("Anek Latin", FontWeight.SemiBold),
    gf("Anek Latin", FontWeight.Bold)
)

/** IBM Plex Sans — body and labels. Falls back to the system sans when unavailable. */
val PlexSansFamily = FontFamily(
    gf("IBM Plex Sans", FontWeight.Normal),
    gf("IBM Plex Sans", FontWeight.Medium),
    gf("IBM Plex Sans", FontWeight.SemiBold)
)

/** IBM Plex Mono — figures and identifiers. Falls back to the system mono when unavailable. */
val PlexMonoFamily = FontFamily(
    gf("IBM Plex Mono", FontWeight.Normal),
    gf("IBM Plex Mono", FontWeight.Medium)
)

private fun anek(weight: FontWeight, size: Int, line: Int, tracking: Int = 0): TextStyle =
    TextStyle(
        fontFamily = AnekFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp
    )

private fun plex(weight: FontWeight, size: Int, line: Int, tracking: Int = 0): TextStyle =
    TextStyle(
        fontFamily = PlexSansFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp
    )

private fun mono(weight: FontWeight, size: Int, line: Int): TextStyle =
    TextStyle(
        fontFamily = PlexMonoFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = 0.sp
    )

/** The fourteen-step type scale plus three named data styles (Design.md §A6.1). */
object TransportTypeScale {
    val displaySmall: TextStyle = anek(FontWeight.Bold, 34, 40, -1)
    val headlineMedium: TextStyle = anek(FontWeight.SemiBold, 28, 34, -1)
    val headlineSmall: TextStyle = anek(FontWeight.SemiBold, 24, 30)
    val titleLarge: TextStyle = anek(FontWeight.SemiBold, 22, 28)
    val titleMedium: TextStyle = plex(FontWeight.SemiBold, 16, 22)
    val titleSmall: TextStyle = plex(FontWeight.SemiBold, 14, 20, 1)
    val bodyLarge: TextStyle = plex(FontWeight.Normal, 16, 24)
    val bodyMedium: TextStyle = plex(FontWeight.Normal, 14, 20)
    val bodySmall: TextStyle = plex(FontWeight.Normal, 12, 16)
    val labelLarge: TextStyle = plex(FontWeight.Medium, 14, 20)
    val labelMedium: TextStyle = plex(FontWeight.Medium, 12, 16)
    val dataLarge: TextStyle = mono(FontWeight.Medium, 20, 26)
    val dataMedium: TextStyle = mono(FontWeight.Normal, 15, 20)
    val dataSmall: TextStyle = mono(FontWeight.Normal, 12, 16)
}

/** Material 3 Typography mapping from the transport scale. */
val TransportM3Typography = Typography(
    displaySmall = TransportTypeScale.displaySmall,
    headlineMedium = TransportTypeScale.headlineMedium,
    headlineSmall = TransportTypeScale.headlineSmall,
    titleLarge = TransportTypeScale.titleLarge,
    titleMedium = TransportTypeScale.titleMedium,
    titleSmall = TransportTypeScale.titleSmall,
    bodyLarge = TransportTypeScale.bodyLarge,
    bodyMedium = TransportTypeScale.bodyMedium,
    bodySmall = TransportTypeScale.bodySmall,
    labelLarge = TransportTypeScale.labelLarge,
    labelMedium = TransportTypeScale.labelMedium,
    labelSmall = TransportTypeScale.bodySmall
)