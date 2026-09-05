package com.example.transportapp.feature.challan.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowRightAlt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TaskAlt
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PaperColors
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.domain.transport.ConsignmentStatus

@Composable
fun ChallanDetailScreen(
    onBack: () -> Unit,
    onCloseTrip: () -> Unit,
    onEditLoad: () -> Unit = {},
    viewModel: ChallanDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val costDraft by viewModel.costDraft.collectAsState()
    // S27: print/share render failures were written into a StateFlow nobody collected.
    val printStatus by viewModel.printStatus.collectAsState()
    ChallanDetailContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onCloseTrip = onCloseTrip,
        // S27: the nav callback was accepted but never forwarded — "Edit load" clicked a
        // default empty lambda and did nothing.
        onEditLoad = onEditLoad,
        printStatus = printStatus,
        onDismissPrintStatus = viewModel::dismissPrintStatus,
    )
    // S19: the §11 add-cost dialog — head chips, rupee amount, mandatory remark.
    if (state.costOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.onEvent(ChallanDetailEvent.DismissAddCost) },
            title = { Text("Add a trip cost", style = TransportTypeScale.titleMedium) },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Diesel", "Toll", "Repair", "Other").forEach { head ->
                            com.example.transportapp.core.designsystem.component.FilterChip(
                                label = head,
                                selected = costDraft.head == head,
                                onClick = { viewModel.onEvent(ChallanDetailEvent.ChangeCostHead(head)) }
                            )
                        }
                    }
                    TransportTextField(
                        value = costDraft.amount,
                        onValueChange = { viewModel.onEvent(ChallanDetailEvent.ChangeCostAmount(it)) },
                        label = "Amount (₹)",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    TransportTextField(
                        value = costDraft.remark,
                        onValueChange = { viewModel.onEvent(ChallanDetailEvent.ChangeCostRemark(it)) },
                        label = "Remark · required",
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = costDraft.valid,
                    onClick = { viewModel.onEvent(ChallanDetailEvent.SaveCost) }
                ) { Text("Record cost", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.onEvent(ChallanDetailEvent.DismissAddCost) }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ChallanDetailContent(
    state: ChallanDetailUiState,
    onEvent: (ChallanDetailEvent) -> Unit,
    onBack: () -> Unit,
    onCloseTrip: () -> Unit,
    onEditLoad: () -> Unit = {},
    printStatus: com.example.transportapp.core.ui.PrintStatus = com.example.transportapp.core.ui.PrintStatus.Idle,
    onDismissPrintStatus: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Navigate back", tint = MaterialTheme.colorScheme.onSurface) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onEvent(ChallanDetailEvent.Print) }) { Icon(Icons.Rounded.Print, contentDescription = "Print", tint = MaterialTheme.colorScheme.onSurface) }
        }

        // S27: render/print failures now surface with the CaseFile affordance (tap to dismiss).
        when (val status = printStatus) {
            is com.example.transportapp.core.ui.PrintStatus.Rendering -> androidx.compose.material3.LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
            is com.example.transportapp.core.ui.PrintStatus.Error -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismissPrintStatus)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(status.message, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                Text("Dismiss", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            com.example.transportapp.core.ui.PrintStatus.Idle -> Unit
        }

        // S27: dispatch/close/add-cost failures set state.error that was never rendered.
        state.error?.let { message ->
            Text(
                message,
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing)
        ) {
            item { ChallanDocketHeader(state) }
            item {
                Column {
                    GroupHeading(state.vehicleAndDriverHeading, modifier = Modifier.padding(bottom = 8.dp))
                    VehicleAndDriver(state)
                }
            }
            // S19 — the §11 money position: freight earned vs hire + recorded costs, with
            // the provisional margin the owner watches (TransportApp.md §11).
            item {
                Column {
                    GroupHeading(
                        state.moneyHeading,
                        trailing = {
                            if (state.isOwnerOrManager && !state.isClosed) {
                                Text(state.addCostLabel, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clickable { onEvent(ChallanDetailEvent.StartAddCost) })
                            }
                        }
                    )
                    ContentCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        MoneyRow("Freight on this trip", state.freightLine)
                        MoneyRow("Lorry hire", state.hireLine)
                        MoneyRow("Other costs", state.costsLine)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(state.marginLabel.uppercase(), style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                state.margin,
                                style = TransportTypeScale.dataLarge,
                                fontFamily = com.example.transportapp.core.designsystem.theme.PlexMonoFamily,
                                color = if (state.margin.startsWith("-")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        }
                        state.costs.forEach { cost ->
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(cost.head, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                Text(cost.remark, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2f), maxLines = 1)
                                Text(cost.amount, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
            item {
                Column {
                    if (state.editLoad.isNotEmpty()) { GroupHeading(state.whatsLoadedTitle, trailing = { Text(state.editLoad, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp).clickable { onEditLoad() }) }) } else { GroupHeading(state.whatsLoadedTitle) }
                    ContentCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        state.challanGroups.forEach { group ->
                            if (group.rows.isNotEmpty()) {
                                Text("${group.station} · ${group.count}", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                group.rows.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(row.bilty, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(Modifier.width(8.dp))
                                        Text(row.consignee, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                        Text(row.weight, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                        // S27: the card already renders every leg — the old "Show all N"
                        // was a styled link to nothing and is gone.
                    }
                }
            }
            item { PaperChallanPreview(state) }
        }

        if (state.isDispatched) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(transportColors().haulAmberContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(state.dispatchedNotice, style = TransportTypeScale.bodySmall, color = transportColors().onHaulAmber)
            }
        }

        // Sticky bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChallanAction(Icons.Rounded.Print, "Print", onClick = { onEvent(ChallanDetailEvent.Print) })
            ChallanAction(Icons.Rounded.Share, "Share", onClick = { onEvent(ChallanDetailEvent.Share) })
            if (state.isDispatched) {
                ChallanAction(Icons.Rounded.TaskAlt, "Close trip", isPrimary = true, onClick = { onEvent(ChallanDetailEvent.CloseTrip) })
            } else {
                ChallanAction(Icons.Rounded.LocalShipping, "Dispatch", isPrimary = true, onClick = { onEvent(ChallanDetailEvent.Dispatch) })
            }
        }
    }
}

@Composable
private fun MoneyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(value, style = TransportTypeScale.dataMedium, fontFamily = com.example.transportapp.core.designsystem.theme.PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ChallanDocketHeader(state: ChallanDetailUiState) {
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.challanNo, style = TransportTypeScale.dataLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            ChallanStatusPill(state.statusLabel)
        }
        Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(state.routeFrom, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.Rounded.ArrowRightAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp).size(24.dp))
            Text(state.routeTo, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Text(state.routeVia, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 4.dp))
        }
        Text(
            if (state.isDispatched) state.dispatchedLine else state.createdLine,
            style = TransportTypeScale.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            HeaderFigure("CONSIGNMENTS", state.consignments.toString())
            HeaderFigure("LOAD", state.loadKg)
            HeaderFigure("HIRE", state.hire)
            HeaderFigure("BALANCE", state.balance)
        }
    }
}

@Composable
private fun ChallanStatusPill(label: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(percent = 100))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(label, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HeaderFigure(label: String, value: String) {
    Column {
        Text(label, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun VehicleAndDriver(state: ChallanDetailUiState) {
    ContentCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.vehicleNumber, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(state.vehicleOwnership, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Text(state.driverInitials, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(8.dp))
            // S27: the call affordance is now honest — a real dialer intent when the driver
            // has a phone on record, nothing drawn when they don't.
            if (state.driverPhone.isNotBlank()) {
                val context = androidx.compose.ui.platform.LocalContext.current
                Icon(
                    Icons.Rounded.Call,
                    contentDescription = "Call driver",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${state.driverPhone}"))
                            context.startActivity(intent)
                        },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(state.driverLine, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PaperChallanPreview(state: ChallanDetailUiState) {
    Column {
        GroupHeading("The paper", modifier = Modifier.padding(bottom = 8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaperColors.paperWhite, RoundedCornerShape(2.dp))
                .padding(16.dp)
        ) {
            Text(state.paperCompany, color = PaperColors.paperInk, fontWeight = FontWeight.Bold, style = TransportTypeScale.titleMedium)
            Text(state.paperDocType, color = PaperColors.paperInk, letterSpacing = 2.sp, style = TransportTypeScale.labelMedium)
            Text(state.paperChallanNo, color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
            Text(state.paperVehicle, color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
            Spacer(Modifier.height(8.dp))
            Text("Bilty / Dest / Weight", color = PaperColors.paperInk, style = TransportTypeScale.labelMedium)
            state.paperBiltyLines.forEach { line ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(line.bilty, color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
                    Text(line.dest, color = PaperColors.paperInk, style = TransportTypeScale.dataSmall, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                    Text(line.weight, color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
                }
            }
        }
        // S27: "See full challan" was a styled link to nothing — Print/Share render the real paper.
    }
}

@Composable
private fun ChallanAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        if (isPrimary) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(percent = 100)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        } else {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
        Text(label, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ChallanDetailPreview() {
    com.example.transportapp.core.designsystem.theme.TransportAppTheme {
        ChallanDetailContent(
            state = ChallanDetailUiState(),
            onEvent = {},
            onBack = {},
            onCloseTrip = {}
        )
    }
}
