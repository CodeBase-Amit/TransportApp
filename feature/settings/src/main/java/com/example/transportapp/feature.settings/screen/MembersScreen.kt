package com.example.transportapp.feature.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

@Composable
fun MembersScreen(
    onBack: () -> Unit,
    viewModel: MembersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val inviteEmail by viewModel.inviteEmail.collectAsStateWithLifecycle()
    val inviteRole by viewModel.inviteRole.collectAsStateWithLifecycle()
    MembersContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
    // S19: the §17.4.1 invite dialog — email + role, Owner only, INVITED membership + outbox.
    if (state.showInvite) {
        val roles = listOf(
            "BOOKING_CLERK" to "Booking clerk",
            "DELIVERY_CLERK" to "Delivery clerk",
            "ACCOUNTANT" to "Accountant",
            "MANAGER" to "Manager",
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.onEvent(MembersEvent.DismissInvite) },
            title = { Text("Invite a member", style = TransportTypeScale.titleMedium) },
            text = {
                Column {
                    com.example.transportapp.core.designsystem.component.TransportTextField(
                        value = inviteEmail,
                        onValueChange = { viewModel.onEvent(MembersEvent.ChangeInviteEmail(it)) },
                        label = "Email address"
                    )
                    Text("Role", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    Column {
                        roles.forEach { (code, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onEvent(MembersEvent.ChangeInviteRole(code)) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(if (inviteRole == code) Icons.Rounded.Check else Icons.Rounded.Remove, contentDescription = null, tint = if (inviteRole == code) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(label, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    Text("They get an invitation valid for 5 days. Invite travels when the phone syncs.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    if (state.inviteError != null) {
                        Text(state.inviteError ?: "", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = android.util.Patterns.EMAIL_ADDRESS.matcher(inviteEmail).matches() && !state.isSaving,
                    onClick = { viewModel.onEvent(MembersEvent.SendInvite) }
                ) { Text("Send invite", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.onEvent(MembersEvent.DismissInvite) }) { Text("Cancel") }
            }
        )
    }
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
        // S27: the no-op Resend icon is gone — there is no resend API; the X cancels.
        IconButton(onClick = { onEvent(MembersEvent.CancelInvite(member.email)) }) { Icon(Icons.Rounded.Close, contentDescription = "Cancel invitation", tint = MaterialTheme.colorScheme.error) }
    }
}

@Preview(showBackground = true)
@Composable
private fun MembersPreview() {
    TransportAppTheme {
        MembersContent(state = MembersUiState(), onEvent = {}, onBack = {})
    }
}
