package com.example.transportapp.feature.consignment.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.DocketRow
import com.example.transportapp.core.designsystem.component.FilterChip
import com.example.transportapp.core.designsystem.component.NavDestination
import com.example.transportapp.core.designsystem.component.SearchField
import com.example.transportapp.core.designsystem.component.SummaryStrip
import com.example.transportapp.core.designsystem.component.TransportBottomNavBar
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.ui.AppNavDrawer
import com.example.transportapp.core.ui.DrawerDestination
import com.example.transportapp.core.ui.rememberAppDrawerState
import com.example.transportapp.core.ui.sample.RegisterListItem
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onDocketClick: (String) -> Unit,
    onNewBilty: () -> Unit,
    onHome: () -> Unit,
    onVehicles: () -> Unit,
    onReports: () -> Unit,
    onMasters: () -> Unit,
    onExports: () -> Unit,
    onSettings: () -> Unit,
    onAccountData: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val items = viewModel.items.collectAsLazyPagingItems()
    RegisterContent(
        state = state,
        items = items,
        onEvent = viewModel::onEvent,
        onDocketClick = onDocketClick,
        onNewBilty = onNewBilty,
        onHome = onHome,
        onVehicles = onVehicles,
        onReports = onReports,
        onMasters = onMasters,
        onExports = onExports,
        onSettings = onSettings,
        onAccountData = onAccountData
    )
}

