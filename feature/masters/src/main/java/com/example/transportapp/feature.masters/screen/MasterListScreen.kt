package com.example.transportapp.feature.masters.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.FilterChip
import com.example.transportapp.core.designsystem.component.InitialsAvatar
import com.example.transportapp.core.designsystem.component.SearchField
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.HaulMotion
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
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TransportTopAppBar(title = state.title, onNavigationClick = onBack, trailingIcons = {
                SearchField(
                    value = state.query,
                    onValueChange = { onEvent(MasterListEvent.SearchQuery(it)) },
                    placeholder = "Search by name or phone",
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    onClear = { onEvent(MasterListEvent.SearchQuery("")) }
                )
            })

            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.chipGap),
                modifier = Modifier.padding(
                    horizontal = Dimens.screenPadding,
                    vertical = Dimens.chipGap
                )
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
                contentPadding = PaddingValues(horizontal = Dimens.screenPadding, vertical = Dimens.chipGap)
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
                        ContentCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Dimens.fieldGap),
                            fill = transportColors().haulAmberContainer,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                transportColors().haulAmber
                            ),
                            contentPadding = Dimens.cardPaddingNested
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    state.duplicateBanner,
                                    style = TransportTypeScale.labelMedium,
                                    color = transportColors().onHaulAmber,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    state.duplicateAction,
                                    style = TransportTypeScale.labelMedium,
                                    color = transportColors().onHaulAmber,
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onEvent(MasterListEvent.MergeDuplicates) }
                                )
                            }
                            duplicates.forEachIndexed { index, party ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                                PartyRow(party, onClick = { onRowClick(party.localId) })
                            }
                        }
                    }
                }
                items(state.parties.filter { !it.isDuplicate }) { party ->
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    PartyRow(party, onClick = { onRowClick(party.localId) })
                }
            }
        }

        // Alphabet sidebar with press feedback
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
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.85f else 1f,
                    animationSpec = HaulMotion.press,
                    label = "letterScale",
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .scale(scale)
                        .clickable(
                            interactionSource = interaction,
                            indication = null
                        ) { onEvent(MasterListEvent.SelectLetter(index)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        letter,
                        style = TransportTypeScale.labelMedium,
                        color = if (index == state.selectedLetterIndex)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = Dimens.screenPadding, bottom = Dimens.screenPadding)) {
            AppPrimaryButton(state.addLabel, onClick = onAddParty, leadingIcon = Icons.Rounded.Add)
        }
    }
}

@Composable
private fun PartyRow(party: MasterListParty, onClick: (() -> Unit)? = null) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = HaulMotion.press,
        label = "partyRowScale",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                enabled = onClick != null,
                interactionSource = interaction,
                indication = null
            ) { onClick?.invoke() }
            .height(Dimens.rowDouble),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InitialsAvatar(
            initials = party.initials,
            modifier = Modifier.padding(start = Dimens.grid)
        )
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
