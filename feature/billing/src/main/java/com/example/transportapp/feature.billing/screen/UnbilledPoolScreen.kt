package com.example.transportapp.feature.billing.screen

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.common.Money
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.FilterChip
import com.example.transportapp.core.designsystem.component.SummaryStrip
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * T13 — the unbilled pool (§12.1): TBB consignments not yet on a bill, grouped by party,
 * with the ageing bar as the card's signature. The sticky bar totals the explicit selection.
 */
@Composable
fun UnbilledPoolScreen(
    onBack: () -> Unit,
    onBillBuilt: (String) -> Unit,
    viewModel: UnbilledPoolViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) { viewModel.onBillBuilt = onBillBuilt }
    UnbilledPoolContent(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
fun UnbilledPoolContent(
    state: UnbilledPoolUiState,
    onEvent: (UnbilledPoolEvent) -> Unit,
    onBack: () -> Unit,
) {
    var showFilterSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = "Unbilled", onNavigationClick = onBack, trailingIcons = {
            // S21: the filter sheet consolidates the pool's period/branch/age filters.
            IconButton(onClick = { showFilterSheet = true }) { Icon(Icons.Rounded.Tune, contentDescription = "Filter", tint = MaterialTheme.colorScheme.onSurface) }
        })

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = Dimens.screenPadding).fillMaxWidth(),
        ) {
            FilterChip("This quarter", selected = state.thisQuarter, onClick = { onEvent(UnbilledPoolEvent.ToggleQuarter(!state.thisQuarter)) })
            FilterChip("All branches", selected = state.allBranches, onClick = { onEvent(UnbilledPoolEvent.ToggleAllBranches(!state.allBranches)) })
            FilterChip("Over 30 days", selected = state.minAgeDays == 30, onClick = { onEvent(UnbilledPoolEvent.SetAgeFilter(if (state.minAgeDays == 30) null else 30)) })
            FilterChip("Over 60 days", selected = state.minAgeDays == 60, onClick = { onEvent(UnbilledPoolEvent.SetAgeFilter(if (state.minAgeDays == 60) null else 60)) })
        }

        SummaryStrip(
            "PARTIES" to state.summaryParties.toString(),
            "CONSIGNMENTS" to state.summaryConsignments.toString(),
            "FREIGHT" to Money(state.summaryFreightPaise).formatted(),
            modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp),
        )

        if (state.parties.isEmpty()) {
            EmptyPool(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.parties, key = { it.group.partyId }) { party ->
                    UnbilledPartyCard(
                        party = party,
                        onToggle = { onEvent(UnbilledPoolEvent.ToggleParty(party.group.partyId)) },
                        onExpand = { onEvent(UnbilledPoolEvent.ToggleExpand(party.group.partyId)) },
                        onToggleConsignment = { id -> onEvent(UnbilledPoolEvent.ToggleConsignment(party.group.partyId, id)) },
                    )
                }
            }
        }

        state.error?.let { message ->
            Text(
                message,
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().clickable { onEvent(UnbilledPoolEvent.DismissError) }.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        // Sticky bar — the selection total updates as the accountant ticks (the signature).
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("SELECTED", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Money(state.selectedPaise).formatted(), style = TransportTypeScale.dataLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${state.selectedConsignments} consignments · ${state.selectedPartyCount} ${if (state.selectedPartyCount == 1) "party" else "parties"}",
                    style = TransportTypeScale.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppPrimaryButton(
                if (state.building) "Building…" else "Build the bill",
                onClick = { onEvent(UnbilledPoolEvent.BuildBill) },
                enabled = state.canBuild,
            )
        }
    }

    // S21 — the filter sheet for the pool.
    if (showFilterSheet) {
        com.example.transportapp.core.designsystem.component.FilterSheet(
            title = "Filter the pool",
            onDismiss = { showFilterSheet = false },
        ) {
            FilterChip("This quarter", selected = state.thisQuarter, onClick = { onEvent(UnbilledPoolEvent.ToggleQuarter(!state.thisQuarter)) })
            FilterChip("All branches", selected = state.allBranches, onClick = { onEvent(UnbilledPoolEvent.ToggleAllBranches(!state.allBranches)) })
            FilterChip("Over 30 days", selected = state.minAgeDays == 30, onClick = { onEvent(UnbilledPoolEvent.SetAgeFilter(if (state.minAgeDays == 30) null else 30)) })
            FilterChip("Over 60 days", selected = state.minAgeDays == 60, onClick = { onEvent(UnbilledPoolEvent.SetAgeFilter(if (state.minAgeDays == 60) null else 60)) })
        }
    }
}

@Composable
private fun EmptyPool(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Nothing waiting to be billed", style = TransportTypeScale.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Every TBB consignment this quarter is on a bill. Widen the period to see older ones.",
            style = TransportTypeScale.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun UnbilledPartyCard(
    party: UnbilledPartyState,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
    onToggleConsignment: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectCheckBox(checked = party.selected, onClick = onToggle)
            Spacer(Modifier.width(12.dp))
            Text(party.group.partyName, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(Money(party.group.totalPaise).formatted(), style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        val period = SimpleDateFormat("d MMM", Locale.ENGLISH)
        Text(
            "${party.group.consignments} consignments · ${period.format(party.group.firstBookedAt)} to ${period.format(party.group.lastBookedAt)}",
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 36.dp, top = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        AgeingBar(party.group)
        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand), horizontalArrangement = Arrangement.End) {
            Icon(
                if (party.expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                contentDescription = if (party.expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (party.expanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                party.rows.take(5).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SelectCheckBox(
                            checked = row.localId in party.selectedIds,
                            onClick = { onToggleConsignment(row.localId) },
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(row.displayNo, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text("${row.fromStation}–${row.toStation}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(Money(row.totalPaise).formatted(), style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (party.rows.size > 5) {
                    Text("Show all ${party.group.consignments}", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AgeingBar(group: com.example.transportapp.data.transport.billing.UnbilledPartyGroup) {
    val amber = transportColors().haulAmberContainer
    val green = MaterialTheme.colorScheme.primaryContainer
    val error = MaterialTheme.colorScheme.error
    val b0 = group.bucket0to30Paise
    val b1 = group.bucket31to60Paise
    val b2 = group.bucket60plusPaise
    val total = (b0 + b1 + b2).coerceAtLeast(1)
    val p0 = b0.toFloat() / total
    val p1 = b1.toFloat() / total
    Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(percent = 100)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        if (p0 > 0) Box(modifier = Modifier.fillMaxWidth(p0).height(8.dp).background(green))
        if (p1 > 0) Box(modifier = Modifier.fillMaxWidth(p1).height(8.dp).background(amber))
        if (b2 > 0) Box(modifier = Modifier.weight(1f).height(8.dp).background(error))
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("0–30 · 31–60 · 60+", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text("oldest ${group.oldestDays} days", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun SelectCheckBox(checked: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
    }
}
