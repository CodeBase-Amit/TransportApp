package com.example.transportapp.feature.masters.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppDestructiveButton
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.SegmentedControl
import com.example.transportapp.core.designsystem.component.StickyActionBar
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

@Composable
fun MasterEditorScreen(masterType: String, onBack: () -> Unit, viewModel: MasterEditorViewModel = hiltViewModel()) {
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
    Column(modifier = Modifier.fillMaxSize()) {
        TransportTopAppBar(
            title = state.title,
            onNavigationClick = onBack,
            trailingIcons = {
                AppTextButton(
                    state.saveTopLabel,
                    onClick = { onEvent(MasterEditorEvent.Save) },
                    color = MaterialTheme.colorScheme.primary
                )
            }
        )

        ContentCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenPadding),
            contentPadding = Dimens.cardPaddingStandard
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.fieldGap)
            ) {
                GroupHeading(state.identityHeading)
                TransportTextField(state.name, { onEvent(MasterEditorEvent.ChangeName(it)) }, "Party name")
                TransportTextField(state.email, { onEvent(MasterEditorEvent.ChangeEmail(it)) }, "Email")
                Text("Type", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SegmentedControl(state.typeOptions.map { it to it }, state.type, { onEvent(MasterEditorEvent.SelectType(it)) })
                TransportTextField(state.phone, { onEvent(MasterEditorEvent.ChangePhone(it)) }, "Phone", monospace = true)

                GroupHeading(state.addressHeading)
                TransportTextField(state.street, { onEvent(MasterEditorEvent.ChangeStreet(it)) }, "Address", singleLine = false, maxLines = 3)
                TransportTextField(state.station, { onEvent(MasterEditorEvent.ChangeStation(it)) }, "Station")
                TransportTextField(state.pincode, { onEvent(MasterEditorEvent.ChangePincode(it)) }, "Pincode", monospace = true)

                GroupHeading(state.taxHeading)
                TransportTextField(state.gstin, { onEvent(MasterEditorEvent.ChangeGstin(it)) }, "GSTIN", monospace = true)
                Text(state.taxStatus, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.primary)

                GroupHeading(state.defaultsHeading)
                TransportTextField(state.usualRoute, { onEvent(MasterEditorEvent.ChangeRoute(it)) }, "Usual route")
                Text("Usual payment mode", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SegmentedControl(state.paymentOptions.map { it to it }, state.payment, { onEvent(MasterEditorEvent.SelectPayment(it)) })
                TransportTextField(state.rateCard, { onEvent(MasterEditorEvent.ChangeRateCard(it)) }, "Rate card")
            }
        }

        StickyActionBar {
            AppPrimaryButton(
                state.saveLabel,
                onClick = { onEvent(MasterEditorEvent.Save) },
                modifier = Modifier.weight(1f)
            )
            AppDestructiveButton(
                state.deleteLabel,
                onClick = { onEvent(MasterEditorEvent.Delete) },
                modifier = Modifier.width(120.dp),
                filled = false
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
