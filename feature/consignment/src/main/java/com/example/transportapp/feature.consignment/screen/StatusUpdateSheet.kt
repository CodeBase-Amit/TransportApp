package com.example.transportapp.feature.consignment.screen

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
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.Caption
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

/**
 * T9 — Status update bottom sheet. The next event is a chip, not a dropdown.
 */
enum class StatusEventOption(val label: String, val detail: String, val holdPath: Boolean = false) {
    DEPARTED("Departed Dhule — back in transit", "The usual next step from At hub"),
    ARRIVED("Arrived at Nashik", "Reached the destination branch"),
    OUT_FOR_DELIVERY("Out for delivery", "Door delivery loaded out"),
    DELIVERED("Delivered", "POD captured"),
    HOLD("Hold", "Exception — needs a reason", holdPath = true),
    RETURN("Return to origin", "RTO decision")
}

enum class HoldReason(val label: String) { SHORTAGE("Shortage"), DAMAGE("Damage"), DETAINED("Detained"), OTHER("Other") }

@Composable
fun StatusUpdateSheet(
    biltyNo: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var selectedEvent by remember { mutableStateOf<StatusEventOption?>(StatusEventOption.DEPARTED) }
    var holdReason by remember { mutableStateOf<HoldReason>(HoldReason.SHORTAGE) }
    var remark by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Dhule") }
    val isHold = selectedEvent == StatusEventOption.HOLD

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 32.dp, height = 4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(percent = 100))
        )

        Text("Update status", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 12.dp))
        Text(
            "$biltyNo · Indore → Nashik · currently At hub, Dhule",
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(24.dp))
        GroupHeading("What happened", modifier = Modifier.padding(bottom = 8.dp))

        // Primary event card
        val primary = selectedEvent ?: StatusEventOption.DEPARTED
        val primaryBorder = if (selectedEvent == StatusEventOption.DEPARTED) {
            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
        } else Modifier
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                .then(primaryBorder)
                .clickable { selectedEvent = StatusEventOption.DEPARTED }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(StatusEventOption.DEPARTED.label, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(StatusEventOption.DEPARTED.detail, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(8.dp))
        Caption("Or choose another")
        Spacer(Modifier.height(8.dp))

        // Other event chips
        val otherEvents = listOf(StatusEventOption.ARRIVED, StatusEventOption.OUT_FOR_DELIVERY, StatusEventOption.DELIVERED)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            otherEvents.forEach { event ->
                StatusChip(
                    label = event.label,
                    selected = selectedEvent == event,
                    error = false,
                    onClick = { selectedEvent = event }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("Hold", selected = selectedEvent == StatusEventOption.HOLD, error = true, onClick = { selectedEvent = StatusEventOption.HOLD })
            StatusChip("Return to origin", selected = selectedEvent == StatusEventOption.RETURN, error = true, onClick = { selectedEvent = StatusEventOption.RETURN })
        }

        // Hold path
        if (isHold) {
            Spacer(Modifier.height(16.dp))
            GroupHeading("Why it's held", modifier = Modifier.padding(bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HoldReason.entries.forEach { reason ->
                    StatusChip(
                        label = reason.label,
                        selected = holdReason == reason,
                        error = true,
                        filled = true,
                        onClick = { holdReason = reason }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        GroupHeading("Where", modifier = Modifier.padding(bottom = 8.dp))
        TransportTextField(
            value = location,
            onValueChange = { location = it },
            label = "Location",
            leadingIcon = Icons.Rounded.LocationOn
        )
        Text("Recorded to the nearest town, not an exact position.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(24.dp))
        GroupHeading(if (isHold) "Remark · Required" else "Remark · Optional", modifier = Modifier.padding(bottom = 8.dp))
        TransportTextField(
            value = remark,
            onValueChange = { remark = it },
            label = "Anything the office should know",
            singleLine = false,
            maxLines = 3
        )

        Spacer(Modifier.height(24.dp))
        GroupHeading(if (isHold) "Photo · Required" else "Photo · Optional", modifier = Modifier.padding(bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CaptureTile(Icons.Rounded.PhotoCamera, "Camera")
            CaptureTile(Icons.Rounded.PhotoLibrary, "Gallery")
            Text(
                "Stored on this phone and uploaded when there's signal.",
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        if (isHold) {
            Text(
                "The office and the consignor are notified. This can't be undone, only followed by another event.",
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        AppPrimaryButton(
            if (isHold) "Hold this consignment" else "Save update",
            onClick = onSave,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Saves offline. Syncs when you reconnect.",
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatusChip(
    label: String,
    selected: Boolean,
    error: Boolean,
    onClick: () -> Unit,
    filled: Boolean = false
) {
    val container = when {
        filled && selected -> MaterialTheme.colorScheme.errorContainer
        filled -> MaterialTheme.colorScheme.errorContainer
        selected && !error -> MaterialTheme.colorScheme.secondaryContainer
        selected && error -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val labelColor = when {
        error -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (selected || error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .background(container, RoundedCornerShape(percent = 100))
            .then(if (selected || error) Modifier.border(1.dp, borderColor, RoundedCornerShape(percent = 100)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, style = TransportTypeScale.labelMedium, color = labelColor)
    }
}

@Composable
private fun CaptureTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(
        modifier = Modifier
            .size(88.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Text(label, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}