@Composable
fun RegisterContent(
    state: RegisterUiState,
    items: androidx.paging.compose.LazyPagingItems<RegisterListItem>,
    onEvent: (RegisterEvent) -> Unit,
    onDocketClick: (String) -> Unit,
    onNewBilty: () -> Unit,
    onHome: () -> Unit,
    onVehicles: () -> Unit,
    onReports: () -> Unit,
    onMasters: () -> Unit,
    onExports: () -> Unit,
    onSettings: () -> Unit,
    onAccountData: () -> Unit
) {
    val drawerState = rememberAppDrawerState()
    val scope = rememberCoroutineScope()
    var showFilterSheet by remember { mutableStateOf(false) }
    AppNavDrawer(
        drawerState = drawerState,
        companyInitials = state.companyInitials,
        companyName = state.companyName,
        branchName = state.branchName,
        activeDestination = DrawerDestination.REGISTER,
        onSelect = { destination ->
            scope.launch { drawerState.close() }
            when (destination) {
                DrawerDestination.HOME -> onHome()
                DrawerDestination.VEHICLES -> onVehicles()
                DrawerDestination.REPORTS -> onReports()
                DrawerDestination.MASTERS -> onMasters()
                DrawerDestination.EXPORTS -> onExports()
                DrawerDestination.SETTINGS -> onSettings()
                DrawerDestination.ACCOUNT_DATA -> onAccountData()
                else -> Unit
            }
        },
        content = {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TransportTopAppBar(title = "Register", navigationIcon = Icons.Rounded.Menu, navigationIconDesc = "Open menu", onNavigationClick = { scope.launch { drawerState.open() } }, trailingIcons = {
                // S21: the Tune icon opens the filter sheet; the download icon exports
                // the freight register to CSV (lands in the Export centre's recent list).
                IconButton(onClick = { showFilterSheet = true }) { Icon(Icons.Rounded.Tune, contentDescription = "Filter", tint = MaterialTheme.colorScheme.onSurface) }
                IconButton(onClick = { onEvent(RegisterEvent.ExportCsv) }, enabled = !state.isExporting) { Icon(Icons.Rounded.FileDownload, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.onSurface) }
            })

            if (state.exportNote != null) {
                Text(
                    state.exportNote,
                    style = TransportTypeScale.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.screenPadding)
                )
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.screenPadding)) {
                SearchField(
                    value = state.searchQuery,
                    onValueChange = { onEvent(RegisterEvent.ChangeSearchQuery(it)) },
                    placeholder = "Bilty number, party, vehicle or private mark"
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.chipGap),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.screenPadding)
                    .padding(top = 8.dp)
            ) {
                state.chips.forEach { chip ->
                    FilterChip(
                        label = chip.label,
                        selected = chip.selected,
                        onClick = { onEvent(RegisterEvent.ToggleChip(chip.kind)) }
                    )
                }
            }

            val summary = state.summary
            SummaryStrip(
                "MATCHING" to (summary?.matching?.toString() ?: "—"),
                "PACKAGES" to (summary?.packages?.toString() ?: "—"),
                "FREIGHT" to (summary?.amountPaise?.let { com.example.transportapp.core.common.Money(it).formatted() } ?: "—"),
                modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp)
            )

            if (items.itemCount == 0 && !state.isLoading) {
                RegisterEmptyState(
                    noRecordsAtAll = state.summary?.matching == 0 && state.chips.none { it.selected } && state.searchQuery.isEmpty(),
                    onClearFilters = { onEvent(RegisterEvent.ClearFilters) },
                    onNewBilty = onNewBilty
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    registerList(items, onDocketClick)
                }
            }

            // Bottom navigation — Home / Register / Vehicles
            TransportBottomNavBar(
                destinations = listOf(
                    NavDestination("Home", Icons.Outlined.Home, Icons.Rounded.Home),
                    NavDestination("Register", Icons.Outlined.ListAlt, Icons.AutoMirrored.Rounded.ListAlt),
                    NavDestination("Vehicles", Icons.Outlined.LocalShipping, Icons.Rounded.LocalShipping)
                ),
                activeIndex = 1,
                onSelect = { index ->
                    when (index) {
                        0 -> onHome()
                        2 -> onVehicles()
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
            AppPrimaryButton(
                text = "New bilty",
                onClick = onNewBilty,
                leadingIcon = Icons.Rounded.Add
            )
        }
    }
        }
    )
    // S21 — the filter sheet: the same chips the bar shows, consolidated for one-hand reach.
    if (showFilterSheet) {
        com.example.transportapp.core.designsystem.component.FilterSheet(
            title = "Filter the register",
            onDismiss = { showFilterSheet = false },
        ) {
            state.chips.forEach { chip ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        label = chip.label,
                        selected = chip.selected,
                        onClick = { onEvent(RegisterEvent.ToggleChip(chip.kind)) }
                    )
                }
            }
            AppTextButton("Clear all filters", onClick = { onEvent(RegisterEvent.ClearFilters) }, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private fun LazyListScope.registerList(
    items: androidx.paging.compose.LazyPagingItems<RegisterListItem>,
    onDocketClick: (String) -> Unit
) {
    items(
        count = items.itemCount,
        key = items.itemKey { item ->
            when (item) {
                is RegisterListItem.Header -> "h-${item.label}"
                is RegisterListItem.Row -> "r-${item.row.docNumber}"
            }
        },
    ) { index ->
        when (val item = items[index]) {
            is RegisterListItem.Header -> Text(
                item.label,
                style = TransportTypeScale.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp)
            )
            is RegisterListItem.Row -> {
                val row = item.row
                DocketRow(
                    docNumber = row.docNumber,
                    amount = row.amount,
                    fromStation = row.from,
                    toStation = row.to,
                    consignee = row.consignee,
                    status = row.status,
                    paymentMode = row.paymentMode,
                    packagesCaption = row.caption,
                    exceptionCaption = row.exception,
                    syncPending = row.syncPending,
                    onClick = { onDocketClick(row.docNumber) }
                )
            }
            null -> {}
        }
    }
}

/** Design T7's two distinct empty states — an empty register is not a filtered-out register. */
@Composable
private fun RegisterEmptyState(
    noRecordsAtAll: Boolean,
    onClearFilters: () -> Unit,
    onNewBilty: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (noRecordsAtAll) Icons.Rounded.LocalShipping else Icons.Rounded.Rule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (noRecordsAtAll) {
            Text("No bilties yet", style = TransportTypeScale.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Book one and it shows up here straight away.",
                style = TransportTypeScale.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            AppPrimaryButton(text = "Book a bilty", onClick = onNewBilty)
        } else {
            Text("No bilties match these filters", style = TransportTypeScale.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Try clearing a filter or widening the date range.",
                style = TransportTypeScale.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            AppTextButton("Clear filters", onClick = onClearFilters)
        }
    }
}
