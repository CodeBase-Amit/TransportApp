package com.example.transportapp.feature.billing.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.common.Money
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.FilterChip
import com.example.transportapp.core.designsystem.component.JourneyChip
import com.example.transportapp.core.designsystem.component.PaymentStamp
import com.example.transportapp.core.designsystem.component.SummaryStrip
import com.example.transportapp.core.designsystem.component.TransportExtendedFab
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.PaymentMode
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * T15 — money coming in (§12.2). Tab 1 collects To Pay at this branch (Held rows wait for a
 * Manager waiver); tab 2 records receipts and allocates them explicitly across bills.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    onBack: () -> Unit,
    viewModel: PaymentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PaymentsContent(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

private val timeFormat = SimpleDateFormat("d MMM", Locale.ENGLISH)

@Composable
fun PaymentsContent(
    state: PaymentsUiState,
    onEvent: (PaymentsEvent) -> Unit,
    onBack: () -> Unit,
) {
    var showFilterSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = "Payments", onNavigationClick = onBack, trailingIcons = {
            // S21: the sheet switches the two money tabs without a second scroll back up.
            IconButton(onClick = { showFilterSheet = true }) { Icon(Icons.Rounded.Tune, contentDescription = "Filter", tint = MaterialTheme.colorScheme.onSurface) }
        })

        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            PaymentsTabItem(
                "To Pay · ${state.toPayRows.size}",
                selected = state.tab == PaymentsTab.TOPAY,
                onClick = { onEvent(PaymentsEvent.SelectTab(PaymentsTab.TOPAY)) },
                modifier = Modifier.weight(1f),
            )
            PaymentsTabItem(
                "Bill receipts",
                selected = state.tab == PaymentsTab.BILL_RECEIPTS,
                onClick = { onEvent(PaymentsEvent.SelectTab(PaymentsTab.BILL_RECEIPTS)) },
                modifier = Modifier.weight(1f),
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (state.tab) {
                PaymentsTab.TOPAY -> ToPayTab(state, onEvent)
                PaymentsTab.BILL_RECEIPTS -> BillReceiptsTab(state, onEvent)
            }
        }
    }

    state.collectSheet?.let { sheet -> CollectSheet(sheet, onEvent) }
    state.allocationSheet?.let { sheet -> AllocationSheet(sheet, onEvent) }
    if (showFilterSheet) {
        com.example.transportapp.core.designsystem.component.FilterSheet(
            title = "Payments",
            onDismiss = { showFilterSheet = false },
        ) {
            Text("Show", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilterChip("To Pay collections", selected = state.tab == PaymentsTab.TOPAY, onClick = { onEvent(PaymentsEvent.SelectTab(PaymentsTab.TOPAY)) })
            FilterChip("Bill receipts", selected = state.tab == PaymentsTab.BILL_RECEIPTS, onClick = { onEvent(PaymentsEvent.SelectTab(PaymentsTab.BILL_RECEIPTS)) })
        }
    }
}

@Composable
private fun PaymentsTabItem(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clickable(onClick = onClick).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = TransportTypeScale.labelLarge, color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .height(4.dp)
                .width(if (selected) 48.dp else 0.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(percent = 100)),
        )
    }
}

