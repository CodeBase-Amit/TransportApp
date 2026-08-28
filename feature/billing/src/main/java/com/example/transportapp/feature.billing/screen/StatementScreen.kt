package com.example.transportapp.feature.billing.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppOutlinedButton
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.ui.sample.SampleData

/**
 * T16 — Party statement of account. Opening/closing pinned, the ledger scrolls.
 */
@Composable
fun StatementScreen(partyId: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = SampleData.BILL_PARTY, onNavigationClick = onBack, trailingIcons = {
            IconButton(onClick = {}) { Icon(Icons.Rounded.FileDownload, contentDescription = "Download", tint = MaterialTheme.colorScheme.onSurface) }
        })
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = Dimens.screenPadding)) {
            Text("1 Apr 2026 – 25 Aug 2026", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("  All branches", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Rounded.DateRange, contentDescription = "Change period", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        // Pinned opening row
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("OPENING BALANCE", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(SampleData.OPENING_BALANCE, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
        }

        // Ledger header
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Date", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(64.dp))
            Text("Particulars", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("Debit", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            Text("Credit", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            Text("Balance", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }

        // Ledger
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(SampleData.ledgerRows) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(if (SampleData.ledgerRows.indexOf(row) % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(row.date, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(64.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.docNo, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(row.desc, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(row.debit, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(72.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    Text(row.credit, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(72.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    Text(row.balance, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }
        }

        // Pinned closing row
        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("CLOSING BALANCE", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                Text(SampleData.CLOSING_BALANCE, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text("of which ${SampleData.OVER90_AGEING}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Sticky bar
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)) {
            AppOutlinedButton("Send statement as PDF", onClick = {}, leadingIcon = Icons.Rounded.Share, modifier = Modifier.fillMaxWidth())
        }
    }
}