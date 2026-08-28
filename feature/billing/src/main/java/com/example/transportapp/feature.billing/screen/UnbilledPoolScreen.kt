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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.FilterChip
import com.example.transportapp.core.designsystem.component.SummaryStrip
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.core.ui.sample.SampleData

@Composable
fun UnbilledPoolScreen(onBack: () -> Unit, onBuildBill: () -> Unit) {
    var parties by remember { mutableStateOf(SampleData.unbilledParties) }
    val selected = parties.filter { it.selected }
    val selectedTotal = selected.sumOf { parseAmount(it.total) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = "Unbilled", onNavigationClick = onBack, trailingIcons = {
            IconButton(onClick = {}) { Icon(Icons.Rounded.Tune, contentDescription = "Filter", tint = MaterialTheme.colorScheme.onSurface) }
        })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = Dimens.screenPadding)) {
            FilterChip("This quarter", selected = true, onClick = {})
            FilterChip("All branches", selected = false, onClick = {})
            FilterChip("Over 30 days", selected = false, onClick = {})
            FilterChip("Over 60 days", selected = false, onClick = {})
        }
        SummaryStrip("PARTIES" to "18", "CONSIGNMENTS" to "214", "FREIGHT" to "3,86,540.00", modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(parties) { party ->
                UnbilledPartyCard(
                    party = party,
                    onToggle = { parties = parties.map { if (it.name == party.name) it.copy(selected = !it.selected) else it } },
                    onExpand = { parties = parties.map { if (it.name == party.name) it.copy(expanded = !it.expanded) else it } }
                )
            }
        }

        // Sticky bar
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("SELECTED", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatAmount(selectedTotal), style = TransportTypeScale.dataLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("${selected.sumOf { it.consignments }} consignments · ${selected.size} party", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppPrimaryButton("Build the bill", onClick = onBuildBill)
        }
    }
}

@Composable
private fun UnbilledPartyCard(party: SampleData.UnbilledParty, onToggle: () -> Unit, onExpand: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectCheckBox(checked = party.selected, onClick = onToggle)
            Spacer(Modifier.width(12.dp))
            Text(party.name, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(party.total, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Text("${party.consignments} consignments · ${party.period} · ${party.branches}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 36.dp, top = 4.dp))
        Spacer(Modifier.height(8.dp))
        AgeingBar(party)
        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand), horizontalArrangement = Arrangement.End) {
            if (party.expanded) {
                Icon(Icons.Rounded.ExpandMore, contentDescription = "Collapse", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Expand", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (party.expanded && party.rows.isNotEmpty()) {
            party.rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(36.dp))
                    Text(row.bilty, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text(row.route, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(row.amount, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Text("Show all ${party.consignments}", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AgeingBar(party: SampleData.UnbilledParty) {
    val amber = transportColors().haulAmberContainer
    val green = MaterialTheme.colorScheme.primaryContainer
    val error = MaterialTheme.colorScheme.error
    if (party.allOver60) {
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(percent = 100)).background(error))
    } else {
        val total = (party.ageBuckets.first + party.ageBuckets.second + party.ageBuckets.third).coerceAtLeast(1)
        val p1 = party.ageBuckets.first.toFloat() / total
        val p2 = party.ageBuckets.second.toFloat() / total
        Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(percent = 100)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            if (p1 > 0) Box(modifier = Modifier.fillMaxWidth(p1).height(8.dp).background(green))
            if (p2 > 0) Box(modifier = Modifier.fillMaxWidth(p2).height(8.dp).background(amber))
            if (party.ageBuckets.third > 0) Box(modifier = Modifier.weight(1f).height(8.dp).background(error))
        }
    }
    Text(
        if (party.allOver60) "All of this is over 60 days old" else "0–30 · 31–60 · 60+",
        style = TransportTypeScale.labelMedium,
        color = if (party.allOver60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
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
        contentAlignment = Alignment.Center
    ) {
        if (checked) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
    }
}

private fun parseAmount(s: String): Long = s.replace(",", "").toLongOrNull() ?: 0
private fun formatAmount(v: Long): String = v.toString().reversed().chunked(3).joinToString(",").reversed()