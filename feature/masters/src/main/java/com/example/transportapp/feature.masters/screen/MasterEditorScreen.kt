package com.example.transportapp.feature.masters.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
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
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.SegmentedControl
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

/**
 * T19 — Generic master editor. One layout serves eight of the nine master types
 * by rendering a different field list. Shown here as the Party frame.
 */
@Composable
fun MasterEditorScreen(masterType: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface) }
            Text("Edit party", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            AppTextButton("Save", onClick = {})
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.fieldGap)
        ) {
            GroupHeading("Identity", modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            TransportTextField("Deepak Steel Traders", {}, "Party name")
            Text("Type", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SegmentedControl(listOf("Consignor" to "Consignor", "Consignee" to "Consignee", "Both" to "Both"), "Both", {})
            TransportTextField("+91 94250 61183", {}, "Phone", monospace = true)

            GroupHeading("Address", modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            TransportTextField("Plot 14, Transport Nagar, Indore", {}, "Address", singleLine = false, maxLines = 3)
            TransportTextField("Indore", {}, "Station")
            TransportTextField("452003", {}, "Pincode", monospace = true)

            GroupHeading("Tax", modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            TransportTextField("23AACDS8812K1Z4", {}, "GSTIN", monospace = true)

            GroupHeading("Defaults for this party", modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            TransportTextField("Indore → Nashik", {}, "Usual route")
            Text("Usual payment mode", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SegmentedControl(listOf("Paid" to "Paid", "To Pay" to "To Pay", "TBB" to "TBB"), "TBB", {})
            TransportTextField("Deepak Steel Traders 2026-27", {}, "Rate card")
        }

        // Sticky bar with delete
        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)
        ) {
            AppPrimaryButton("Save party", onClick = {}, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("Delete this record", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.error)
            }
            Text("41 bilties use this party, so it can't be deleted. You can mark it inactive instead.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}