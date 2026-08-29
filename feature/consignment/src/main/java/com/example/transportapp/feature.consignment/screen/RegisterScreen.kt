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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalShipping
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.DocketRow
import com.example.transportapp.core.designsystem.component.FilterChip
import com.example.transportapp.core.designsystem.component.NavDestination
import com.example.transportapp.core.designsystem.component.SearchField
import com.example.transportapp.core.designsystem.component.SummaryStrip
import com.example.transportapp.core.designsystem.component.TransportBottomNavBar
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.ui.sample.RegisterListItem

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onDocketClick: (String) -> Unit,
    onNewBilty: () -> Unit,
    onHome: () -> Unit,
    onVehicles: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    RegisterContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onDocketClick = onDocketClick,
        onNewBilty = onNewBilty,
        onHome = onHome,
        onVehicles = onVehicles
    )
}

@Composable
fun RegisterContent(
    state: RegisterUiState,
    onEvent: (RegisterEvent) -> Unit,
    onBack: () -> Unit,
    onDocketClick: (String) -> Unit,
    onNewBilty: () -> Unit,
    onHome: () -> Unit,
    onVehicles: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TransportTopAppBar(title = "Register", onNavigationClick = onBack, trailingIcons = {
                IconButton(onClick = {}) { Icon(Icons.Rounded.Tune, contentDescription = "Filter", tint = MaterialTheme.colorScheme.onSurface) }
                IconButton(onClick = {}) { Icon(Icons.Rounded.FileDownload, contentDescription = "Export", tint = MaterialTheme.colorScheme.onSurface) }
            })

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
                state.filterOptions.forEach { option ->
                    FilterChip(
                        label = option,
                        selected = option == state.selectedFilter,
                        onClick = { onEvent(RegisterEvent.ChangeFilter(option)) }
                    )
                }
            }

            SummaryStrip(
                *state.summaryFigures.toTypedArray(),
                modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.items) { item ->
                    when (item) {
                        is RegisterListItem.Header -> {
                            Text(
                                item.label,
                                style = TransportTypeScale.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp)
                            )
                        }
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
                    }
                }
            }

            // Bottom navigation — Home / Register / Vehicles
            TransportBottomNavBar(
                destinations = listOf(
                    NavDestination("Home", Icons.Outlined.Home, Icons.Rounded.Home),
                    NavDestination("Register", Icons.Outlined.ListAlt, Icons.Outlined.ListAlt),
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
