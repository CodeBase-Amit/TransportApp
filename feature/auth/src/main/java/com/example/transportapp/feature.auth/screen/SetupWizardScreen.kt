package com.example.transportapp.feature.auth.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.RouteLine
import com.example.transportapp.core.designsystem.component.RouteLineStep
import com.example.transportapp.core.designsystem.component.SegmentedControl
import com.example.transportapp.core.designsystem.component.StepState
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PaperColors
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.ui.sample.SetupWizardSampleData

/**
 * T3 — Company setup wizard. Four steps + done frame.
 */
@Composable
fun SetupWizardScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit,
    viewModel: SetupWizardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    SetupWizardContent(state = state, onEvent = viewModel::onEvent, onFinish = onFinish, onSkip = onSkip)
}

@Composable
fun SetupWizardContent(
    state: SetupWizardUiState,
    onEvent: (SetupWizardEvent) -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(state.title, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (state.step < state.stepLabels.lastIndex) {
                AppTextButton(state.skipLabel, onClick = onSkip)
            }
        }

        // Wizard route line — 4 ticks, no truck
        val steps = List(state.stepLabels.size) { i ->
            RouteLineStep(
                label = state.stepLabels[i],
                state = when {
                    i < state.step -> StepState.DONE
                    i == state.step -> StepState.CURRENT
                    else -> StepState.UPCOMING
                }
            )
        }
        RouteLine(steps = steps, showTruck = false, modifier = Modifier.padding(horizontal = 16.dp))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.fieldGap)
        ) {
            when (state.step) {
                0 -> StepCompany(state)
                1 -> StepTax(state, onEvent)
                2 -> StepBranch(state)
                3 -> StepVehicle(state, onEvent)
            }
        }

        // Sticky bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp)
        ) {
            AppPrimaryButton(
                when (state.step) { 0 -> state.nextLabel; 1 -> state.nextLabel; 2 -> state.nextLabel; else -> state.finishLabel },
                onClick = { if (state.step < state.stepLabels.lastIndex) onEvent(SetupWizardEvent.Next) else onFinish() },
                modifier = Modifier.fillMaxWidth()
            )
            if (state.step == state.stepLabels.lastIndex) {
                Text(
                    state.addVehiclesLaterLabel,
                    style = TransportTypeScale.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StepCompany(state: SetupWizardUiState) {
    TransportTextField(value = state.companyName, onValueChange = {}, label = "Company name")
    TransportTextField(value = state.headOffice, onValueChange = {}, label = "Head office address", singleLine = false, maxLines = 3)
    TransportTextField(value = state.phone, onValueChange = {}, label = "Phone")
    TransportTextField(value = state.email, onValueChange = {}, label = "Email")
    Spacer(Modifier.height(12.dp))
    GroupHeading(state.printHeading, modifier = Modifier.padding(bottom = 8.dp))
    // Letterhead preview (paper colours)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(PaperColors.paperWhite, RoundedCornerShape(2.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(state.printName, color = PaperColors.paperInk, style = TransportTypeScale.titleMedium)
        Text(state.headOffice, color = PaperColors.paperInk, style = TransportTypeScale.bodySmall, textAlign = TextAlign.Center)
        Text(state.printPhoneLabel, color = PaperColors.paperInk, style = TransportTypeScale.bodySmall)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(PaperColors.paperRule))
        Spacer(Modifier.height(8.dp))
        Text(state.printDoc, color = PaperColors.paperRule, style = TransportTypeScale.bodySmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StepTax(state: SetupWizardUiState, onEvent: (SetupWizardEvent) -> Unit) {
    TransportTextField(value = state.gstin, onValueChange = {}, label = "GSTIN", monospace = true)
    TransportTextField(value = state.pan, onValueChange = {}, label = "PAN", monospace = true)
    Spacer(Modifier.height(8.dp))
    GroupHeading(state.gstHeading, modifier = Modifier.padding(bottom = 8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        GstOptionRow(state.gstForwardLabel, selected = state.gstOption == 0, onClick = { onEvent(SetupWizardEvent.SelectGstOption(0)) })
        Spacer(Modifier.height(10.dp))
        GstOptionRow(state.gstReverseLabel, selected = state.gstOption == 1, onClick = { onEvent(SetupWizardEvent.SelectGstOption(1)) })
    }
    Spacer(Modifier.height(8.dp))
    Text(
        state.gstNote,
        style = TransportTypeScale.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(state.gstThirdParty, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun GstOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = TransportTypeScale.bodyMedium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StepBranch(state: SetupWizardUiState) {
    TransportTextField(value = state.branchName, onValueChange = {}, label = "Branch name")
    TransportTextField(value = state.branchAddress, onValueChange = {}, label = "Branch address")
    TransportTextField(value = state.branchCode, onValueChange = {}, label = "Branch code", monospace = true)
    Spacer(Modifier.height(8.dp))
    GroupHeading(state.branchHeading, modifier = Modifier.padding(bottom = 8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Prefix ${state.branchPrefix}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Year ${state.branchFy}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Digits ${state.branchDigits}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Text(state.nextBiltyLabel, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(state.nextBilty, style = TransportTypeScale.dataLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun StepVehicle(state: SetupWizardUiState, onEvent: (SetupWizardEvent) -> Unit) {
    TransportTextField(value = state.vehicleNumber, onValueChange = {}, label = "Vehicle number", monospace = true)
    Column {
        Text(state.ownershipLabel, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
        SegmentedControl(
            options = state.ownershipOptions.map { it to it },
            selected = state.ownership,
            onSelect = { onEvent(SetupWizardEvent.SelectOwnership(it)) }
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TransportTextField(value = state.capacity, onValueChange = {}, label = "Capacity", modifier = Modifier.weight(1f), monospace = true)
        Text(state.capacityUnit, style = TransportTypeScale.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    TransportTextField(value = state.driverName, onValueChange = {}, label = "Driver name")
    TransportTextField(value = state.driverPhone, onValueChange = {}, label = "Driver phone")
}

@Composable
fun SetupDoneFrame(onBookFirst: () -> Unit, onGoDashboard: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, contentDescription = "Done", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(SetupWizardSampleData.DONE_TITLE, style = TransportTypeScale.headlineMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(SetupWizardSampleData.DONE_BODY, style = TransportTypeScale.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        AppPrimaryButton(SetupWizardSampleData.DONE_PRIMARY, onClick = onBookFirst, modifier = Modifier.fillMaxWidth())
        AppTextButton(SetupWizardSampleData.DONE_SECONDARY, onClick = onGoDashboard)
    }
}
