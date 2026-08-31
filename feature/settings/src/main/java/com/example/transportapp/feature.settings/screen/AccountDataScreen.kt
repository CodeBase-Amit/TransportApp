package com.example.transportapp.feature.settings.screen

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transportapp.core.designsystem.component.AppOutlinedButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.SyncChip
import com.example.transportapp.core.designsystem.component.SyncState
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * T31 — Account and data. Leaving and deleting are visibly different acts.
 */
@Composable
fun AccountDataScreen(
    onBack: () -> Unit,
    viewModel: AccountDataViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    AccountDataContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@Composable
fun AccountDataContent(
    state: AccountDataUiState,
    onEvent: (AccountDataEvent) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState())) {
        TransportTopAppBar(title = state.title, onNavigationClick = onBack)

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.screenPadding, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Section A — This phone
            GroupHeading("This phone")
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp)) {
                InfoRow("Records stored", state.records)
                InfoRow("Space used", state.space)
                InfoRow("Documents cached", state.cachedPdfs)
                InfoRow("Last full sync", state.lastSync)
                Spacer(Modifier.height(8.dp))
                Text(state.waiting, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.syncQueue.forEach { item ->
                    SyncQueueRow(item)
                }
                Text(state.syncNote, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                AppTextButton(state.tryNow, onClick = { onEvent(AccountDataEvent.TrySync) })
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(state.clearLabel, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(state.clearNote, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
                }
            }

            // Section B — Your data
            GroupHeading("Your data")
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp)) {
                DataRow(state.downloadLabel, state.downloadSub)
                DataRow(state.privacyLabel, null)
            }

            // Section C — Sign out
            GroupHeading("Sign out")
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(state.signOutLabel, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Text(state.signOutNote, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }

            // Section D — Leaving
            GroupHeading("Leaving")
            Column(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(20.dp)).padding(20.dp)
            ) {
                Text(state.leaveTitle, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(state.leaveBody, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(12.dp))
                AppOutlinedButton(state.leaveAction, onClick = { onEvent(AccountDataEvent.Leave) }, modifier = Modifier.fillMaxWidth(), borderColor = MaterialTheme.colorScheme.error, labelColor = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Text(state.deleteTitle, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(state.deleteBody, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                state.destroyLines.forEach { line ->
                    Text("> $line", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onEvent(AccountDataEvent.RequestDelete) }.background(MaterialTheme.colorScheme.error, RoundedCornerShape(percent = 100)).padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(state.deleteAction, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.onError)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (state.showDeleteDialog) {
        DeleteDialog(state, onEvent)
    }
}

@Composable
private fun SyncQueueRow(item: SyncQueueRowUi) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.description, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(item.atText, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SyncChip(state = if (item.state == "Syncing") SyncState.SYNCING else SyncState.PENDING)
    }
}

@Composable
private fun DeleteDialog(state: AccountDataUiState, onEvent: (AccountDataEvent) -> Unit) {
    var confirmName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onEvent(AccountDataEvent.CancelDelete) },
        title = { Text(state.deleteDialogTitle) },
        text = {
            Column {
                Text(state.deleteDialogBody, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.deleteCounts.forEach { count ->
                    Text("· $count", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(12.dp))
                TransportTextField(
                    value = confirmName,
                    onValueChange = { confirmName = it },
                    label = state.deleteDialogPlaceholder
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(AccountDataEvent.ConfirmDelete) },
                enabled = confirmName == state.deleteConfirmCompany
            ) {
                Text(state.deleteDialogAction, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(AccountDataEvent.CancelDelete) }) {
                Text(state.deleteDialogCancel)
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DataRow(label: String, sub: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = TransportTypeScale.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (sub != null) Text(sub, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountDataPreview() {
    TransportAppTheme {
        AccountDataContent(state = AccountDataUiState(), onEvent = {}, onBack = {})
    }
}
