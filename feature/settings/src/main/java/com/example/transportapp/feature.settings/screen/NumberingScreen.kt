package com.example.transportapp.feature.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PlexMonoFamily
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors

@Composable
fun NumberingScreen(
    onBack: () -> Unit,
    viewModel: NumberingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    NumberingContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@Composable
fun NumberingContent(
    state: NumberingUiState,
    onEvent: (NumberingEvent) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = state.title, onNavigationClick = onBack)
        Text(state.subtitle, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Dimens.screenPadding))

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.series.forEach { series ->
                SeriesCard(series, state.editLabel, state.isOwner, onEdit = { onEvent(NumberingEvent.StartCounterEdit(series.localId)) })
            }
        }
    }

    // §9 counter change: Owner-only, typed confirmation, forward-only (S19).
    state.counterEdit?.let { edit ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { onEvent(NumberingEvent.DismissCounterEdit) },
            title = { Text("Change the counter", style = TransportTypeScale.titleMedium) },
            text = {
                Column {
                    Text(edit.label, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "The counter is at ${edit.nextNumber.substringAfterLast('/')}. Moving it forward skips numbers; it can never move back.",
                        style = TransportTypeScale.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    TransportTextField(
                        value = edit.typed,
                        onValueChange = { onEvent(NumberingEvent.ChangeCounter(it)) },
                        label = "Type the new last-used number (${edit.digits} digits)",
                        monospace = true,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    if (state.error != null) {
                        Text(state.error, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = edit.valid && !state.isSaving,
                    onClick = { onEvent(NumberingEvent.ConfirmCounter) }
                ) { Text("Set counter", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { onEvent(NumberingEvent.DismissCounterEdit) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SeriesCard(series: SeriesRow, editLabel: String, isOwner: Boolean, onEdit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(series.label, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("NEXT NUMBER", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(series.nextNumber, style = TransportTypeScale.dataLarge, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column { Text("PREFIX ${series.prefix}", style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface); Text("Prefix", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Column { Text("FY ${series.fy}", style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface); Text("FY", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Column { Text("LAST USED ${series.lastUsed}", style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface); Text("Last used", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(series.caption, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            // §9: the counter change is Owner data (S19 wires the dead Edit button).
            if (isOwner) AppTextButton(editLabel, onClick = onEdit)
        }
        if (series.neverUsed) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(transportColors().haulAmberContainer, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(series.caption, style = TransportTypeScale.bodySmall, color = transportColors().onHaulAmber)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NumberingPreview() {
    TransportAppTheme {
        NumberingContent(state = NumberingUiState(), onEvent = {}, onBack = {})
    }
}
