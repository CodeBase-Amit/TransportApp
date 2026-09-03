package com.example.transportapp.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.HaulMotion
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors

/**
 * Small top app bar: 64dp, surface, no elevation, no divider.
 * Built as a plain row — fully under the design's control.
 */
@Composable
fun TransportTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = Icons.Rounded.ArrowBack,
    navigationIconDesc: String = "Navigate back",
    onNavigationClick: (() -> Unit)? = null,
    trailingIcons: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.topAppBarHeight)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (navigationIcon != null) {
            IconButton(onClick = onNavigationClick ?: {}) {
                Icon(
                    navigationIcon,
                    contentDescription = navigationIconDesc,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = title,
            style = TransportTypeScale.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = if (navigationIcon != null) 4.dp else 16.dp)
        )
        trailingIcons?.invoke()
    }
}

/** A bottom-nav destination definition. */
data class NavDestination(
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector
)

/**
 * Bottom navigation bar: 80dp, surfaceContainer, three destinations.
 * Active destination carries a pill indicator in secondaryContainer with animated color transitions.
 */
@Composable
fun TransportBottomNavBar(
    destinations: List<NavDestination>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        destinations.forEachIndexed { index, dest ->
            val isActive = index == activeIndex
            NavigationBarItem(
                selected = isActive,
                onClick = { onSelect(index) },
                icon = {
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(32.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(percent = 100)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                dest.activeIcon,
                                contentDescription = dest.label,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Icon(
                            dest.icon,
                            contentDescription = dest.label,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        dest.label,
                        style = TransportTypeScale.labelMedium,
                        color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Sticky bottom action bar: 72dp single action / 88dp multi-action,
 * surfaceContainer, 1dp top divider. Content laid out horizontally inside 16dp padding.
 */
@Composable
fun StickyActionBar(
    modifier: Modifier = Modifier,
    multiAction: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (multiAction) 88.dp else 72.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                content()
            }
        }
    }
}

/**
 * Extended FAB pill: 56dp, primary fill, leading icon + label. "New bilty".
 */
@Composable
fun TransportExtendedFab(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Add
) {
    AppPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier.height(Dimens.extendedFabHeight),
        leadingIcon = icon
    )
}

/**
 * Offline bar: 32dp, haulAmberContainer fill, bodySmall.
 * "Offline — N changes will sync when you reconnect".
 */
@Composable
fun OfflineBar(
    unsyncedCount: Int,
    modifier: Modifier = Modifier,
    alternate: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.offlineBarHeight)
            .background(transportColors().haulAmberContainer)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = alternate ?: "Offline — $unsyncedCount changes will sync when you reconnect",
            style = TransportTypeScale.bodySmall,
            color = transportColors().onHaulAmber,
            maxLines = 1
        )
    }
}

/**
 * Error banner: 16dp radius, errorContainer fill, message + optional Retry.
 */
@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = message,
            style = TransportTypeScale.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
            maxLines = 3
        )
        if (onRetry != null) {
            AppTextButton("Try again", onClick = onRetry, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}