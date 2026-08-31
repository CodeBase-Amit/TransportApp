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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppOutlinedButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.SegmentedControl
import com.example.transportapp.core.designsystem.component.SignaturePad
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

/**
 * T33 — Your profile. The member's own profile (not the company's), with the
 * delivery signature drawn by finger.
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    ProfileContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.screenPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(state.title, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            AppTextButton(state.saveLabel, onClick = { onEvent(ProfileEvent.Save) })
        }

        // Identity
        Row(modifier = Modifier.fillMaxWidth().height(96.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Text(state.initials, style = TransportTypeScale.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.BottomEnd)
                        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = "Change photo", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(state.name, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(state.email, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(percent = 100))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Text(state.roleLine, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        GroupHeading(state.howYouAppear, modifier = Modifier.padding(bottom = 12.dp))
        TransportTextField(value = state.displayName, onValueChange = {}, label = "Display name")
        Spacer(Modifier.height(12.dp))
        TransportTextField(value = state.phone, onValueChange = {}, label = "Phone", monospace = true)

        Spacer(Modifier.height(24.dp))
        GroupHeading(state.howAppBehaves, modifier = Modifier.padding(bottom = 12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.languageLabel, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            SegmentedControl(
                options = state.languageOptions,
                selected = state.language,
                onSelect = { onEvent(ProfileEvent.ChangeLanguage(it)) }
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text(state.defaultBranchLabel, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(state.defaultBranch, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(state.openOnLaunchLabel, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(state.openOnLaunchCaption, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = state.openOnLaunch, onCheckedChange = { onEvent(ProfileEvent.ToggleOpenOnLaunch) })
        }

        Spacer(Modifier.height(24.dp))
        GroupHeading(state.signatureHeading, modifier = Modifier.padding(bottom = 12.dp))
        SignaturePad(
            clearSignal = state.clearSignal,
            modifier = Modifier.fillMaxWidth().height(160.dp)
        )
        Row {
            AppTextButton(state.clearLabel, onClick = { onEvent(ProfileEvent.Clear) })
            AppTextButton(state.redrawLabel, onClick = { onEvent(ProfileEvent.Redraw) })
        }
        Text(state.signatureCaption, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(24.dp))
        GroupHeading(state.notifyHeading, modifier = Modifier.padding(bottom = 12.dp))
        state.notifications.forEachIndexed { index, notification ->
            NotificationRow(notification.label, notification.on) { onEvent(ProfileEvent.ToggleNotification(index)) }
        }

        Spacer(Modifier.height(24.dp))
        AppOutlinedButton(state.signOutLabel, onClick = { onEvent(ProfileEvent.SignOut) }, leadingIcon = Icons.Rounded.Sync, modifier = Modifier.fillMaxWidth())
        Text(
            state.signOutCaption,
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NotificationRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
