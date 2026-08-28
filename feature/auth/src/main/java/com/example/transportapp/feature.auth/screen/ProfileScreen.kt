package com.example.transportapp.feature.auth.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
fun ProfileScreen(onBack: () -> Unit) {
    var clearSignal by remember { mutableIntStateOf(0) }
    var notifications by remember { mutableStateOf(listOf(true, true, false)) }

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
            Text("Your profile", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            AppTextButton("Save", onClick = {})
        }

        // Identity
        Row(modifier = Modifier.fillMaxWidth().height(96.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Text("MP", style = TransportTypeScale.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
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
                Text("Mahesh Patidar", style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("mahesh.patidar@gmail.com", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(percent = 100))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Text("Owner · Shivshakti Roadlines", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        GroupHeading("How you appear", modifier = Modifier.padding(bottom = 12.dp))
        TransportTextField(value = "Mahesh Patidar", onValueChange = {}, label = "Display name")
        Spacer(Modifier.height(12.dp))
        TransportTextField(value = "+91 94250 61183", onValueChange = {}, label = "Phone", monospace = true)

        Spacer(Modifier.height(24.dp))
        GroupHeading("How the app behaves", modifier = Modifier.padding(bottom = 12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Language", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            SegmentedControl(
                options = listOf("English" to "English", "Hindi" to "हिन्दी"),
                selected = "English",
                onSelect = {}
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text("Default branch", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text("Indore", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Open the booking form on launch", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("For clerks who only book. Skips the dashboard.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = false, onCheckedChange = {})
        }

        Spacer(Modifier.height(24.dp))
        GroupHeading("Delivery signature", modifier = Modifier.padding(bottom = 12.dp))
        SignaturePad(
            clearSignal = clearSignal,
            modifier = Modifier.fillMaxWidth().height(160.dp)
        )
        Row {
            AppTextButton("Clear", onClick = { clearSignal++ })
            AppTextButton("Redraw", onClick = { clearSignal++ })
        }
        Text("Printed in the receiver's box on the POD copy. Draw it once.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(24.dp))
        GroupHeading("Notify me about", modifier = Modifier.padding(bottom = 12.dp))
        NotificationRow("A consignment I booked is held", notifications[0]) { v -> notifications = notifications.toMutableList().also { it[0] = v } }
        NotificationRow("A vehicle is late", notifications[1]) { v -> notifications = notifications.toMutableList().also { it[1] = v } }
        NotificationRow("A freight bill I raised is paid", notifications[2]) { v -> notifications = notifications.toMutableList().also { it[2] = v } }

        Spacer(Modifier.height(24.dp))
        AppOutlinedButton("Sign out of this phone", onClick = {}, leadingIcon = Icons.Rounded.Sync, modifier = Modifier.fillMaxWidth())
        Text(
            "3 changes haven't synced yet. Sign out will wait for them.",
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