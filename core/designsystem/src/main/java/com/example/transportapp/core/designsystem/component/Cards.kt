package com.example.transportapp.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.theme.AppShapes
import com.example.transportapp.core.designsystem.theme.Dimens

/**
 * Content card (Design.md §A7): surfaceContainerLow, 20dp radius, 1dp outlineVariant border,
 * 20dp padding, NO shadow. Tonal elevation only — shadow is banned everywhere except the
 * four-copy stack on T6.
 */
@Composable
fun ContentCard(
    modifier: Modifier = Modifier,
    fill: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    border: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    contentPadding: Dp = Dimens.cardPaddingStandard,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) = Card(
    modifier = modifier.then(
        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = fill, contentColor = MaterialTheme.colorScheme.onSurface),
    border = border,
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(contentPadding),
        content = content
    )
}

/**
 * Nested card inside another card: 12dp radius, 12dp padding, surfaceContainer fill.
 */
@Composable
fun NestedCard(
    modifier: Modifier = Modifier,
    fill: Color = MaterialTheme.colorScheme.surfaceContainer,
    border: BorderStroke? = null,
    contentPadding: Dp = Dimens.cardPaddingNested,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) = Card(
    modifier = modifier.then(
        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = fill),
    border = border,
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(contentPadding),
        content = content
    )
}

/**
 * Paper sheet — 2dp radius, square-ish corners. Used only inside document frames.
 * Paper carries no tonal chips, no pill buttons, and no green.
 */
@Composable
fun PaperSheet(
    modifier: Modifier = Modifier,
    fill: Color,
    borderColor: Color,
    contentPadding: Dp = Dimens.cardPaddingNested,
    content: @Composable ColumnScope.() -> Unit
) = Card(
    modifier = modifier,
    shape = AppShapes.paper,
    colors = CardDefaults.cardColors(containerColor = fill),
    border = BorderStroke(1.dp, borderColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(contentPadding),
        content = content
    )
}