package com.example.transportapp.feature.masters.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.FilterChip
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors

@Composable
fun MasterListScreen(
    masterType: String,
    onBack: () -> Unit,
    onRowClick: (String) -> Unit,
    onAddParty: () -> Unit,
    viewModel: MasterListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    MasterListContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onRowClick = onRowClick,
        onAddParty = onAddParty
    )
}

@Composable
fun MasterListContent(
    state: MasterListUiState,
    onEvent: (MasterListEvent) -> Unit,
    onBack: () -> Unit,
    onRowClick: (String) -> Unit,
    onAddParty: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TransportTopAppBar(title = state.title, onNavigationClick = onBack, trailingIcons = {
                IconButton(onClick = { onEvent(MasterListEvent.ToggleSearch) }) { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface) }
                IconButton(onClick = {}) { Icon(Icons.Rounded.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onSurface) }
            })

            if (state.isSearching) {
                TransportTextField(
                    value = state.query,
                    onValueChange = { onEvent(MasterListEvent.SearchQuery(it)) },
                    label = "Search by name or phone",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = Dimens.screenPadding)
            ) {
                state.filterOptions.forEachIndexed { index, label ->
                    FilterChip(
                        label = label,
                        selected = index == state.selectedFilterIndex,
                        onClick = { onEvent(MasterListEvent.SelectFilter(index)) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                item {
                    Text(
                        state.sectionHeader,
                        style = TransportTypeScale.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                item {
                    val duplicates = state.parties.filter { it.isDuplicate }
                    if (duplicates.isNotEmpty()) {
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
                                Text(state.duplicateBanner, style = TransportTypeScale.labelMedium, color = transportColors().onHaulAmber, modifier = Modifier.weight(1f))
                                Text(state.duplicateAction, style = TransportTypeScale.labelMedium, color = transportColors().onHaulAmber, modifier = Modifier.clickable { onEvent(MasterListEvent.MergeDuplicates) })
                            }
                            duplicates.forEach { PartyRow(it, onClick = { onRowClick(it.localId) }) }
                        }
                    }
                }
                items(state.parties.filter { !it.isDuplicate }) { party -> PartyRow(party, onClick = { onRowClick(party.localId) }) }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .fillMaxHeight()
                .padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            state.alphabet.forEachIndexed { index, letter ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onEvent(MasterListEvent.SelectLetter(index)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        letter,
                        style = TransportTypeScale.labelMedium,
                        color = if (index == state.selectedLetterIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp)) {
            AppPrimaryButton(state.addLabel, onClick = onAddParty, leadingIcon = Icons.Rounded.Add)
        }
    }
}

@Composable
private fun PartyRow(party: MasterListParty, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .height(72.dp),
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

@Preview(showBackground = true)
@Composable
private fun MasterListPreview() {
    TransportAppTheme {
        MasterListContent(
            state = MasterListUiState(),
            onEvent = {},
            onBack = {},
            onRowClick = {},
            onAddParty = {}
        )
    }
}
