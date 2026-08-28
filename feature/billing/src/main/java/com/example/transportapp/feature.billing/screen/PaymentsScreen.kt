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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.FilterChip
import com.example.transportapp.core.designsystem.component.JourneyChip
import com.example.transportapp.core.designsystem.component.PaymentStamp
import com.example.transportapp.core.designsystem.component.SummaryStrip
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.ui.sample.SampleData

enum class PaymentsTab { TOPAY, BILL_RECEIPTS }

@Composable
fun PaymentsScreen(onBack: () -> Unit) {
    var tab by remember { mutableStateOf(PaymentsTab.TOPAY) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = "Payments", onNavigationClick = onBack, trailingIcons = {
            IconButton(onClick = {}) { Icon(Icons.Rounded.Tune, contentDescription = "Filter", tint = MaterialTheme.colorScheme.onSurface) }
        })

        // Tabs
        Row(modifier = Modifier.fillMaxWidth()) {
            PaymentsTabItem("To Pay · 9", selected = tab == PaymentsTab.TOPAY, onClick = { tab = PaymentsTab.TOPAY }, modifier = Modifier.weight(1f))
            PaymentsTabItem("Bill receipts", selected = tab == PaymentsTab.BILL_RECEIPTS, onClick = { tab = PaymentsTab.BILL_RECEIPTS }, modifier = Modifier.weight(1f))
        }

        when (tab) {
            PaymentsTab.TOPAY -> ToPayTab()
            PaymentsTab.BILL_RECEIPTS -> BillReceiptsTab()
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
private fun ToPayTab() {
    val rows = SampleData.toPayRows
    var collectSheetOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SummaryStrip("TO COLLECT" to "41,760.00", "AT INDORE" to "9", modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(rows) { row ->
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
private fun BillReceiptsTab() {
    val receipts = SampleData.receiptRows
    Column(modifier = Modifier.fillMaxSize()) {
        SummaryStrip("RECEIVED THIS MONTH" to "4,18,200.00", "RECEIPTS" to "27", modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp))
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(receipts) { (no, party, amount) ->
                Row(modifier = Modifier.fillMaxWidth().height(88.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(no, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(amount, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(party, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("NEFT · 20 Aug", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}