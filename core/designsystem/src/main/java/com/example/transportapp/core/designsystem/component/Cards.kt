package com.example.transportapp.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.theme.AppShapes
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.transportColors

/**
 * Content card: surfaceContainerLow, 24dp radius,
 * 1dp outlineVariant border, 20dp padding. `elevated = true` lifts the card with a
 * tinted soft shadow (the app's own green/black, never gray) for the money cards
 * and hero tiles that must read as floating above the desk.
 * Clickable cards get a subtle tonal lift on press.
 * `onLongClick` (with `ExperimentalFoundationApi`) adds the hidden long-press affordance
 * (D53: the debug screen-map entry on T31's diagnostics card).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContentCard(
    modifier: Modifier = Modifier,
    fill: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    contentPadding: Dp = Dimens.cardPaddingStandard,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shadowTint = transportColors().shadowTint
    val base = modifier
        .then(
            if (elevated) Modifier.shadow(
                elevation = 16.dp,
                shape = AppShapes.contentCard,
                ambientColor = shadowTint.copy(alpha = 0.18f),
                spotColor = shadowTint.copy(alpha = 0.12f)
            ) else Modifier
        )
        .then(
            when {
                onClick != null && onLongClick != null -> Modifier.combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                )
                onClick != null -> Modifier.clickable(
                    onClick = onClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                )
                onLongClick != null -> Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                )
                else -> Modifier
            }
        )
    Card(
        modifier = base,
        shape = AppShapes.contentCard,
        colors = CardDefaults.cardColors(
            containerColor = fill,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = border,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = if (onClick != null) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Nested card inside another card: 12dp radius, 12dp padding, surfaceContainer fill.
 * Slightly elevated when clickable for tactile feedback.
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
        if (onClick != null) Modifier.clickable(
            onClick = onClick,
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) else Modifier
    ),
    shape = AppShapes.nestedCard,
    colors = CardDefaults.cardColors(
        containerColor = fill,
        contentColor = MaterialTheme.colorScheme.onSurface
    ),
    border = border,
    elevation = CardDefaults.cardElevation(
        defaultElevation = 0.dp,
        pressedElevation = if (onClick != null) 1.dp else 0.dp
    )
) {
    Column(
        modifier = Modifier.padding(contentPadding),
        content = content
    )
}

/**
 * Paper sheet — 2dp radius, square-ish corners. Used only inside document frames.
 * Paper carries no tonal chips, no pill buttons, and no green.
 * The sheet rests on a soft warmed paper shadow so the bilty reads as a real
 * sheet on the desk (both themes).
 */
@Composable
fun PaperSheet(
    modifier: Modifier = Modifier,
    fill: Color,
    borderColor: Color,
    contentPadding: Dp = Dimens.cardPaddingNested,
    content: @Composable ColumnScope.() -> Unit
) {
    val paperShadow = transportColors().paperShadow
    Card(
        modifier = modifier.shadow(
            elevation = 12.dp,
            shape = AppShapes.paper,
            ambientColor = paperShadow.copy(alpha = 0.25f),
            spotColor = paperShadow.copy(alpha = 0.18f),
        ),
        shape = AppShapes.paper,
        colors = CardDefaults.cardColors(containerColor = fill),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
