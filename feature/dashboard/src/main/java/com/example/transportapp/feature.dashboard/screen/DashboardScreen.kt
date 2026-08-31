package com.example.transportapp.feature.dashboard.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.NavDestination
import com.example.transportapp.core.designsystem.component.TransportBottomNavBar
import com.example.transportapp.core.designsystem.theme.PlexMonoFamily
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

/**
 * T4 — Dashboard. Exception strip above the numbers, 2-column tiles, one sparkline.
 * Phase2 S2: header reads the active company/branch; tiles arrive from §13 queries in S10.
 */
@Composable
fun DashboardScreen(
    onNewBilty: () -> Unit,
    onRegister: () -> Unit,
    onVehicles: () -> Unit,
    onOpenScreenMap: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    DashboardContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNewBilty = onNewBilty,
        onRegister = onRegister,
        onVehicles = onVehicles,
        onOpenScreenMap = onOpenScreenMap,
    )
}

@Composable
fun DashboardContent(
    state: DashboardUiState,
    onEvent: (DashboardEvent) -> Unit,
    onNewBilty: () -> Unit,
    onRegister: () -> Unit,
    onVehicles: () -> Unit,
    onOpenScreenMap: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top app bar
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Text(state.companyInitials, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.companyName, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(state.branchName, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {}) { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onOpenScreenMap) { Icon(Icons.Rounded.Person, contentDescription = "Screen map", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            Text(
                state.asOf,
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                textAlign = TextAlign.End
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    state.visibleExceptions.forEachIndexed { visibleIndex, exc ->
                        val index = state.exceptions.indexOfFirst { it == exc }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp)).padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(if (exc.isLate) Icons.Rounded.Schedule else Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exc.title, style = TransportTypeScale.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text(exc.body, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                            IconButton(onClick = { onEvent(DashboardEvent.DismissException(index)) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                state.tiles.chunked(2).forEach { rowTiles ->
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowTiles.forEach { tile ->
                                DashboardTile(tile, modifier = Modifier.weight(1f))
                            }
                            if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                item { ThisMonthTile(state) }
            }
        }

        // Extended FAB
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 96.dp)) {
            AppPrimaryButton(state.newBiltyLabel, onClick = onNewBilty, leadingIcon = Icons.Rounded.Add)
        }

        // Bottom nav
        TransportBottomNavBar(
            destinations = listOf(
                NavDestination("Home", Icons.Outlined.Home, Icons.Rounded.Home),
                NavDestination("Register", Icons.Outlined.ListAlt, Icons.Outlined.ListAlt),
                NavDestination("Vehicles", Icons.Outlined.LocalShipping, Icons.Rounded.LocalShipping)
            ),
            activeIndex = 0,
            onSelect = { index -> when (index) { 1 -> onRegister(); 2 -> onVehicles() } },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun DashboardTile(tile: DashTile, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.height(116.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(tile.label.uppercase(), style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(tile.value, style = if (tile.money) TransportTypeScale.dataLarge else TransportTypeScale.titleLarge, fontFamily = if (tile.money) PlexMonoFamily else null, color = MaterialTheme.colorScheme.onSurface)
        Text(tile.qualifier, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ThisMonthTile(state: DashboardUiState) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier.fillMaxWidth().height(132.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("THIS MONTH", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(state.thisMonthDelta, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(percent = 100)).padding(horizontal = 8.dp, vertical = 2.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            state.thisMonthFigures.forEach { (value, label) ->
                Column(modifier = Modifier.weight(1f)) {
                    Text(value, style = TransportTypeScale.dataMedium, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface)
                    Text(label, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Canvas(modifier = Modifier.width(64.dp).height(32.dp)) {
                val points = listOf(0.85f, 0.75f, 0.8f, 0.6f, 0.7f, 0.5f, 0.55f, 0.35f, 0.45f, 0.25f)
                val step = size.width / (points.size - 1)
                points.forEachIndexed { i, p ->
                    if (i > 0) {
                        drawLine(primary, start = Offset((i - 1) * step, size.height * points[i - 1]), end = Offset(i * step, size.height * p), strokeWidth = 2.dp.toPx())
                    }
                }
            }
        }
    }
}
