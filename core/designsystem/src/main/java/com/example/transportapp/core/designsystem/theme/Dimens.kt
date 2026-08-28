package com.example.transportapp.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing, dimension tokens and row-height ladder (Design.md §A7).
 * All on a 4dp grid.
 */
object Dimens {
    // Grid unit
    val grid = 4.dp

    // Screen padding
    val screenPadding = 16.dp

    // Section spacing
    val sectionSpacing = 24.dp
    val fieldGap = 12.dp
    val chipGap = 8.dp

    // Card padding
    val cardPaddingStandard = 20.dp
    val cardPaddingNested = 12.dp

    // Row height ladder
    val rowHeader: Dp = 40.dp
    val rowTable: Dp = 48.dp
    val rowSingle: Dp = 56.dp
    val rowDouble: Dp = 72.dp
    val rowDocket: Dp = 88.dp

    // App bars
    val topAppBarHeight = 64.dp
    val bottomNavHeight = 80.dp
    val stickyBarSingle = 72.dp
    val stickyBarMulti = 88.dp
    val extendedFabHeight = 56.dp
    val offlineBarHeight = 32.dp

    // Chip heights
    val journeyChipHeight = 24.dp
    val filterChipHeight = 32.dp
    val paymentStampHeight = 24.dp
    val syncChipHeight = 20.dp
    val segmentedButtonHeight = 40.dp

    // Primary button height
    val primaryButtonHeight = 56.dp

    // Route line
    val routeLineThickness = 2.dp
    val routeTickSize = 8.dp
    val routeTruckSize = 20.dp
    val routeCurrentHalo = 20.dp

    // Summary strip
    val summaryStripHeight = 64.dp

    // Paper stack
    val paperStackOffset = 6.dp
    val paperShadowOffset = 1.dp

    // Avatar
    val avatarSmall = 40.dp
    val avatarMedium = 48.dp
    val avatarLarge = 72.dp
    val avatarBadge = 28.dp

    // Thumbnail
    val thumbnailSmall = 96.dp
    val thumbnailLarge = 132.dp

    // Tile
    val tileHeight = 116.dp
    val tileFullWidthHeight = 132.dp
}