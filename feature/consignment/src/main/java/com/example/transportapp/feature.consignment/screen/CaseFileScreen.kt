package com.example.transportapp.feature.consignment.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowRightAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.ReceiptLong
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.JourneyChip
import com.example.transportapp.core.designsystem.component.NestedCard
import com.example.transportapp.core.designsystem.component.PaymentStamp
import com.example.transportapp.core.designsystem.component.StepState
import com.example.transportapp.core.designsystem.component.SummaryStrip
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.core.ui.sample.CaseEvent

@Composable
fun CaseFileScreen(
    biltyNo: String,
    onBack: () -> Unit,
    onPrint: () -> Unit,
    onAddPhoto: () -> Unit,
    onHold: () -> Unit,
    onRaiseBill: () -> Unit,
    onFullHistory: () -> Unit,
    viewModel: CaseFileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    androidx.lifecycle.compose.LifecycleResumeEffect(biltyNo) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    CaseFileContent(
        state = state,
        biltyNo = biltyNo,
        onBack = onBack,
        onPrint = onPrint,
        onAddPhoto = onAddPhoto,
        onHold = onHold,
        onRaiseBill = onRaiseBill,
        onFullHistory = onFullHistory
    )
}

@Composable
fun CaseFileContent(
    state: CaseFileUiState,
    biltyNo: String,
    onBack: () -> Unit,
    onPrint: () -> Unit,
    onAddPhoto: () -> Unit,
    onHold: () -> Unit,
    onRaiseBill: () -> Unit,
    onFullHistory: () -> Unit
) {
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

        SummaryStrip(
            *state.stats.map { it.label to it.value }.toTypedArray(),
            modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Dimens.screenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing)
        ) {
            item { DocketHeaderCard(state, state.biltyNo.ifEmpty { biltyNo }) }
            item { CaseFileActions(onPrint, onAddPhoto, onHold, onRaiseBill) }
            item { WhereItIs(state, onFullHistory) }
            item { DocumentsSection(state) }
            item { MoneySection(state) }
            item { RecordSection(state) }
        }
    }
}

@Composable
private fun DocketHeaderCard(state: CaseFileUiState, biltyNo: String) {
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(biltyNo, style = TransportTypeScale.dataLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            PaymentStamp(mode = state.paymentMode)
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(state.fromStation, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.Rounded.ArrowRightAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp).size(24.dp))
            Text(state.toStation, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Text(state.distance, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            JourneyChip(status = state.status)
            Spacer(Modifier.width(8.dp))
            Text(state.bookedText, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CaseFileActions(onPrint: () -> Unit, onAddPhoto: () -> Unit, onHold: () -> Unit, onRaiseBill: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.chipGap)
    ) {
        CaseFileActionPill(Icons.Rounded.Print, "Print bilty", onPrint)
        CaseFileActionPill(Icons.Rounded.AddPhotoAlternate, "Add photo", onAddPhoto)
        CaseFileActionPill(Icons.Rounded.LocalShipping, "Hold", onHold)
        CaseFileActionPill(Icons.Rounded.ReceiptLong, "Raise bill", onRaiseBill)
    }
}

@Composable
private fun CaseFileActionPill(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 100))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
        Text(label, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun WhereItIs(state: CaseFileUiState, onFullHistory: () -> Unit) {
    Column {
        GroupHeading("WHERE IT IS", modifier = Modifier.padding(bottom = 8.dp))
        ContentCard {
            Column {
                state.events.forEachIndexed { index, event ->
                    EventTimelineRow(event = event, isLast = index == state.events.lastIndex)
                }
            }
        }
        AppTextButton(
            "Full history with locations",
            onClick = onFullHistory,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun EventTimelineRow(event: CaseEvent, isLast: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            EventDot(event.state)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(if (event.state == StepState.UPCOMING) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(event.name, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(event.time, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(event.station, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(event.actor, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EventDot(state: StepState) {
    when (state) {
        StepState.CURRENT -> Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
        StepState.DONE -> Box(
            modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
        )
        StepState.UPCOMING -> Box(
            modifier = Modifier.size(10.dp).border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
    }
}

@Composable
private fun DocumentsSection(state: CaseFileUiState) {
    Column {
        GroupHeading("The documents", modifier = Modifier.padding(bottom = 8.dp))
        ContentCard {
            state.documents.forEach { doc ->
                DocumentRow(
                    icon = when (doc.title) {
                        "Bilty" -> Icons.Rounded.Description
                        "Loading challan" -> Icons.Rounded.LocalShipping
                        "Freight bill" -> Icons.Rounded.ReceiptLong
                        else -> Icons.Rounded.TaskAlt
                    },
                    title = doc.title,
                    detail = doc.detail,
                    trailing = doc.trailing,
                    action = doc.action,
                )
            }
        }
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
private fun MoneySection(state: CaseFileUiState) {
    Column {
        GroupHeading("The money", modifier = Modifier.padding(bottom = 8.dp))
        ContentCard {
            state.moneyRows.forEach { line ->
                MoneyRow(line.label, line.value, line.strong)
            }
        }
        if (state.toPayCallout != null) {
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
                        state.toPayCallout,
                        style = TransportTypeScale.bodyMedium,
                        color = transportColors().onHaulAmber
                    )
                }
            }
        }
    }
}

/** Design T8 §RECORD — provenance lines, no card. */
@Composable
private fun RecordSection(state: CaseFileUiState) {
    Column {
        state.recordLines.forEach { line ->
            Text(
                line,
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
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
