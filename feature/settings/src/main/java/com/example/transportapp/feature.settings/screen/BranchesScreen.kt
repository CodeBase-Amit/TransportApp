package com.example.transportapp.feature.settings.screen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.core.ui.sample.BranchRow

/**
 * T26 — Branches. Each branch carries its own document series.
 */
@Composable
fun BranchesScreen(
    onBack: () -> Unit,
    viewModel: BranchesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    BranchesContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@Composable
fun BranchesContent(
    state: BranchesUiState,
    onEvent: (BranchesEvent) -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TransportTopAppBar(title = state.title, onNavigationClick = onBack)
            Text(state.subtitle, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Dimens.screenPadding))

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.branches.forEach { branch ->
                    BranchCard(branch, state.headOfficeChip)
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            AppPrimaryButton(state.addBranch, onClick = { onEvent(BranchesEvent.AddBranch) }, leadingIcon = Icons.Rounded.Add)
        }
    }
}

@Composable
private fun BranchCard(branch: BranchRow, headOfficeChip: String) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(branch.name, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            if (branch.isHeadOffice) {
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(percent = 100)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text(headOfficeChip, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Text(branch.address, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            BranchFigure("MEMBERS", branch.members.toString())
            BranchFigure("OPEN BILTIES", branch.openBiltes.toString())
            BranchFigure("TO PAY", branch.toPay)
        }
        Spacer(Modifier.height(8.dp))
        Text("SERIES", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            branch.series.forEach { s ->
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(percent = 100)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text(s, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        val noMembersLine = branch.noMembersLine
        if (noMembersLine != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(transportColors().haulAmberContainer, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(noMembersLine, style = TransportTypeScale.bodySmall, color = transportColors().onHaulAmber, modifier = Modifier.weight(1f))
                Text(branch.inviteText.orEmpty(), style = TransportTypeScale.labelLarge, color = transportColors().onHaulAmber)
            }
        }
    }
}

@Composable
private fun BranchFigure(label: String, value: String) {
    Column {
        Text(value, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
private fun BranchesPreview() {
    TransportAppTheme {
        BranchesContent(state = BranchesUiState(), onEvent = {}, onBack = {})
    }
}
