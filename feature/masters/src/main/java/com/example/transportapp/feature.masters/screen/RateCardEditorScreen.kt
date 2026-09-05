package com.example.transportapp.feature.masters.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.StickyActionBar
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PlexMonoFamily
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

@Composable
fun RateCardEditorScreen(onBack: () -> Unit, viewModel: RateCardEditorViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val newRate by viewModel.newRate.collectAsStateWithLifecycle()
    RateCardEditorContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
    // S21: the Add-rate dialog — rupee amount, copies basis/scope from existing rows.
    if (state.showAddRate) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.onEvent(RateCardEditorEvent.DismissAddRate) },
            title = { Text("Add a rate", style = TransportTypeScale.titleMedium) },
            text = {
                Column {
                    Text(
                        "The new rate follows this party's existing route and goods scope.",
                        style = TransportTypeScale.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    com.example.transportapp.core.designsystem.component.TransportTextField(
                        value = newRate,
                        onValueChange = { viewModel.onEvent(RateCardEditorEvent.ChangeNewRate(it)) },
                        label = "Rate (₹)",
                        monospace = true,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = (newRate.toLongOrNull() ?: 0L) > 0,
                    onClick = { viewModel.onEvent(RateCardEditorEvent.ConfirmAddRate) }
                ) { Text("Add rate", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.onEvent(RateCardEditorEvent.DismissAddRate) }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun RateCardEditorContent(
    state: RateCardEditorUiState,
    onEvent: (RateCardEditorEvent) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TransportTopAppBar(title = state.title, onNavigationClick = onBack, trailingIcons = {})
        Text(
            state.subtitle,
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.screenPadding)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing)
        ) {
            // Resolution steps section
            GroupHeading(state.resolutionHeading)
            ContentCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = Dimens.cardPaddingStandard
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.fieldGap)) {
                    state.resolutionSteps.forEachIndexed { i, step ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .width(Dimens.routeLineThickness)
                                    .height(24.dp)
                                    .background(
                                        if (i < state.resolutionSteps.lastIndex)
                                            MaterialTheme.colorScheme.outlineVariant
                                        else
                                            MaterialTheme.colorScheme.surface
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            val sub = step.note
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    step.label,
                                    style = TransportTypeScale.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (sub != null) {
                                    Text(
                                        sub,
                                        style = TransportTypeScale.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                Text(
                    state.resolutionIntro,
                    style = TransportTypeScale.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            // Rates table section
            GroupHeading(
                state.ratesHeading,
                trailing = {
                    AppTextButton(
                        state.addRate,
                        onClick = { onEvent(RateCardEditorEvent.AddRate) }
                    )
                }
            )
            ContentCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 0.dp
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                        .padding(horizontal = Dimens.cardPaddingNested)
                ) {
                    // Table header
                    Row(
                        Modifier
                            .width(560.dp)
                            .padding(vertical = Dimens.chipGap)
                    ) {
                        state.tableHeaders.forEach {
                            Text(
                                it,
                                style = TransportTypeScale.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(headerWidth(it))
                            )
                        }
                    }
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    // Table rows
                    state.rateRows.forEach { rate ->
                        Row(
                            Modifier
                                .width(560.dp)
                                .padding(vertical = Dimens.chipGap)
                        ) {
                            Text(
                                rate.route,
                                style = TransportTypeScale.dataSmall,
                                fontFamily = PlexMonoFamily,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(140.dp)
                            )
                            Text(
                                rate.goods,
                                style = TransportTypeScale.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(90.dp)
                            )
                            Text(
                                rate.basis,
                                style = TransportTypeScale.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(90.dp)
                            )
                            Text(
                                rate.rate,
                                style = TransportTypeScale.dataSmall,
                                fontFamily = PlexMonoFamily,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(80.dp)
                            )
                            Text(
                                rate.minQty,
                                style = TransportTypeScale.dataSmall,
                                fontFamily = PlexMonoFamily,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(80.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    AppTextButton(state.viewAll, onClick = { onEvent(RateCardEditorEvent.ViewAllRates) })
                }
            }

            // Charges section
            GroupHeading(state.chargesHeading)
            ContentCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = Dimens.cardPaddingStandard
            ) {
                state.charges.forEachIndexed { index, charge ->
                    if (index > 0) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onEvent(RateCardEditorEvent.ToggleCharge(index)) }
                            .padding(vertical = Dimens.fieldGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                charge.label,
                                style = TransportTypeScale.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                charge.value,
                                style = TransportTypeScale.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (charge.enabled) "ON" else "OFF",
                            style = TransportTypeScale.labelMedium,
                            color = if (charge.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    state.chargesNote,
                    style = TransportTypeScale.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.fieldGap)
                )
            }
        }

        // S27: the sticky "Save rate card" is gone — it rewrote unchanged rates (outbox
        // spam); AddRate persists immediately and is the only edit path.
    }
}

private fun headerWidth(header: String): androidx.compose.ui.unit.Dp = when (header) {
    "Route" -> 140.dp
    "Goods" -> 90.dp
    "Basis" -> 90.dp
    "Rate (₹)" -> 80.dp
    "Min Qty" -> 80.dp
    else -> 100.dp
}

@Preview(showBackground = true)
@Composable
private fun RateCardEditorPreview() {
    TransportAppTheme {
        RateCardEditorContent(
            state = RateCardEditorUiState(),
            onEvent = {},
            onBack = {}
        )
    }
}
