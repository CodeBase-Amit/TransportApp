package com.example.transportapp.feature.consignment.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowRightAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.JourneyChip
import com.example.transportapp.core.designsystem.component.NestedCard
import com.example.transportapp.core.designsystem.component.PaymentStamp
import com.example.transportapp.core.designsystem.component.RouteLine
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.core.ui.sample.SampleData

@Composable
fun CaseFileScreen(
    biltyNo: String,
    onBack: () -> Unit,
    onUpdateStatus: () -> Unit
) {
    val events = SampleData.caseFileEvents
    val journey = SampleData.journeySteps

    Column(modifier = Modifier.fillMaxSize()) {
        // Top app bar
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.MoreVert, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Rounded.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurface) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing)
        ) {
            item { DocketHeaderCard(biltyNo) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UpdateStatusButton(onClick = onUpdateStatus)
                }
            }
            item {
                Column {
                    GroupHeading("Where it is", modifier = Modifier.padding(bottom = 8.dp))
                    ContentCard {
                        RouteLine(steps = journey, orientation = com.example.transportapp.core.designsystem.component.RouteLineOrientation.VERTICAL)
                    }
                }
            }
            item {
                Column {
                    GroupHeading("The documents", modifier = Modifier.padding(bottom = 8.dp))
                    ContentCard {
                        DocumentRow(Icons.Rounded.Description, "Bilty", biltyNo, trailing = "4 copies")
                        DocumentRow(Icons.Rounded.LocalShipping, "Loading challan", "CHL/IND/2627/00742", trailing = "MH 15 BK 4412")
                        DocumentRow(Icons.Rounded.ReceiptLong, "Freight bill", "Not raised yet", trailing = "Raise", action = true)
                        DocumentRow(Icons.Rounded.TaskAlt, "POD", "Pending delivery", trailing = null)
                    }
                }
            }
            item {
                Column {
                    GroupHeading("The money", modifier = Modifier.padding(bottom = 8.dp))
                    ContentCard {
                        MoneyRow("Freight", "3,510.00")
                        MoneyRow("Charges", "246.00")
                        MoneyRow("GST 5%", "187.80")
                        MoneyRow("Total to collect", "3,944.00", strong = true)
                    }
                    NestedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        fill = transportColors().haulAmberContainer
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Payments, contentDescription = null, tint = transportColors().onHaulAmber, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "To Pay — collect 3,944.00 at Nashik before handing over the goods.",
                                style = TransportTypeScale.bodyMedium,
                                color = transportColors().onHaulAmber
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocketHeaderCard(biltyNo: String) {
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(biltyNo, style = TransportTypeScale.dataLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            PaymentStamp(mode = SampleData.PAYMENT_MODE)
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Indore", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.Rounded.ArrowRightAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp).size(24.dp))
            Text("Nashik", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Text("585 km", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            JourneyChip(status = SampleData.STATUS)
            Spacer(Modifier.width(8.dp))
            Text("booked 25 Aug, 11:42 AM by Mahesh Patidar", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UpdateStatusButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(percent = 100))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
        Text("Update status", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun DocumentRow(
    icon: ImageVector,
    title: String,
    detail: String,
    trailing: String?,
    action: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(detail, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) {
            Text(trailing, style = TransportTypeScale.labelMedium, color = if (action) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MoneyRow(label: String, value: String, strong: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = if (strong) TransportTypeScale.titleSmall else TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = if (strong) TransportTypeScale.titleSmall else TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}