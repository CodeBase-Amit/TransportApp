package com.example.transportapp.feature.challan.screen

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowRightAlt
import androidx.compose.material.icons.rounded.Call
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PaperColors
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.domain.transport.ConsignmentStatus

@Composable
fun ChallanDetailScreen(
    onBack: () -> Unit,
    onCloseTrip: () -> Unit,
    viewModel: ChallanDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    ChallanDetailContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onCloseTrip = onCloseTrip
    )
}

@Composable
fun ChallanDetailContent(
    state: ChallanDetailUiState,
    onEvent: (ChallanDetailEvent) -> Unit,
    onBack: () -> Unit,
    onCloseTrip: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.MoreVert, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onEvent(ChallanDetailEvent.Print) }) { Icon(Icons.Rounded.Print, contentDescription = "Print", tint = MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = { onEvent(ChallanDetailEvent.More) }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurface) }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing)
        ) {
            item { ChallanDocketHeader(state) }
            item {
                Column {
                    GroupHeading(state.vehicleAndDriverHeading, modifier = Modifier.padding(bottom = 8.dp))
                    VehicleAndDriver(state)
                }
            }
            item {
                Column {
                    GroupHeading(state.whatsLoadedTitle, trailing = { Text(state.editLoad, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp)) })
                    ContentCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        state.challanGroups.forEach { group ->
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
                        Text(state.showAll, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item { PaperChallanPreview(state) }
        }

        if (state.isDispatched) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(transportColors().haulAmberContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(state.dispatchedNotice, style = TransportTypeScale.bodySmall, color = transportColors().onHaulAmber)
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
            ChallanAction(Icons.Rounded.Print, "Print", onClick = { onEvent(ChallanDetailEvent.Print) })
            ChallanAction(Icons.Rounded.Share, "Share", onClick = { onEvent(ChallanDetailEvent.Share) })
            if (state.isDispatched) {
                ChallanAction(Icons.Rounded.TaskAlt, "Close trip", isPrimary = true, onClick = { onEvent(ChallanDetailEvent.CloseTrip) })
            } else {
                ChallanAction(Icons.Rounded.LocalShipping, "Dispatch", isPrimary = true, onClick = { onEvent(ChallanDetailEvent.Dispatch) })
            }
        }
    }
}

@Composable
private fun ChallanDocketHeader(state: ChallanDetailUiState) {
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.challanNo, style = TransportTypeScale.dataLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            ChallanStatusPill(state.statusLabel)
        }
        Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(state.routeFrom, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.Rounded.ArrowRightAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp).size(24.dp))
            Text(state.routeTo, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Text(state.routeVia, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 4.dp))
        }
        Text(
            if (state.isDispatched) state.dispatchedLine else state.createdLine,
            style = TransportTypeScale.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            HeaderFigure("CONSIGNMENTS", state.consignments.toString())
            HeaderFigure("LOAD", state.loadKg)
            HeaderFigure("HIRE", state.hire)
            HeaderFigure("BALANCE", state.balance)
        }
    }
}

@Composable
private fun ChallanStatusPill(label: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(percent = 100))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(label, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun VehicleAndDriver(state: ChallanDetailUiState) {
    ContentCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.vehicleNumber, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(state.vehicleOwnership, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Text(state.driverInitials, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.Call, contentDescription = "Call driver", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(state.driverLine, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PaperChallanPreview(state: ChallanDetailUiState) {
    Column {
        GroupHeading("The paper", modifier = Modifier.padding(bottom = 8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaperColors.paperWhite, RoundedCornerShape(2.dp))
                .padding(16.dp)
        ) {
            Text(state.paperCompany, color = PaperColors.paperInk, fontWeight = FontWeight.Bold, style = TransportTypeScale.titleMedium)
            Text(state.paperDocType, color = PaperColors.paperInk, letterSpacing = 2.sp, style = TransportTypeScale.labelMedium)
            Text(state.paperChallanNo, color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
            Text(state.paperVehicle, color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
            Spacer(Modifier.height(8.dp))
            Text("Bilty / Dest / Weight", color = PaperColors.paperInk, style = TransportTypeScale.labelMedium)
            state.paperBiltyLines.forEach { line ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(line.bilty, color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
                    Text(line.dest, color = PaperColors.paperInk, style = TransportTypeScale.dataSmall, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                    Text(line.weight, color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
                }
            }
        }
        Text(state.paperSeeFull, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun ChallanAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ChallanDetailPreview() {
    com.example.transportapp.core.designsystem.theme.TransportAppTheme {
        ChallanDetailContent(
            state = ChallanDetailUiState(),
            onEvent = {},
            onBack = {},
            onCloseTrip = {}
        )
    }
}
