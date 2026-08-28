package com.example.transportapp.feature.challan.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.JourneyChip
import com.example.transportapp.core.designsystem.component.PaymentStamp
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.core.ui.sample.SampleData

@Composable
fun ChallanBuilderScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpenTrip: () -> Unit
) {
    val items = SampleData.loadable
    var selected by remember { mutableStateOf(setOf("IND/2627/04188", "IND/2627/04191", "IND/2627/04192")) }

    val selectedWeight = items.filter { it.docNumber in selected }.sumOf {
        it.weight.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
    }
    val overloaded = selectedWeight > 9000

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("New challan", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text("CHL/IND/2627/00742", style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Load meter (pinned)
        LoadMeter(weight = selectedWeight, capacity = 9000, overloaded = overloaded, count = selected.size)

        // Pick list
        Column(modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp)) {
            GroupHeading("Ready to load · 23 at Indore", trailing = { Text("Select all", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary) })
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items) { item ->
                val isSelected = item.docNumber in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent)
                        .clickable {
                            selected = if (isSelected) selected - item.docNumber else selected + item.docNumber
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheckBox(isSelected)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(item.docNumber, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(item.amount, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("· ${item.consignee}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${item.packages} · ${item.weight}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            PaymentStamp(mode = item.paymentMode)
                            if (item.isOnwardLeg) {
                                Spacer(Modifier.width(8.dp))
                                JourneyChip(status = item.status)
                                Spacer(Modifier.width(4.dp))
                                Text(item.onwardNote ?: "", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Sticky bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp)
        ) {
            if (overloaded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(transportColors().haulAmberContainer, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = transportColors().onHaulAmber, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("$selectedWeight kg over capacity. A manager has to approve this challan before dispatch.", style = TransportTypeScale.bodyMedium, color = transportColors().onHaulAmber)
                }
                Spacer(Modifier.height(8.dp))
            }
            AppPrimaryButton(
                "Create challan · ${selected.size} consignments",
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Rounded.LocalShipping
            )
        }
    }
}

@Composable
private fun LoadMeter(weight: Int, capacity: Int, overloaded: Boolean, count: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("LOAD", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$weight / $capacity kg", style = TransportTypeScale.dataMedium, color = if (overloaded) transportColors().haulAmber else MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(8.dp))
        // Meter
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(percent = 100))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            val fillFraction = (weight.toFloat() / capacity).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillFraction)
                    .height(8.dp)
                    .background(if (overloaded) transportColors().haulAmber else MaterialTheme.colorScheme.primary, RoundedCornerShape(percent = 100))
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "$count consignments · 3 stations · freight 41,880.00",
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CheckBox(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
        }
    }
}