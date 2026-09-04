package com.example.transportapp.core.designsystem.component

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

/**
 * Loading state — static blocks shaped like the real rows (Design.md §B3).
 * S20 (D57): the block breathes — a slow green pulse, no shimmer, no spinner.
 */
@Composable
fun LoadingBlock(
    modifier: Modifier = Modifier,
    height: Int = 88,
    radius: Int = 12,
    shape: RoundedCornerShape = RoundedCornerShape(radius.dp)
) {
    val transition = rememberInfiniteTransition(label = "skeletonPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "skeletonPulseAlpha",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha),
                shape
            )
    )
}

/**
 * Loading column — a stack of static blocks at the same spacing as the real rows.
 */
@Composable
fun LoadingList(
    count: Int = 4,
    rowHeight: Int = 88,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.fieldGap)
    ) {
        repeat(count) {
            LoadingBlock(height = rowHeight)
        }
    }
}

/**
 * Empty state (Design.md §B3): centred 64dp line-art icon, headlineSmall title,
 * one bodyMedium line, one filled pill.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Text(
            title,
            style = TransportTypeScale.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            body,
            style = TransportTypeScale.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        AppPrimaryButton(buttonText, onClick = onButtonClick, modifier = Modifier.padding(top = 8.dp))
        if (secondaryButtonText != null && onSecondaryClick != null) {
            AppTextButton(secondaryButtonText, onClick = onSecondaryClick)
        }
    }
}

/**
 * S20 (D57) — the expressive empty state: a line-art illustration built from the app's
 * own motifs (a 200dp wide route line with ticks), a headline that invites rather than
 * apologises, and the primary CTA in the thumb zone. Every empty screen is an
 * instruction, never a dead end.
 */
@Composable
fun EmptyStateIllustrated(
    title: String,
    body: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val routeColor = MaterialTheme.colorScheme.outlineVariant
        // The route-line motif: origin tick, dashed stretch, hollow destination.
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 64.dp)
                .height(40.dp)
        ) {
            val y = size.height / 2
            val tick = 8.dp.toPx()
            val lineColor = routeColor
            // origin tick
            drawLine(lineColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(tick, y), strokeWidth = 4f)
            // dashed stretch
            var x = tick + 12f
            while (x < size.width - tick - 12f) {
                drawLine(lineColor.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Offset(x + 16f, y), strokeWidth = 2f)
                x += 28f
            }
            // hollow destination
            drawCircle(lineColor, radius = tick, center = androidx.compose.ui.geometry.Offset(size.width - tick, y), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
        }
        Text(
            title,
            style = TransportTypeScale.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            body,
            style = TransportTypeScale.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        AppPrimaryButton(buttonText, onClick = onButtonClick, modifier = Modifier.padding(top = 8.dp))
        if (secondaryButtonText != null && onSecondaryClick != null) {
            AppTextButton(secondaryButtonText, onClick = onSecondaryClick)
        }
    }
}

/**
 * Error state (Design.md §B3): centred 48dp outlined icon in error, headlineSmall title,
 * plain-language cause, "Try again" filled pill + "Get help" text button.
 */
@Composable
fun ErrorState(
    title: String,
    body: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onHelp: (() -> Unit)? = null,
    icon: ImageVector = Icons.Rounded.ErrorOutline
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Text(title, style = TransportTypeScale.headlineSmall, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Text(
            body,
            style = TransportTypeScale.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        AppPrimaryButton("Try again", onClick = onRetry, modifier = Modifier.padding(top = 8.dp))
        if (onHelp != null) {
            AppTextButton("Get help", onClick = onHelp)
        }
    }
}

/**
 * Avatar with initials (Design.md §B3): 40dp circular, initials in Anek titleLarge.
 */
@Composable
fun InitialsAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Int = 40,
    fill: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    labelColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(fill, RoundedCornerShape(percent = 100)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initials,
            style = TransportTypeScale.titleLarge.copy(
                fontSize = androidx.compose.ui.unit.TextUnit(if (size >= 48) 16f else 14f, androidx.compose.ui.unit.TextUnitType.Sp)
            ),
            color = labelColor
        )
    }
}