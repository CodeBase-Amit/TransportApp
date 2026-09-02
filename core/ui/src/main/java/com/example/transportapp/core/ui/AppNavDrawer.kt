package com.example.transportapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

/**
 * S17 — the app shell (TransportApp.md §6.2: bottom navigation on T4/T7/T12 plus the
 * settings hub reached naturally). The drawer is the single way into the hub graphs once
 * the dev screen map is gone: Work = the three tabs, Business = accountant/manager hubs,
 * Admin = settings. Selecting an item fires [onSelect]; the nav layer owns routing.
 */
enum class DrawerDestination { HOME, REGISTER, VEHICLES, REPORTS, MASTERS, EXPORTS, SETTINGS, ACCOUNT_DATA }

private data class DrawerEntry(val destination: DrawerDestination, val label: String, val icon: ImageVector)

private val WORK_ENTRIES = listOf(
    DrawerEntry(DrawerDestination.HOME, "Home", Icons.Rounded.Home),
    DrawerEntry(DrawerDestination.REGISTER, "Register", Icons.AutoMirrored.Rounded.ListAlt),
    DrawerEntry(DrawerDestination.VEHICLES, "Vehicles", Icons.Rounded.LocalShipping),
)

private val BUSINESS_ENTRIES = listOf(
    DrawerEntry(DrawerDestination.REPORTS, "Reports", Icons.Rounded.Rule),
    DrawerEntry(DrawerDestination.MASTERS, "Masters", Icons.Rounded.Folder),
    DrawerEntry(DrawerDestination.EXPORTS, "Exports", Icons.Rounded.FileDownload),
)

private val ADMIN_ENTRIES = listOf(
    DrawerEntry(DrawerDestination.SETTINGS, "Settings", Icons.Rounded.Settings),
    DrawerEntry(DrawerDestination.ACCOUNT_DATA, "Account & data", Icons.Rounded.Info),
)

@Composable
fun AppNavDrawer(
    drawerState: androidx.compose.material3.DrawerState,
    companyInitials: String,
    companyName: String,
    branchName: String,
    activeDestination: DrawerDestination?,
    onSelect: (DrawerDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(companyInitials, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(companyName.ifEmpty { "TransportApp" }, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(branchName.ifEmpty { "—" }, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DrawerSection(null, WORK_ENTRIES, activeDestination, onSelect)
                DrawerSection("BUSINESS", BUSINESS_ENTRIES, activeDestination, onSelect)
                DrawerSection("ADMIN", ADMIN_ENTRIES, activeDestination, onSelect)
            }
        },
        content = content
    )
}

@Composable
private fun DrawerSection(
    heading: String?,
    entries: List<DrawerEntry>,
    activeDestination: DrawerDestination?,
    onSelect: (DrawerDestination) -> Unit,
) {
    if (heading != null) {
        Text(
            heading,
            style = TransportTypeScale.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 4.dp)
        )
    }
    entries.forEach { entry ->
        NavigationDrawerItem(
            label = { Text(entry.label, style = TransportTypeScale.bodyLarge) },
            icon = { Icon(entry.icon, contentDescription = null) },
            selected = entry.destination == activeDestination,
            onClick = { onSelect(entry.destination) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

/**
 * S17 — debug-only developer access to the screen map. The map was the dev start
 * destination; it now lives behind a long-press on the diagnostics card in T31 so
 * release users can never reach it (AgentChanges D53).
 */
fun drawerIsTab(destination: DrawerDestination): Boolean = destination in setOf(
    DrawerDestination.HOME, DrawerDestination.REGISTER, DrawerDestination.VEHICLES
)

@Composable
fun rememberAppDrawerState() = rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
