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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.ui.sample.SampleData

@Composable
fun MembersScreen(onBack: () -> Unit) {
    val members = SampleData.members
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
        TransportTopAppBar(title = "Members", onNavigationClick = onBack, trailingIcons = {
            IconButton(onClick = {}) { Icon(Icons.Rounded.HelpOutline, contentDescription = "Role matrix", tint = MaterialTheme.colorScheme.onSurface) }
        })

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = Dimens.screenPadding)) {
            Text("Active · 4", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text("Invited · 1", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            items(members) { member ->
                if (member.invited) {
                    InvitedMemberRow(member)
                } else {
                    ActiveMemberRow(member)
                }
            }
        }
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            AppPrimaryButton("Invite someone", onClick = {}, leadingIcon = Icons.Rounded.Add)
        }
    }
}

@Composable
private fun ActiveMemberRow(member: SampleData.MemberRow) {
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
private fun InvitedMemberRow(member: SampleData.MemberRow) {
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
        IconButton(onClick = {}) { Icon(Icons.Rounded.Refresh, contentDescription = "Resend", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        IconButton(onClick = {}) { Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error) }
    }
}