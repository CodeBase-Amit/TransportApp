package com.example.transportapp.feature.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppOutlinedButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.SyncChip
import com.example.transportapp.core.designsystem.component.SyncState
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.core.ui.sample.SampleData

/**
 * T31 — Account and data. Leaving and deleting are visibly different acts.
 */
@Composable
fun AccountDataScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState())) {
        TransportTopAppBar(title = "Account and data", onNavigationClick = onBack)

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.screenPadding, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Section A — This phone
            GroupHeading("This phone")
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp)) {
                InfoRow("Records stored", "6,412")
                InfoRow("Space used", "84 MB")
                InfoRow("Documents cached", "311 PDFs")
                InfoRow("Last full sync", "25 Aug 11:42 AM")
                Spacer(Modifier.height(8.dp))
                Text("WAITING TO SYNC · 3", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SampleData.syncQueue.forEach { item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.description, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(item.time, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        SyncChip(state = if (item.state == "Syncing") SyncState.SYNCING else SyncState.PENDING)
                    }
                }
                Text("They send themselves as soon as you have a connection. Nothing is lost by closing the app.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                AppTextButton("Try now", onClick = {})
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Clear cached PDFs", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Frees about 62 MB. They rebuild when printed again.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
                }
            }

            // Section B — Your data
            GroupHeading("Your data")
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp)) {
                DataRow("Download a copy of everything", "one Excel file, every record you can see")
                DataRow("What we store and why", "plain-language list")
                DataRow("Privacy policy", null)
            }

            // Section C — Sign out
            GroupHeading("Sign out")
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign out of this phone", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Text("Your 3 unsynced changes are sent first. Records stay on this phone in case you sign back in.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }

            // Section D — Leaving
            GroupHeading("Leaving")
            Column(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(20.dp)).padding(20.dp)
            ) {
                Text("Leave Shivshakti Roadlines", style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("You lose access. Everything you created stays with the company, with your name on it. Mahesh Patidar can invite you again.", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(12.dp))
                AppOutlinedButton("Leave this company", onClick = {}, modifier = Modifier.fillMaxWidth(), borderColor = MaterialTheme.colorScheme.error, labelColor = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Text("Delete the company and all its data", style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("6,412 records, 311 documents, 3 branches and 4 members are permanently removed. Documents already given to customers are not affected. This cannot be undone.", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(12.dp))
                AppDestructiveRow("Delete this company", onClick = {})
            }
            Spacer(Modifier.height(24.dp))
        }
    }
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
        androidx.compose.material3.Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AppDestructiveRow(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.error, RoundedCornerShape(percent = 100)).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.onError)
    }
}