package com.example.transportapp.feature.settings.screen

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors

/**
 * T24 — Settings hub. Routes only; locked rows state who can change them.
 * S17: every row routes to its destination; SignOut confirms, clears the session and
 * rewinds to Splash via the [signedOut] one-shot.
 */
@Composable
fun SettingsHubScreen(
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onRowClick: (String) -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SettingsHubViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val signedOut by viewModel.signedOut.collectAsState()
    LaunchedEffect(signedOut) {
        if (signedOut) onSignedOut()
    }
    SettingsHubContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onProfile = onProfile,
        onRowClick = onRowClick
    )
}

@Composable
fun SettingsHubContent(
    state: SettingsHubUiState,
    onEvent: (SettingsHubEvent) -> Unit,
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onRowClick: (String) -> Unit
) {
    val isClerk = false // Owner view here
    var showSignOutDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState())) {
        TransportTopAppBar(title = state.title, onNavigationClick = onBack)

        // Identity card
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.screenPadding).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(16.dp).clickable { onProfile() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Text(state.identityInitials, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(state.identityName, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(state.identityEmail, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(state.identityRole, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        Spacer(Modifier.height(24.dp))

        state.groups.forEach { group ->
            Text(group.heading, style = TransportTypeScale.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp))
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.screenPadding).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(horizontal = 20.dp)
            ) {
                group.rows.forEach { row ->
                    val locked = row.locked && isClerk
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onRowClick(row.label) }.padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(iconGlyph(row.icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(row.label, style = TransportTypeScale.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        if (row.syncIcon) {
                            Box(modifier = Modifier.background(transportColors().haulAmberContainer, RoundedCornerShape(percent = 100)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("3 changes waiting", style = TransportTypeScale.labelMedium, color = transportColors().onHaulAmber)
                            }
                        } else if (row.value != null) {
                            Text(row.value!!, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (locked) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            if (row.gate != null) {
                                Text(row.gate!!, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Sign out
        Row(
            modifier = Modifier.fillMaxWidth().clickable { showSignOutDialog = true }.padding(horizontal = Dimens.screenPadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Rounded.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text(state.signOutLabel, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.error)
        }
        Text(state.signOutNote, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), textAlign = TextAlign.Center)
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(state.signOutLabel, style = TransportTypeScale.titleMedium) },
            text = { Text(state.signOutNote, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    onEvent(SettingsHubEvent.SignOut)
                }) { Text("Sign out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Stay signed in") }
            }
        )
    }
}

private fun iconGlyph(icon: String): ImageVector = when (icon) {
    "business" -> Icons.Rounded.Business
    "account_balance" -> Icons.Rounded.AccountBalance
    "group" -> Icons.Rounded.Group
    "numbers" -> Icons.Rounded.Pin
    "description" -> Icons.Rounded.Description
    "photo_camera" -> Icons.Rounded.PhotoCamera
    "print" -> Icons.Rounded.Print
    "article" -> Icons.AutoMirrored.Rounded.Article
    "language" -> Icons.Rounded.Language
    "text_fields" -> Icons.Rounded.TextFields
    "dark_mode" -> Icons.Rounded.DarkMode
    "sync" -> Icons.Rounded.Sync
    "folder" -> Icons.Rounded.Folder
    "help" -> Icons.Rounded.HelpOutline
    "info" -> Icons.Rounded.Info
    else -> Icons.Rounded.Business
}

@Preview(showBackground = true)
@Composable
private fun SettingsHubPreview() {
    TransportAppTheme {
        SettingsHubContent(
            state = SettingsHubUiState(),
            onEvent = {},
            onBack = {},
            onProfile = {},
            onRowClick = {}
        )
    }
}
