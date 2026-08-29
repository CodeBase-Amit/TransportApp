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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PlexMonoFamily
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

@Composable
fun RateCardEditorScreen(onBack: () -> Unit, viewModel: RateCardEditorViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    RateCardEditorContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@Composable
fun RateCardEditorContent(
    state: RateCardEditorUiState,
    onEvent: (RateCardEditorEvent) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = state.title, onNavigationClick = onBack, trailingIcons = {})
        Text(
            state.subtitle,
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.screenPadding)
        )

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing)
        ) {
            GroupHeading(state.resolutionHeading)
            ContentCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.resolutionSteps.forEachIndexed { i, step ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(24.dp)
                                    .background(if (i < state.resolutionSteps.lastIndex) MaterialTheme.colorScheme.outlineVariant else Color.Transparent)
                            )
                            Spacer(Modifier.width(8.dp))
                            val sub = step.sub
                            Column(modifier = Modifier.weight(1f)) {
                                Text(step.label, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                if (sub != null) {
                                    Text(sub, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            GroupHeading(state.ratesHeading, trailing = { AppTextButton(state.addRate, onClick = { onEvent(RateCardEditorEvent.AddRate) }) })
            ContentCard(modifier = Modifier.fillMaxWidth()) {
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.horizontalScroll(scrollState)) {
                    Row(Modifier.width(560.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(vertical = 8.dp)) {
                        state.tableHeaders.forEach {
                            Text(it, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(headerWidth(it)))
                        }
                    }
                    state.rateRows.forEach { rate ->
                        Row(Modifier.width(560.dp).padding(vertical = 8.dp)) {
                            Text(rate.route, style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(140.dp))
                            Text(rate.goods, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(90.dp))
                            Text(rate.basis, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(90.dp))
                            Text(rate.rate, style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(80.dp))
                            Text(rate.min, style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(80.dp))
                        }
                        rate.note?.let { note ->
                            Text(note, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 140.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    AppTextButton(state.viewAll, onClick = { onEvent(RateCardEditorEvent.ViewAllRates) })
                }
            }

            GroupHeading(state.chargesHeading)
            ContentCard {
                state.charges.forEachIndexed { index, charge ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onEvent(RateCardEditorEvent.ToggleCharge(index)) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(charge.label, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(charge.detail, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(if (charge.isOn) "ON" else "OFF", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    state.chargesNote,
                    style = TransportTypeScale.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        AppPrimaryButton(state.saveLabel, onClick = { onEvent(RateCardEditorEvent.SaveRateCard) }, modifier = Modifier.fillMaxWidth().padding(16.dp))
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
