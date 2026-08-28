package com.example.transportapp.feature.challan.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowRightAlt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TaskAlt
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.JourneyChip
import com.example.transportapp.core.designsystem.component.PaymentStamp
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PaperColors
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.core.ui.sample.SampleData
import com.example.transportapp.domain.transport.ConsignmentStatus

@Composable
fun ChallanDetailScreen(
    onBack: () -> Unit,
    onDispatch: () -> Unit,
    onCloseTrip: () -> Unit
) {
    var dispatched by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.MoreVert, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Rounded.Print, contentDescription = "Print", tint = MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurface) }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing)
        ) {
            item { ChallanDocketHeader(dispatched) }
            item { VehicleAndDriver() }
            item {
                Column {
                    GroupHeading("What's loaded · 14", trailing = { Text("Group by station", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary) })
                    ContentCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        SampleData.challanGroups.forEach { group ->
                            if (group.rows.isNotEmpty()) {
                                Text("${group.station} · ${group.count}", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                group.rows.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(row.bilty, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(Modifier.width(8.dp))
                                        Text(row.consignee, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                        Text(row.weight, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                        Text("Show all 14", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item { PaperChallanPreview() }
        }

        if (dispatched) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(transportColors().haulAmberContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Balance 6,500.00 payable to the driver when the trip closes.", style = TransportTypeScale.bodySmall, color = transportColors().onHaulAmber)
            }
        }

        // Sticky bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChallanAction(Icons.Rounded.Print, "Print", onClick = {})
            ChallanAction(Icons.Rounded.Share, "Share", onClick = {})
            if (dispatched) {
                ChallanAction(Icons.Rounded.TaskAlt, "Close trip", isPrimary = true, onClick = onCloseTrip)
            } else {
                ChallanAction(Icons.Rounded.LocalShipping, "Dispatch", isPrimary = true, onClick = { dispatched = true })
            }
        }
    }
}

@Composable
private fun ChallanDocketHeader(dispatched: Boolean) {
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(SampleData.CHALLAN_NO, style = TransportTypeScale.dataLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            JourneyChip(status = if (dispatched) ConsignmentStatus.IN_TRANSIT else ConsignmentStatus.DRAFT)
        }
        Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Indore", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.Rounded.ArrowRightAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp).size(24.dp))
            Text("Bhiwandi", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Text("via Dhule · 588 km", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            if (dispatched) SampleData.CHALLAN_DISPATCHED else SampleData.CHALLAN_CREATED,
            style = TransportTypeScale.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            HeaderFigure("CONSIGNMENTS", SampleData.CHALLAN_CONSIGNMENTS.toString())
            HeaderFigure("LOAD", SampleData.CHALLAN_LOAD_KG)
            HeaderFigure("HIRE", SampleData.CHALLAN_HIRE)
            HeaderFigure("BALANCE", SampleData.CHALLAN_BALANCE)
        }
    }
}

@Composable
private fun HeaderFigure(label: String, value: String) {
    Column {
        Text(label, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun VehicleAndDriver() {
    ContentCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(SampleData.VEHICLE, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text("Own · 9,000 kg", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Text("GS", style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.Call, contentDescription = "Call driver", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("${SampleData.DRIVER} · ${SampleData.DRIVER_PHONE}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PaperChallanPreview() {
    Column {
        GroupHeading("The paper", modifier = Modifier.padding(bottom = 8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(PaperColors.paperWhite, RoundedCornerShape(2.dp))
                .padding(16.dp)
        ) {
            Text("SHIVSHAKTI ROADLINES", color = PaperColors.paperInk, fontWeight = FontWeight.Bold, style = TransportTypeScale.titleMedium)
            Text("LOADING CHALLAN", color = PaperColors.paperInk, letterSpacing = 2.sp, style = TransportTypeScale.labelMedium)
            Text("Challan No. ${SampleData.CHALLAN_NO}", color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
            Text("Vehicle ${SampleData.VEHICLE}", color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
        }
        Text("See full challan", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun ChallanAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
        if (isPrimary) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(percent = 100)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        } else {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
        Text(label, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}