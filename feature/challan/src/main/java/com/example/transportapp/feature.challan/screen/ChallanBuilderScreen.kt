package com.example.transportapp.feature.challan.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.FilterChip
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.JourneyChip
import com.example.transportapp.core.designsystem.component.PaymentStamp
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors

@Composable
fun ChallanBuilderScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpenTrip: (String) -> Unit,
    viewModel: ChallanBuilderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    androidx.compose.runtime.LaunchedEffect(state.createdChallanNo) {
        state.createdChallanNo?.let { no ->
            onOpenTrip(no)
            viewModel.consumeCreatedChallanNo()
        }
    }
    ChallanBuilderContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onCreate = onCreate,
        onOpenTrip = { onOpenTrip("") }
    )
}

@Composable
fun ChallanBuilderContent(
    state: ChallanBuilderUiState,
    onEvent: (ChallanBuilderEvent) -> Unit,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpenTrip: () -> Unit
) {
    val selectedItems = state.loadable.filter { it.docNumber in state.selectedBilties }

    val selectedWeight = state.selectedWeightKg
    val overloaded = state.overloaded

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("New challan", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(state.reservedNumber, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
        }

        // Filter chips
        Row(
            modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.filterChips.forEach { chip ->
                FilterChip(
                    label = chip,
                    selected = state.selectedFilter == chip,
                    onClick = { onEvent(ChallanBuilderEvent.SelectFilter(chip)) }
                )
            }
        }

        // Vehicle & driver block
        Spacer(Modifier.height(4.dp))
        VehicleAndDriver(state)

        // Route & hire block
        Spacer(Modifier.height(8.dp))
        RouteAndHire(state)

        // Load meter (pinned)
        Spacer(Modifier.height(8.dp))
        LoadMeter(weight = selectedWeight, capacity = state.capacityKg, overloaded = overloaded, count = selectedItems.size, freightTotal = state.freightTotal)

        // Pick list
        Column(modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp)) {
            GroupHeading(state.readyToLoad, trailing = {
                Text(state.selectAll, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onEvent(ChallanBuilderEvent.ToggleSelectAll) })
            })
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.loadable) { item ->
                val isSelected = item.docNumber in state.selectedBilties
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent)
                        .clickable { onEvent(ChallanBuilderEvent.ToggleConsignment(item.docNumber)) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheckBox(isSelected)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(item.docNumber, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(item.amount, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("· ${item.consignee}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${item.packages} · ${item.weight}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            PaymentStamp(mode = item.paymentMode)
                            if (item.isOnwardLeg) {
                                Spacer(Modifier.width(8.dp))
                                JourneyChip(status = item.status)
                                Spacer(Modifier.width(4.dp))
                                Text(item.onwardNote ?: "", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Sticky bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp)
        ) {
            if (overloaded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(transportColors().haulAmberContainer, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = transportColors().onHaulAmber, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${state.overByKg} kg over capacity. A manager has to approve this challan before dispatch.", style = TransportTypeScale.bodyMedium, color = transportColors().onHaulAmber)
                }
                Spacer(Modifier.height(8.dp))
            }
            if (state.error != null) {
                Text(
                    state.error,
                    style = TransportTypeScale.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            AppPrimaryButton(
                "${state.createChallan} · ${selectedItems.size} ${state.consignmentsSuffix}",
                onClick = { onEvent(ChallanBuilderEvent.Create) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Rounded.LocalShipping
            )
        }
    }
}

@Composable
private fun VehicleAndDriver(state: ChallanBuilderUiState) {
    Column(modifier = Modifier.padding(horizontal = Dimens.screenPadding)) {
        GroupHeading("Vehicle & driver", modifier = Modifier.padding(bottom = 8.dp))
        ContentCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.vehicleNumber, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(state.vehicleOwnership, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(state.driverName, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(state.driverLicenceLine, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RouteAndHire(state: ChallanBuilderUiState) {
    Column(modifier = Modifier.padding(horizontal = Dimens.screenPadding)) {
        GroupHeading("Route & hire", modifier = Modifier.padding(bottom = 8.dp))
        ContentCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("To", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(state.routeTo, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text("Via", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(state.routeVia, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Lorry hire", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(state.lorryHire, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text("Advance paid", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(state.advancePaid, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text("Balance", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(state.balance, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun LoadMeter(weight: Int, capacity: Int, overloaded: Boolean, count: Int, freightTotal: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenPadding)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("LOAD", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$weight / $capacity kg", style = TransportTypeScale.dataMedium, color = if (overloaded) transportColors().haulAmber else MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(percent = 100))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            val fillFraction = (weight.toFloat() / capacity).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillFraction)
                    .height(8.dp)
                    .background(if (overloaded) transportColors().haulAmber else MaterialTheme.colorScheme.primary, RoundedCornerShape(percent = 100))
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "$count consignments · freight $freightTotal",
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CheckBox(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ChallanBuilderPreview() {
    com.example.transportapp.core.designsystem.theme.TransportAppTheme {
        ChallanBuilderContent(
            state = ChallanBuilderUiState(),
            onEvent = {},
            onBack = {},
            onCreate = {},
            onOpenTrip = {}
        )
    }
}
