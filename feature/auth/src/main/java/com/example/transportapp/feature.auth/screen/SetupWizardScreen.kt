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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

/**
 * T3 — Company setup wizard. Four steps + done frame. S18: every field writes back through
 * [SetupWizardEvent.EditField]; Finish routes through the ViewModel so the company actually
 * persists (previously both wires were dead — AgentChanges S18).
 */
@Composable
fun SetupWizardScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit,
    viewModel: SetupWizardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
        if (state.justFinished) {
            // §6.1/T3: after the company persists, the done frame offers the next action.
            SetupDoneFrame(state = state, onBookFirst = onFinish, onGoDashboard = onFinish)
            return@Column
        }
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
                0 -> StepCompany(state, onEvent)
                1 -> StepTax(state, onEvent)
                2 -> StepBranch(state, onEvent)
                3 -> StepVehicle(state, onEvent)
            }
            if (state.error != null) {
                Text(
                    state.error,
                    style = TransportTypeScale.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
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
                // S18: Finish goes through the ViewModel — registerCompany must run.
                onClick = { if (state.step < state.stepLabels.lastIndex) onEvent(SetupWizardEvent.Next) else onEvent(SetupWizardEvent.Finish) },
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
private fun SetupDoneFrame(state: SetupWizardUiState, onBookFirst: () -> Unit, onGoDashboard: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(state.doneTitle, style = TransportTypeScale.headlineMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "You're the owner. Book your first bilty and all four copies print together.",
            style = TransportTypeScale.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        AppPrimaryButton("Book the first bilty", onClick = onBookFirst, modifier = Modifier.fillMaxWidth())
        AppTextButton("Go to dashboard", onClick = onGoDashboard, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StepCompany(state: SetupWizardUiState, onEvent: (SetupWizardEvent) -> Unit) {
    val edit = { field: SetupField -> { value: String -> onEvent(SetupWizardEvent.EditField(field, value)) } }
    TransportTextField(value = state.companyName, onValueChange = edit(SetupField.COMPANY_NAME), label = "Company name")
    TransportTextField(value = state.headOffice, onValueChange = edit(SetupField.HEAD_OFFICE), label = "Head office address", singleLine = false, maxLines = 3)
    TransportTextField(value = state.phone, onValueChange = edit(SetupField.PHONE), label = "Phone")
    TransportTextField(value = state.email, onValueChange = edit(SetupField.EMAIL), label = "Email")
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
        Text(state.companyName.uppercase().ifBlank { "YOUR COMPANY" }, color = PaperColors.paperInk, style = TransportTypeScale.titleMedium)
        Text(state.headOffice, color = PaperColors.paperInk, style = TransportTypeScale.bodySmall, textAlign = TextAlign.Center)
        if (state.phone.isNotBlank()) Text("Ph: ${state.phone}", color = PaperColors.paperInk, style = TransportTypeScale.bodySmall)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(PaperColors.paperRule))
        Spacer(Modifier.height(8.dp))
        Text(state.printDoc, color = PaperColors.paperRule, style = TransportTypeScale.bodySmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StepTax(state: SetupWizardUiState, onEvent: (SetupWizardEvent) -> Unit) {
    val edit = { field: SetupField -> { value: String -> onEvent(SetupWizardEvent.EditField(field, value)) } }
    TransportTextField(value = state.gstin, onValueChange = edit(SetupField.GSTIN), label = "GSTIN", monospace = true)
    TransportTextField(value = state.pan, onValueChange = edit(SetupField.PAN), label = "PAN", monospace = true)
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
private fun StepBranch(state: SetupWizardUiState, onEvent: (SetupWizardEvent) -> Unit) {
    val edit = { field: SetupField -> { value: String -> onEvent(SetupWizardEvent.EditField(field, value)) } }
    TransportTextField(value = state.branchName, onValueChange = edit(SetupField.BRANCH_NAME), label = "Branch name")
    TransportTextField(value = state.branchAddress, onValueChange = edit(SetupField.BRANCH_ADDRESS), label = "Branch address", singleLine = false, maxLines = 3)
    TransportTextField(value = state.branchCode, onValueChange = edit(SetupField.BRANCH_CODE), label = "Branch code (e.g. IND)", monospace = true)
    Spacer(Modifier.height(12.dp))
    GroupHeading(state.branchHeading, modifier = Modifier.padding(bottom = 8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text("${state.nextBiltyLabel}:", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(state.nextBilty, style = TransportTypeScale.dataLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Prefix ${state.branchCode.uppercase()}/${state.branchFyPart} · ${state.branchDigits} digits · resets each financial year",
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepVehicle(state: SetupWizardUiState, onEvent: (SetupWizardEvent) -> Unit) {
    val edit = { field: SetupField -> { value: String -> onEvent(SetupWizardEvent.EditField(field, value)) } }
    TransportTextField(value = state.vehicleNumber, onValueChange = edit(SetupField.VEHICLE_NUMBER), label = "Vehicle number", monospace = true)
    GroupHeading(state.ownershipLabel, modifier = Modifier.padding(top = 8.dp))
    SegmentedControl(
        options = state.ownershipOptions.map { it to it },
        selected = state.ownership,
        onSelect = { onEvent(SetupWizardEvent.SelectOwnership(it)) }
    )
    TransportTextField(value = state.capacity, onValueChange = edit(SetupField.CAPACITY), label = "Capacity (${state.capacityUnit})")
    TransportTextField(value = state.driverName, onValueChange = edit(SetupField.DRIVER_NAME), label = "Driver name")
    TransportTextField(value = state.driverPhone, onValueChange = edit(SetupField.DRIVER_PHONE), label = "Driver phone")
}