@Composable
private fun ToPayTab(state: PaymentsUiState, onEvent: (PaymentsEvent) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SummaryStrip(
            "TO COLLECT" to Money(state.toCollectPaise).formatted(),
            "AWAITING" to state.toPayRows.size.toString(),
            modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp),
        )
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(state.toPayRows, key = { it.localId }) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(88.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(row.displayNo, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(Money(row.amountPaise).formatted(), style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("· ${row.consigneeName}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val status = runCatching { ConsignmentStatus.valueOf(row.status) }.getOrDefault(ConsignmentStatus.BOOKED)
                            JourneyChip(status = status)
                            Spacer(Modifier.width(8.dp))
                            PaymentStamp(mode = PaymentMode.TOPAY)
                        }
                        if (row.status == "HELD" && !row.waived) {
                            Text(
                                row.heldRemark?.let { "Held — $it" } ?: "Held — collect only after the hold is settled",
                                style = TransportTypeScale.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (row.collectable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                RoundedCornerShape(percent = 100),
                            )
                            .clickable { onEvent(PaymentsEvent.OpenCollect(row)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Payments,
                            contentDescription = "Collect",
                            tint = if (row.collectable) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun BillReceiptsTab(state: PaymentsUiState, onEvent: (PaymentsEvent) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SummaryStrip(
                "RECEIVED THIS MONTH" to Money(state.receivedThisMonthPaise).formatted(),
                "RECEIPTS" to state.receiptsCount.toString(),
                modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp),
            )
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(state.receipts, key = { it.localId }) { row ->
                    Row(modifier = Modifier.fillMaxWidth().height(88.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.SpaceBetween) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(row.receiptNo ?: "(no number)", style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(Money(row.amountPaise).formatted(), style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(row.partyName, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "${row.instrument}${row.instrumentRef?.let { " $it" } ?: ""} · ${timeFormat.format(row.receivedAt)}",
                                style = TransportTypeScale.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        TransportExtendedFab(
            text = "Record a receipt",
            icon = Icons.Rounded.Add,
            onClick = { onEvent(PaymentsEvent.OpenAllocation) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Composable
private fun ModeButtons(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("CASH", "UPI", "CHEQUE", "NEFT").forEach { mode ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (mode == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(mode, style = TransportTypeScale.labelLarge, color = if (mode == selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CollectSheet(sheet: CollectSheetState, onEvent: (PaymentsEvent) -> Unit) {
    ModalBottomSheet(onDismissRequest = { onEvent(PaymentsEvent.DismissCollect) }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Collect ${Money(sheet.line.amountPaise).formatted()}", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text("${sheet.line.displayNo} · ${sheet.line.consigneeName}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (sheet.line.status == "HELD" && !sheet.line.waived) {
                Text(
                    "Held — collect only after the hold is settled.",
                    style = TransportTypeScale.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                if (sheet.isManager) {
                    TransportTextField(
                        value = sheet.waiverReason,
                        onValueChange = { onEvent(PaymentsEvent.SetWaiverReason(it)) },
                        label = "Waiver reason (Manager)",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AppPrimaryButton(
                        if (sheet.waiving) "Recording…" else "Record the waiver",
                        onClick = { onEvent(PaymentsEvent.RecordWaiver) },
                        enabled = sheet.waiverReason.isNotBlank() && !sheet.waiving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Text("HOW", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ModeButtons(selected = sheet.mode, onSelect = { onEvent(PaymentsEvent.SetCollectMode(it)) })
                TransportTextField(
                    value = sheet.amountText,
                    onValueChange = { onEvent(PaymentsEvent.SetCollectAmount(it)) },
                    label = "Amount received",
                    modifier = Modifier.fillMaxWidth(),
                )
                if (sheet.mode != "CASH") {
                    TransportTextField(
                        value = sheet.reference,
                        onValueChange = { onEvent(PaymentsEvent.SetCollectReference(it)) },
                        label = "Reference",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                AppPrimaryButton(
                    if (sheet.saving) "Saving…" else "Collect and print receipt",
                    onClick = { onEvent(PaymentsEvent.SaveCollect) },
                    enabled = !sheet.saving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            sheet.error?.let { Text(it, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AllocationSheet(sheet: AllocationSheetState, onEvent: (PaymentsEvent) -> Unit) {
    ModalBottomSheet(onDismissRequest = { onEvent(PaymentsEvent.DismissAllocation) }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Record a receipt", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface)

            if (sheet.partyId == null) {
                Text("Who paid?", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (sheet.parties.isEmpty()) Text("No issued bills yet — nothing to receive against.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                sheet.parties.forEach { (id, name) ->
                    Text(
                        name,
                        style = TransportTypeScale.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().clickable { onEvent(PaymentsEvent.SelectParty(id)) }.padding(vertical = 8.dp),
                    )
                }
            } else {
                Text(sheet.partyName, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                ModeButtons(selected = sheet.mode, onSelect = { onEvent(PaymentsEvent.SetMode(it)) })
                TransportTextField(
                    value = sheet.amountText,
                    onValueChange = { onEvent(PaymentsEvent.SetAmount(it)) },
                    label = "Amount",
                    modifier = Modifier.fillMaxWidth(),
                )
                if (sheet.mode != "CASH") {
                    TransportTextField(
                        value = sheet.reference,
                        onValueChange = { onEvent(PaymentsEvent.SetReference(it)) },
                        label = "Reference",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text("APPLY IT TO", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                sheet.bills.forEach { bill ->
                    val appliedText = sheet.applied[bill.localId] ?: ""
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bill.billNo ?: "(no number)", style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "outstanding ${Money(bill.outstandingPaise).formatted()}",
                                style = TransportTypeScale.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TransportTextField(
                            value = appliedText,
                            onValueChange = { onEvent(PaymentsEvent.SetApplied(bill.localId, it)) },
                            label = "Amount",
                            modifier = Modifier.width(120.dp),
                            singleLine = true,
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("UNAPPLIED", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(
                        Money(sheet.unappliedPaise).formatted(),
                        style = TransportTypeScale.dataMedium,
                        color = if (sheet.unappliedPaise == 0L) MaterialTheme.colorScheme.primary else transportColors().haulAmber,
                    )
                }
                AppPrimaryButton(
                    if (sheet.saving) "Saving…" else "Save receipt",
                    onClick = { onEvent(PaymentsEvent.SaveAllocation) },
                    enabled = sheet.canSave,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (sheet.unappliedPaise > 0) {
                    Text(
                        "Apply the whole amount, or park the rest as an on-account credit.",
                        style = TransportTypeScale.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    com.example.transportapp.core.designsystem.component.AppTextButton(
                        "Park the rest (${Money(sheet.unappliedPaise).formatted()})",
                        onClick = { onEvent(PaymentsEvent.ParkTheRest) },
                    )
                }
                sheet.error?.let { Text(it, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
