package com.example.transportapp.feature.masters.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.SegmentedControl
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

@Composable
fun MasterEditorScreen(masterType: String, onBack: () -> Unit, viewModel: MasterEditorViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    MasterEditorContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@Composable
fun MasterEditorContent(
    state: MasterEditorUiState,
    onEvent: (MasterEditorEvent) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface) }
            Text(state.title, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            AppTextButton(state.saveTopLabel, onClick = { onEvent(MasterEditorEvent.Save) })
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.fieldGap)
        ) {
            GroupHeading(state.identityHeading, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            TransportTextField(state.name, { onEvent(MasterEditorEvent.ChangeName(it)) }, "Party name")
            TransportTextField(state.email, { onEvent(MasterEditorEvent.ChangeEmail(it)) }, "Email")
            Text("Type", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SegmentedControl(state.typeOptions.map { it to it }, state.type, { onEvent(MasterEditorEvent.SelectType(it)) })
            TransportTextField(state.phone, { onEvent(MasterEditorEvent.ChangePhone(it)) }, "Phone", monospace = true)

            GroupHeading(state.addressHeading, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            TransportTextField(state.street, { onEvent(MasterEditorEvent.ChangeStreet(it)) }, "Address", singleLine = false, maxLines = 3)
            TransportTextField(state.station, { onEvent(MasterEditorEvent.ChangeStation(it)) }, "Station")
            TransportTextField(state.pincode, { onEvent(MasterEditorEvent.ChangePincode(it)) }, "Pincode", monospace = true)

            GroupHeading(state.taxHeading, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            TransportTextField(state.gstin, { onEvent(MasterEditorEvent.ChangeGstin(it)) }, "GSTIN", monospace = true)
            Text(state.taxStatus, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.primary)

            GroupHeading(state.defaultsHeading, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            TransportTextField(state.usualRoute, { onEvent(MasterEditorEvent.ChangeRoute(it)) }, "Usual route")
            Text("Usual payment mode", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SegmentedControl(state.paymentOptions.map { it to it }, state.payment, { onEvent(MasterEditorEvent.SelectPayment(it)) })
            TransportTextField(state.rateCard, { onEvent(MasterEditorEvent.ChangeRateCard(it)) }, "Rate card")
        }

        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)
        ) {
            AppPrimaryButton(state.saveLabel, onClick = { onEvent(MasterEditorEvent.Save) }, modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEvent(MasterEditorEvent.Delete) }
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text(state.deleteLabel, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.error)
            }
            Text(
                state.deleteMessage,
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MasterEditorPreview() {
    TransportAppTheme {
        MasterEditorContent(
            state = MasterEditorUiState(),
            onEvent = {},
            onBack = {}
        )
    }
}
