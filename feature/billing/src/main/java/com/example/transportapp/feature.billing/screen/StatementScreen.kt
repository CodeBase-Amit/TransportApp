package com.example.transportapp.feature.billing.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppOutlinedButton
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

/**
 * T16 — Party statement of account. Opening/closing pinned, the ledger scrolls.
 */
@Composable
fun StatementScreen(
    partyId: String,
    onBack: () -> Unit,
    viewModel: StatementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    StatementContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@Composable
fun StatementContent(
    state: StatementUiState,
    onEvent: (StatementEvent) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = state.party.ifEmpty { "Statement" }, onNavigationClick = onBack, trailingIcons = {})
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = Dimens.screenPadding)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(state.partySubtitle, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(state.period, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Pinned opening row
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("OPENING BALANCE", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(state.opening, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
        }

        // Ledger header
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Date", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(64.dp))
            Text("Particulars", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("Debit", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
            Text("Credit", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
            Text("Balance", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
        }

        // Ledger
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(state.ledgerRows) { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(row.date, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(64.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.docNo, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(row.desc, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(row.debit, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
                    Text(row.credit, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
                    Text(row.balance, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
                }
            }
        }

        // Pinned closing row
        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("CLOSING BALANCE", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                Text(state.closing, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(state.ofWhich, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Sticky bar
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)) {
            AppOutlinedButton("Send statement as PDF", onClick = { onEvent(StatementEvent.SendPdf) }, leadingIcon = Icons.Rounded.Share, modifier = Modifier.fillMaxWidth())
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun StatementPreview() {
    com.example.transportapp.core.designsystem.theme.TransportAppTheme {
        StatementContent(
            state = StatementUiState(),
            onEvent = {},
            onBack = {}
        )
    }
}
