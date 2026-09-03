package com.example.transportapp.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Radius by nesting depth (Design.md §A7, S20 expressive bump).
 * Depth is legible without shadows because radius decreases as elements nest deeper.
 * S20 (D57): cards and sheets grow more generous; the paper sheet stays the hardest
 * corner in the app — the hard/soft contrast between chrome and artifact is the identity.
 */
object AppShapes {
    // Paper sheet — the hardest corner in the app
    val paper = RoundedCornerShape(2.dp)
    // Payment stamp — semi-hard rectangle
    val stamp = RoundedCornerShape(4.dp)
    // Text field, nested card
    val field = RoundedCornerShape(12.dp)
    val nestedCard = RoundedCornerShape(12.dp)
    // Banner or inline alert
    val banner = RoundedCornerShape(16.dp)
    // Content card sitting on the screen
    val contentCardRadius = 24.dp
    val contentCard = RoundedCornerShape(contentCardRadius)
    // Bottom sheet and dialog
    val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val dialog = RoundedCornerShape(32.dp)
    // Chips, buttons, FAB — fully rounded pill
    val pill = RoundedCornerShape(percent = 100)
}

/** Material 3 Shapes mapping — matches the AppShapes ladder. */
val TransportShapes = Shapes(
    extraSmall = AppShapes.paper,         // 2dp
    small = AppShapes.stamp,               // 4dp
    medium = AppShapes.field,              // 12dp
    large = AppShapes.contentCard,         // 20dp
    extraLarge = AppShapes.sheet,          // 28dp
)