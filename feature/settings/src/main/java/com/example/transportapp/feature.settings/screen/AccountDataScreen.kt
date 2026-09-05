package com.example.transportapp.feature.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transportapp.core.designsystem.component.AppDestructiveButton
import com.example.transportapp.core.designsystem.component.AppOutlinedButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.Caption
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.StickyActionBar
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
 * D53: in debug builds the diagnostics card carries the hidden screen-map entry.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountDataScreen(
    onBack: () -> Unit,
    onOpenScreenMap: (() -> Unit)? = null,
    onSignedOut: () -> Unit = {},
    viewModel: AccountDataViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    androidx.compose.runtime.LaunchedEffect(state.signedOut) {
        if (state.signedOut) onSignedOut()
    }
    AccountDataContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onOpenScreenMap = onOpenScreenMap
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountDataContent(
    state: AccountDataUiState,
    onEvent: (AccountDataEvent) -> Unit,
    onBack: () -> Unit,
    onOpenScreenMap: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState())) {
        TransportTopAppBar(title = state.title, onNavigationClick = onBack)

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.screenPadding, vertical = Dimens.chipGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing)
        ) {
            // Section A — This phone
            GroupHeading("This phone")
            ContentCard(
                modifier = Modifier.fillMaxWidth(),
                onLongClick = onOpenScreenMap
            ) {
                InfoRow("Records stored", state.records)
                InfoRow("Space used", state.space)
                InfoRow("Documents cached", state.cachedPdfs)
                InfoRow("Last full sync", state.lastSync)
                Spacer(Modifier.height(Dimens.chipGap))
                Caption(state.waiting)
                state.syncQueue.forEach { item ->
                    SyncQueueRow(item)
                }
                Text(
                    state.syncNote,
                    style = TransportTypeScale.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                AppTextButton(state.tryNow, onClick = { onEvent(AccountDataEvent.TrySync) })
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = Dimens.chipGap)
                )
                // S27: the "Clear cached PDFs" row promised an action and fired nothing —
                // removed until a cache-clear exists (no PDF cache is kept yet).
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.clearLabel,
                        style = TransportTypeScale.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        state.clearNote,
                        style = TransportTypeScale.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Dimens.fieldGap)
                    )
                }
            }

            // Section B — Your data
            GroupHeading("Your data")
            // S27: both rows drew chevrons with no tap target (no download/export exists
            // yet, and Privacy Policy lives in sign-in's legal pages) — honest static copy.
            ContentCard(modifier = Modifier.fillMaxWidth()) {
                Text(state.downloadLabel, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(state.downloadSub, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(state.privacyLabel, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }

            // Section C — Sign out
            GroupHeading("Sign out")
            // S27: the sign-out card was display-only; it now signs out for real — the nav
            // graph routes the signed-out flag back to Splash (same wiring as T24).
            ContentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEvent(AccountDataEvent.SignOut) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(Dimens.fieldGap))
                    Text(
                        state.signOutLabel,
                        style = TransportTypeScale.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    state.signOutNote,
                    style = TransportTypeScale.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Section D — Leaving
            GroupHeading("Leaving")
            ContentCard(
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text(
                    state.leaveTitle,
                    style = TransportTypeScale.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    state.leaveBody,
                    style = TransportTypeScale.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(Dimens.fieldGap))
                AppOutlinedButton(
                    state.leaveAction,
                    onClick = { onEvent(AccountDataEvent.Leave) },
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = MaterialTheme.colorScheme.error,
                    labelColor = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(Dimens.sectionSpacing))
                Text(
                    state.deleteTitle,
                    style = TransportTypeScale.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    state.deleteBody,
                    style = TransportTypeScale.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                state.destroyLines.forEach { line ->
                    Text(
                        "> $line",
                        style = TransportTypeScale.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(Dimens.fieldGap))
                AppDestructiveButton(
                    text = state.deleteAction,
                    onClick = { onEvent(AccountDataEvent.RequestDelete) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(Dimens.sectionSpacing))
        }
    }

    if (state.showDeleteDialog) {
        DeleteDialog(state, onEvent)
    }
}

@Composable
private fun SyncQueueRow(item: SyncQueueRowUi) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Dimens.chipGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        title = { Text(state.deleteDialogTitle, style = TransportTypeScale.titleMedium) },
        text = {
            Column {
                Text(
                    state.deleteDialogBody,
                    style = TransportTypeScale.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.deleteCounts.forEach { count ->
                    Text(
                        "· $count",
                        style = TransportTypeScale.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(Dimens.fieldGap))
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
    Row(
        Modifier.fillMaxWidth().padding(vertical = Dimens.chipGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = TransportTypeScale.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = TransportTypeScale.dataSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DataRow(label: String, sub: String?) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Dimens.chipGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = TransportTypeScale.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (sub != null) {
                Text(sub, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountDataPreview() {
    TransportAppTheme {
        AccountDataContent(state = AccountDataUiState(), onEvent = {}, onBack = {})
    }
}
