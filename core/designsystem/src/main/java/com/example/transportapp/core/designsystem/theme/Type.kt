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

private fun mono(weight: FontWeight, size: Int, line: Int, tracking: Int = 0): TextStyle =
    TextStyle(
        fontFamily = PlexMonoFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp
    )

/**
 * Material 3 refined type scale for Transport app.
 * Enhanced hierarchy with improved line heights and tracking for better readability.
 * Maintains the transport identity with Anek Latin for display, IBM Plex for body/data.
 */
object TransportTypeScale {
    // Display — hero headlines and large numbers
    val displaySmall: TextStyle = anek(FontWeight.Bold, 36, 42, -1)
    val displayHeroMoney: TextStyle = mono(FontWeight.Medium, 36, 42, 0)

    // Headline — section headers, screen titles
    val headlineMedium: TextStyle = anek(FontWeight.SemiBold, 28, 36, -1)
    val headlineSmall: TextStyle = anek(FontWeight.SemiBold, 24, 32, 0)

    // Title — card titles, subsection headers
    val titleLarge: TextStyle = anek(FontWeight.SemiBold, 22, 28, 0)
    val titleMedium: TextStyle = plex(FontWeight.SemiBold, 16, 24, 0)
    val titleSmall: TextStyle = plex(FontWeight.SemiBold, 14, 20, 1)

    // Body — primary reading text
    val bodyLarge: TextStyle = plex(FontWeight.Normal, 16, 26, 0)
    val bodyMedium: TextStyle = plex(FontWeight.Normal, 14, 22, 0)
    val bodySmall: TextStyle = plex(FontWeight.Normal, 12, 18, 0)

    // Label — buttons, chips, captions, navigation
    val labelLarge: TextStyle = plex(FontWeight.Medium, 14, 20, 1)
    val labelMedium: TextStyle = plex(FontWeight.Medium, 12, 16, 1)

    // Data — figures, monetary values, identifiers (monospace for alignment)
    val dataLarge: TextStyle = mono(FontWeight.Medium, 20, 26)
    val dataMedium: TextStyle = mono(FontWeight.Normal, 15, 22)
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