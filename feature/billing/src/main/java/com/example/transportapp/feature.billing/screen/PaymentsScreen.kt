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
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transportapp.core.designsystem.component.JourneyChip
import com.example.transportapp.core.designsystem.component.PaymentStamp
import com.example.transportapp.core.designsystem.component.SummaryStrip
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

@Composable
fun PaymentsScreen(
    onBack: () -> Unit,
    viewModel: PaymentsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    PaymentsContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@Composable
fun PaymentsContent(
    state: PaymentsUiState,
    onEvent: (PaymentsEvent) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = state.title, onNavigationClick = onBack, trailingIcons = {
            IconButton(onClick = {}) { Icon(Icons.Rounded.Tune, contentDescription = "Filter", tint = MaterialTheme.colorScheme.onSurface) }
        })

        Text(
            state.subtitle,
            style = TransportTypeScale.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.screenPadding)
        )

        // Tabs
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            PaymentsTabItem("${state.toPayTab} · 9", selected = state.tab == PaymentsTab.TOPAY, onClick = { onEvent(PaymentsEvent.SelectTab(PaymentsTab.TOPAY)) }, modifier = Modifier.weight(1f))
            PaymentsTabItem(state.billReceiptsTab, selected = state.tab == PaymentsTab.BILL_RECEIPTS, onClick = { onEvent(PaymentsEvent.SelectTab(PaymentsTab.BILL_RECEIPTS)) }, modifier = Modifier.weight(1f))
        }

        when (state.tab) {
            PaymentsTab.TOPAY -> ToPayTab(state)
            PaymentsTab.BILL_RECEIPTS -> BillReceiptsTab(state)
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
                .height(3.dp)
                .width(if (selected) 48.dp else 0.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(percent = 100))
        )
    }
}

@Composable
private fun ToPayTab(state: PaymentsUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        SummaryStrip("TO COLLECT" to state.toCollect, "AT INDORE" to state.atIndore, modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(state.toPayRows) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(88.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(row.bilty, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(row.amount, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("· ${row.consignee}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            JourneyChip(status = row.status)
                            Spacer(Modifier.width(8.dp))
                            PaymentStamp(mode = row.mode)
                        }
                        if (row.caption != null) {
                            Text(row.caption!!, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    // Collect button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (row.collectable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(percent = 100)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Payments, contentDescription = "Collect", tint = if (row.collectable) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BillReceiptsTab(state: PaymentsUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        SummaryStrip("RECEIVED THIS MONTH" to state.receivedThisMonth, "RECEIPTS" to state.receipts, modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp))
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(state.receiptRows) { (no, party, amount) ->
                Row(modifier = Modifier.fillMaxWidth().height(88.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(no, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(amount, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(party, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(state.receiptModeLine, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PaymentsPreview() {
    com.example.transportapp.core.designsystem.theme.TransportAppTheme {
        PaymentsContent(
            state = PaymentsUiState(),
            onEvent = {},
            onBack = {}
        )
    }
}
