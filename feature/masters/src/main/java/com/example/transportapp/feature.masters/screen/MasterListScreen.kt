package com.example.transportapp.feature.masters.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.FilterChip
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.core.ui.sample.SampleData

/**
 * T18 — Master list (Parties). A-to-Z rail + inline duplicate merge offer.
 */
@Composable
fun MasterListScreen(masterType: String, onBack: () -> Unit, onRowClick: () -> Unit) {
    val parties = SampleData.partiesList

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
        TransportTopAppBar(title = "Parties", onNavigationClick = onBack, trailingIcons = {
            IconButton(onClick = {}) { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = {}) { Icon(Icons.Rounded.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onSurface) }
        })

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = Dimens.screenPadding)) {
            FilterChip("All 1,284", selected = true, onClick = {})
            FilterChip("Used this month 212", selected = false, onClick = {})
            FilterChip("Never used 64", selected = false, onClick = {})
            FilterChip("Possible duplicates 7", selected = false, onClick = {})
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item { Text("D", style = TransportTypeScale.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp)) }
            // Duplicate pair in a shared nested card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, transportColors().haulAmber, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(transportColors().haulAmberContainer).padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Same phone number", style = TransportTypeScale.labelMedium, color = transportColors().onHaulAmber, modifier = Modifier.weight(1f))
                        Text("Merge", style = TransportTypeScale.labelMedium, color = transportColors().onHaulAmber)
                    }
                    parties.filter { it.isDuplicate }.forEach { PartyRow(it) }
                }
            }
            items(parties.filter { !it.isDuplicate }) { party -> PartyRow(party) }
        }
        }

        // Extended FAB
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp)) {
            AppPrimaryButton("Add party", onClick = {}, leadingIcon = Icons.Rounded.Add)
        }
    }
}

@Composable
private fun PartyRow(party: SampleData.PartyListItem, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp).then(if (onClick != null) Modifier else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
            Text(party.initials, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(party.name, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(party.detail, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}