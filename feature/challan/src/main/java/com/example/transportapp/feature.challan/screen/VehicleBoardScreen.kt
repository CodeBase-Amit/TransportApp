package com.example.transportapp.feature.challan.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.FilterChip
import com.example.transportapp.core.designsystem.component.NavDestination
import com.example.transportapp.core.designsystem.component.TransportBottomNavBar
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.ui.AppNavDrawer
import com.example.transportapp.core.ui.DrawerDestination
import com.example.transportapp.core.ui.rememberAppDrawerState
import kotlinx.coroutines.launch

/**
 * T12 (Phase2.md S7): every vehicle with its current trip. S17 adds the app shell —
 * hamburger drawer and the design-mandated bottom navigation (Design.md T4/T7/T12).
 */
@Composable
fun VehicleBoardScreen(
    onNewChallan: () -> Unit,
    onHome: () -> Unit,
    onRegister: () -> Unit,
    onReports: () -> Unit,
    onMasters: () -> Unit,
    onExports: () -> Unit,
    onSettings: () -> Unit,
    onAccountData: () -> Unit,
    viewModel: VehicleBoardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    VehicleBoardContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNewChallan = onNewChallan,
        onHome = onHome,
        onRegister = onRegister,
        onReports = onReports,
        onMasters = onMasters,
        onExports = onExports,
        onSettings = onSettings,
        onAccountData = onAccountData
    )
}

@Composable
fun VehicleBoardContent(
    state: VehicleBoardUiState,
    onEvent: (VehicleBoardEvent) -> Unit,
    onNewChallan: () -> Unit,
    onHome: () -> Unit,
    onRegister: () -> Unit,
    onReports: () -> Unit,
    onMasters: () -> Unit,
    onExports: () -> Unit,
    onSettings: () -> Unit,
    onAccountData: () -> Unit
) {
    val drawerState = rememberAppDrawerState()
    val scope = rememberCoroutineScope()
    AppNavDrawer(
        drawerState = drawerState,
        companyInitials = state.companyInitials,
        companyName = state.companyName,
        branchName = state.branchName,
        activeDestination = DrawerDestination.VEHICLES,
        onSelect = { destination ->
            scope.launch { drawerState.close() }
            when (destination) {
                DrawerDestination.HOME -> onHome()
                DrawerDestination.REGISTER -> onRegister()
                DrawerDestination.REPORTS -> onReports()
                DrawerDestination.MASTERS -> onMasters()
                DrawerDestination.EXPORTS -> onExports()
                DrawerDestination.SETTINGS -> onSettings()
                DrawerDestination.ACCOUNT_DATA -> onAccountData()
                DrawerDestination.VEHICLES -> Unit
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TransportTopAppBar(title = state.title, navigationIcon = Icons.Rounded.Menu, navigationIconDesc = "Open menu", onNavigationClick = { scope.launch { drawerState.open() } }, trailingIcons = {
                    IconButton(onClick = {}) { Icon(Icons.Rounded.Tune, contentDescription = "Filter", tint = MaterialTheme.colorScheme.onSurface) }
                })

                Row(
                    modifier = Modifier.padding(horizontal = Dimens.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.filterChips.forEach { chip ->
                        FilterChip(chip, selected = state.selectedFilter == chip, onClick = { onEvent(VehicleBoardEvent.SelectFilter(chip)) })
                    }
                }

                // Summary strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.screenPadding, vertical = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryValue("RUNNING", state.summaryRunning, normal = true)
                    SummaryValue("IDLE", state.summaryIdle, normal = true)
                    SummaryValue("LATE", state.summaryLate, normal = false)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.vehicles) { vehicle ->
                        VehicleCard(vehicle, state.loadIt, onLoad = { onNewChallan() })
                    }
                }

                // Bottom navigation — Home / Register / Vehicles
                TransportBottomNavBar(
                    destinations = listOf(
                        NavDestination("Home", Icons.Outlined.Home, Icons.Rounded.Home),
                        NavDestination("Register", Icons.Outlined.ListAlt, Icons.AutoMirrored.Rounded.ListAlt),
                        NavDestination("Vehicles", Icons.Outlined.LocalShipping, Icons.Rounded.LocalShipping)
                    ),
                    activeIndex = 2,
                    onSelect = { index ->
                        when (index) {
                            0 -> onHome()
                            1 -> onRegister()
                        }
                    }
                )
            }

            // Extended FAB
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 96.dp)
            ) {
                AppPrimaryButton(state.newChallan, onClick = onNewChallan, leadingIcon = Icons.Rounded.Add)
            }
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String, normal: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = TransportTypeScale.dataMedium,
            color = if (normal) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun VehicleCard(vehicle: VehicleRow, loadIt: String, onLoad: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    ContentCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(vehicle.number, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(vehicle.ownership, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (vehicle.isLate) {
                Spacer(Modifier.padding(horizontal = 4.dp))
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(percent = 100))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Late", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        if (vehicle.idleDays != null) {
            // Idle card — no route line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Idle ${vehicle.idleDays} days · last trip closed ${vehicle.lastTrip}",
                    style = TransportTypeScale.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(loadIt, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        } else {
            // Route line
            val stops = vehicle.stops
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (stops.size > 1) {
                    Canvas(
                        modifier = Modifier.fillMaxWidth().height(2.dp)
                    ) {
                        val lineY = size.height / 2
                        val fraction = vehicle.currentStop.toFloat() / (stops.size - 1)
                        drawLine(outlineVariant, start = Offset(0f, lineY), end = Offset(size.width, lineY), strokeWidth = 2.dp.toPx())
                        if (fraction > 0) {
                            drawLine(primary, start = Offset(0f, lineY), end = Offset(size.width * fraction, lineY), strokeWidth = 2.dp.toPx())
                        }
                        val tickX = size.width * fraction
                        drawCircle(primary, radius = 4.dp.toPx(), center = Offset(tickX, lineY))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (stops.isNotEmpty()) {
                    Text(stops.first(), style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stops.last(), style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${vehicle.driver} · ${vehicle.load}${vehicle.challan?.let { " · $it" } ?: ""}",
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (vehicle.lateLine != null) {
                Text(" · ${vehicle.lateLine}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun VehicleBoardPreview() {
    com.example.transportapp.core.designsystem.theme.TransportAppTheme {
        VehicleBoardContent(
            state = VehicleBoardUiState(),
            onEvent = {},
            onNewChallan = {},
            onHome = {},
            onRegister = {},
            onReports = {},
            onMasters = {},
            onExports = {},
            onSettings = {},
            onAccountData = {}
        )
    }
}
