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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.Caption
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.NestedCard
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors

/**
 * T26 — Branches. Each branch carries its own document series.
 */
@Composable
fun BranchesScreen(
    onBack: () -> Unit,
    viewModel: BranchesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    BranchesContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
    // S21: the add-branch dialog — name + code required, address optional.
    if (state.showAddBranch) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.onEvent(BranchesEvent.DismissAddBranch) },
            title = { Text("Add a branch", style = TransportTypeScale.titleMedium) },
            text = {
                Column {
                    com.example.transportapp.core.designsystem.component.TransportTextField(
                        value = state.branchName,
                        onValueChange = { viewModel.onEvent(BranchesEvent.ChangeBranchName(it)) },
                        label = "Branch name"
                    )
                    com.example.transportapp.core.designsystem.component.TransportTextField(
                        value = state.branchCode,
                        onValueChange = { viewModel.onEvent(BranchesEvent.ChangeBranchCode(it)) },
                        label = "Branch code (e.g. NAG)",
                        monospace = true,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    com.example.transportapp.core.designsystem.component.TransportTextField(
                        value = state.branchAddress,
                        onValueChange = { viewModel.onEvent(BranchesEvent.ChangeBranchAddress(it)) },
                        label = "Address (optional)",
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (state.error != null) {
                        Text(state.error ?: "", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = state.branchValid && !state.isSaving,
                    onClick = { viewModel.onEvent(BranchesEvent.SaveBranch) }
                ) { Text("Add branch", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.onEvent(BranchesEvent.DismissAddBranch) }) { Text("Cancel") }
            }
        )
    }
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
            Caption(state.subtitle, modifier = Modifier.padding(horizontal = Dimens.screenPadding))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.branches.forEach { branch ->
                    BranchCard(branch, state.headOfficeChip)
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(Dimens.screenPadding)) {
            AppPrimaryButton(
                state.addBranch,
                onClick = { onEvent(BranchesEvent.AddBranch) },
                leadingIcon = Icons.Rounded.Add
            )
        }
    }
}

@Composable
private fun BranchCard(branch: BranchRow, headOfficeChip: String) {
    ContentCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                branch.name,
                style = TransportTypeScale.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (branch.isHeadOffice) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.RoundedCornerShape(percent = 100))
                        .padding(horizontal = Dimens.fieldGap, vertical = 2.dp)
                ) {
                    Text(
                        headOfficeChip,
                        style = TransportTypeScale.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.weight(1f))
        }
        Text(
            branch.address,
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            BranchFigure("MEMBERS", branch.members.toString())
            BranchFigure("OPEN BILTIES", branch.openBiltes.toString())
            BranchFigure("TO PAY", branch.toPay)
        }
        Spacer(Modifier.height(8.dp))
        GroupHeading("SERIES")
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            branch.series.forEach { s ->
                NestedCard(
                    fill = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        s,
                        style = TransportTypeScale.dataSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        val noMembersLine = branch.noMembersLine
        if (noMembersLine != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        transportColors().haulAmberContainer,
                        androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    .padding(Dimens.cardPaddingNested),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    noMembersLine,
                    style = TransportTypeScale.bodySmall,
                    color = transportColors().onHaulAmber,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    branch.inviteText.orEmpty(),
                    style = TransportTypeScale.labelLarge,
                    color = transportColors().onHaulAmber
                )
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
