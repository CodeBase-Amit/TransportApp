package com.example.transportapp.core.designsystem.component

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors

/**
 * Small top app bar (Design.md §B3): 64dp, surface, no elevation, no divider.
 * Built as a plain row — fully under the design's control.
 *
 * @param title The title text in titleLarge
 * @param navigationIcon Leading icon (arrow_back/close). Pass null for none.
 * @param onNavigationClick Click for the leading icon
 * @param trailingIcons Any trailing content (icon buttons, text button, reserved number, etc.)
 */
@Composable
fun TransportTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = Icons.Rounded.ArrowBack,
    onNavigationClick: (() -> Unit)? = null,
    trailingIcons: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.topAppBarHeight)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (navigationIcon != null) {
            IconButton(onClick = onNavigationClick ?: {}) {
                Icon(navigationIcon, contentDescription = "Navigate back", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Text(
            text = title,
            style = TransportTypeScale.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
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
 * Bottom navigation bar (Design.md §B3): 80dp, surfaceContainer, three destinations.
 * Active destination carries a pill indicator in secondaryContainer.
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
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Icon(
                            dest.icon,
                            contentDescription = dest.label,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
 * Sticky bottom action bar (Design.md §A7): 72dp single action / 88dp multi-action,
 * surfaceContainer, 1dp top border. Content laid out horizontally inside 16dp padding.
 */
@Composable
fun StickyActionBar(
    modifier: Modifier = Modifier,
    multiAction: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
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

/**
 * Extended FAB pill (Design.md §B3): 56dp, primary fill, leading icon + label. "New bilty".
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
 * Offline bar (Design.md §A12): 32dp, haulAmberContainer fill, bodySmall
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
 * Error banner (Design.md §B3): 16dp radius, errorContainer fill, message + optional Retry.
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