package com.example.transportapp.feature.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
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
import com.example.transportapp.core.ui.sample.MemberRow

@Composable
fun MembersScreen(
    onBack: () -> Unit,
    viewModel: MembersViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    MembersContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@Composable
fun MembersContent(
    state: MembersUiState,
    onEvent: (MembersEvent) -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TransportTopAppBar(title = state.title, onNavigationClick = onBack, trailingIcons = {
                IconButton(onClick = { onEvent(MembersEvent.ToggleRoleMatrix) }) { Icon(Icons.Rounded.HelpOutline, contentDescription = "Role matrix", tint = MaterialTheme.colorScheme.onSurface) }
            })

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = Dimens.screenPadding)) {
                Text(state.activeTab, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(state.invitedTab, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (state.showRoleMatrix) {
                RoleMatrix(state)
            }

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                items(state.members) { member ->
                    if (member.invited) {
                        InvitedMemberRow(member, onEvent)
                    } else {
                        ActiveMemberRow(member)
                    }
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            AppPrimaryButton(state.inviteAction, onClick = { onEvent(MembersEvent.Invite) }, leadingIcon = Icons.Rounded.Add)
        }
    }
}

@Composable
private fun RoleMatrix(state: MembersUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenPadding, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Capability", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2f))
            state.roleColumns.drop(1).forEach { col ->
                Text(col, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        state.roleMatrix.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(row.capability, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(2f))
                row.marks.forEach { mark ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (mark) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Rounded.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        Text(state.roleMatrixNote, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun ActiveMemberRow(member: MemberRow) {
    Row(
        modifier = Modifier.fillMaxWidth().height(88.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
            Text(member.name.take(2), style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(member.name, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(member.email, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(member.scope, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(percent = 100)).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(member.role, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        if (member.isSelf) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp).padding(start = 4.dp))
        }
    }
}

@Composable
private fun InvitedMemberRow(member: MemberRow, onEvent: (MembersEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(88.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(member.email, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Invited ${member.invitedDate} by ${member.invitedBy} · ${member.role} · ${member.scope}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { onEvent(MembersEvent.Resend) }) { Icon(Icons.Rounded.Refresh, contentDescription = "Resend", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        IconButton(onClick = {}) { Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error) }
    }
}

@Preview(showBackground = true)
@Composable
private fun MembersPreview() {
    TransportAppTheme {
        MembersContent(state = MembersUiState(), onEvent = {}, onBack = {})
    }
}
