package com.example.transportapp.feature.auth.screen

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.RouteLine
import com.example.transportapp.core.designsystem.component.RouteLineStep
import com.example.transportapp.core.designsystem.component.StepState
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.PaperColors

/**
 * T3 — Company setup wizard. Four steps + done frame.
 */
@Composable
fun SetupWizardScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Set up your company", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (step < 3) {
                AppTextButton("Skip for now", onClick = onSkip)
            }
        }

        // Wizard route line — 4 ticks, no truck
        val steps = List(4) { i ->
            RouteLineStep(
                label = listOf("Company", "Tax", "Branch", "Vehicle")[i],
                state = when {
                    i < step -> StepState.DONE
                    i == step -> StepState.CURRENT
                    else -> StepState.UPCOMING
                }
            )
        }
        RouteLine(steps = steps, showTruck = false, modifier = Modifier.padding(horizontal = 16.dp))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.fieldGap)
        ) {
            when (step) {
                0 -> StepCompany()
                1 -> StepTax()
                2 -> StepBranch()
                3 -> StepVehicle()
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
                when (step) { 0 -> "Next"; 1 -> "Next"; 2 -> "Next"; else -> "Finish setup" },
                onClick = { if (step < 3) step++ else onFinish() },
                modifier = Modifier.fillMaxWidth()
            )
            if (step == 3) {
                Text(
                    "Add vehicles later",
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
private fun StepCompany() {
    TransportTextField(value = "Shivshakti Roadlines", onValueChange = {}, label = "Company name")
    TransportTextField(value = "Plot 14, Transport Nagar, Indore, Madhya Pradesh 452003", onValueChange = {}, label = "Head office address", singleLine = false, maxLines = 3)
    TransportTextField(value = "+91 94250 61183", onValueChange = {}, label = "Phone")
    TransportTextField(value = "office@shivshaktiroadlines.in", onValueChange = {}, label = "Email")
    Spacer(Modifier.height(12.dp))
    GroupHeading("How it will print", modifier = Modifier.padding(bottom = 8.dp))
    // Letterhead preview (paper colours)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .background(PaperColors.paperWhite, RoundedCornerShape(2.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SHIVSHAKTI ROADLINES", color = PaperColors.paperInk, style = TransportTypeScale.titleMedium)
        Text("Plot 14, Transport Nagar, Indore 452003 · +91 94250 61183", color = PaperColors.paperInk, style = TransportTypeScale.bodySmall)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(PaperColors.paperRule))
        Spacer(Modifier.height(8.dp))
        Text("CONSIGNMENT NOTE", color = PaperColors.paperRule, style = TransportTypeScale.bodySmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StepTax() {
    TransportTextField(value = "23AABCS4521M1Z9", onValueChange = {}, label = "GSTIN", monospace = true)
    TransportTextField(value = "AABCS4521M", onValueChange = {}, label = "PAN", monospace = true)
    Spacer(Modifier.height(8.dp))
    GroupHeading("GST on freight", modifier = Modifier.padding(bottom = 8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text("We pay GST — 5% forward charge, no input credit", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        Text("The consignee pays under reverse charge", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "This decides how every freight bill is calculated. You can change it later in Company profile, and bilties already issued keep the treatment they were printed with.",
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
        Text("Confirm the current GTA rates with your CA before your first bill.", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StepBranch() {
    TransportTextField(value = "Indore", onValueChange = {}, label = "Branch name")
    TransportTextField(value = "Same as head office", onValueChange = {}, label = "Branch address")
    TransportTextField(value = "IND", onValueChange = {}, label = "Branch code", monospace = true)
    Spacer(Modifier.height(8.dp))
    GroupHeading("Bilty numbers from this branch", modifier = Modifier.padding(bottom = 8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Prefix IND", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Year 2627", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Digits 5", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Text("Next bilty will print as", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("IND/2627/00001", style = TransportTypeScale.dataLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun StepVehicle() {
    TransportTextField(value = "MH 15 BK 4412", onValueChange = {}, label = "Vehicle number", monospace = true)
    TransportTextField(value = "9000", onValueChange = {}, label = "Capacity", monospace = true)
    TransportTextField(value = "Gurmeet Singh", onValueChange = {}, label = "Driver name")
    TransportTextField(value = "+91 90280 41176", onValueChange = {}, label = "Driver phone")
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
        Text("Shivshakti Roadlines is ready", style = TransportTypeScale.headlineMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("You're the owner. Book your first bilty and all four copies print together.", style = TransportTypeScale.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        AppPrimaryButton("Book the first bilty", onClick = onBookFirst, modifier = Modifier.fillMaxWidth())
        AppTextButton("Go to dashboard", onClick = onGoDashboard)
    }
}