package com.example.transportapp.core.designsystem.component

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

/**
 * Loading state — static blocks shaped like the real rows (Design.md §B3).
 * Never a shimmer and a spinner together.
 */
@Composable
fun LoadingBlock(
    modifier: Modifier = Modifier,
    height: Int = 88,
    radius: Int = 12,
    shape: RoundedCornerShape = RoundedCornerShape(radius.dp)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
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