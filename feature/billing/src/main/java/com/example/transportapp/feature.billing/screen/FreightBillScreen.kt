package com.example.transportapp.feature.billing.screen

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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transportapp.core.designsystem.component.AppOutlinedButton
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PaperColors
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors

@Composable
fun FreightBillScreen(
    onBack: () -> Unit,
    viewModel: FreightBillViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    FreightBillContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@Composable
fun FreightBillContent(
    state: FreightBillUiState,
    onEvent: (FreightBillEvent) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface) }
            Text(if (state.state == BillState.ISSUED) "Freight bill" else "New freight bill", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurface) }
        }

        if (state.state == BillState.DRAFT) {
            // Draft bar
            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text(state.draftBar, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing)
        ) {
            when (state.state) {
                BillState.DRAFT -> DraftBillBody(state)
                BillState.PREVIEW -> BillPaperPreview(state, preview = true)
                BillState.ISSUED -> IssuedBillBody(state)
            }
        }

        // Sticky bar
        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)
        ) {
            when (state.state) {
                BillState.DRAFT -> AppPrimaryButton(state.previewAndIssue, onClick = { onEvent(FreightBillEvent.ChangeState(BillState.PREVIEW)) }, modifier = Modifier.fillMaxWidth())
                BillState.PREVIEW -> {
                    Text(state.issueNotice, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppOutlinedButton(state.backToEdit, onClick = { onEvent(FreightBillEvent.ChangeState(BillState.DRAFT)) }, modifier = Modifier.weight(1f))
                        AppPrimaryButton(state.issueThisBill, onClick = { onEvent(FreightBillEvent.ChangeState(BillState.ISSUED)) }, modifier = Modifier.weight(1f))
                    }
                }
                BillState.ISSUED -> Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    IssuedAction(Icons.Rounded.Print, "Print", onClick = {})
                    IssuedAction(Icons.Rounded.Share, "Share", onClick = {})
                    IssuedAction(Icons.Rounded.Payments, "Receipt", onClick = {})
                    IssuedAction(Icons.Rounded.MoreVert, "More", onClick = {})
                }
            }
        }
    }
}

@Composable
private fun DraftBillBody(state: FreightBillUiState) {
    // Docket header with empty number slot
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(140.dp).height(20.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                Text("number on issue", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Text("UNPAID", style = TransportTypeScale.labelMedium, color = transportColors().haulAmber)
        }
        Text(state.party, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
        Text("Indore · GSTIN ${state.gstin}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            BillFigure("CONSIGNMENTS", state.consignments.toString())
            BillFigure("PERIOD", state.period)
            BillFigure("FREIGHT", state.freight)
            BillFigure("GST 5%", state.gst)
        }
    }

    Column {
        GroupHeading(state.whatsOnTitle, trailing = { AppTextButton(state.removeSome, onClick = {}) }, modifier = Modifier.padding(bottom = 8.dp))
        ContentCard {
            state.removalRows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(row.bilty, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text("${row.route} · ${row.date}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(row.amount, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = {}) { Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                }
            }
            Text(state.showAll, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }

    Column {
        GroupHeading("Totals", modifier = Modifier.padding(bottom = 8.dp))
        ContentCard {
            BillTotals(state)
            Text("Every consignment on this bill uses the same GST treatment. Mixed treatments have to go on separate bills.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
        }
    }

    Column {
        GroupHeading("Terms", modifier = Modifier.padding(bottom = 8.dp))
        ContentCard {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Payment due", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(state.due, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Notes on the bill", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(state.notesOnBill, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BillTotals(state: FreightBillUiState) {
    listOf(
        "Freight" to state.freight,
        "Other charges" to "0.00",
        "Taxable" to state.freight,
        "GST 5% — we pay, forward charge" to state.gst
    ).forEach { (label, value) ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(value, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Total", style = TransportTypeScale.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(state.total, style = TransportTypeScale.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun IssuedBillBody(state: FreightBillUiState) {
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.issuedNo, style = TransportTypeScale.dataLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Box(modifier = Modifier.rotate(-3f).border(2.dp, transportColors().haulAmber, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 2.dp)) {
                Text("UNPAID", color = transportColors().haulAmber, fontSize = 12.sp, letterSpacing = 1.2.sp)
            }
        }
        Text(state.issuedLine, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            BillFigure("CONSIGNMENTS", state.consignments.toString())
            BillFigure("PERIOD", state.period)
            BillFigure("FREIGHT", state.freight)
            BillFigure("GST 5%", state.gst)
        }
    }
    Column {
        GroupHeading("Totals", modifier = Modifier.padding(bottom = 8.dp))
        ContentCard {
            BillTotals(state)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).background(transportColors().haulAmberContainer, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Payments, contentDescription = null, tint = transportColors().onHaulAmber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(state.outstandingLine, style = TransportTypeScale.bodyMedium, color = transportColors().onHaulAmber, modifier = Modifier.weight(1f))
            AppTextButton(state.recordReceipt, onClick = {}, color = transportColors().onHaulAmber)
        }
    }
}

@Composable
private fun BillPaperPreview(state: FreightBillUiState, preview: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().background(PaperColors.paperWhite, RoundedCornerShape(2.dp)).border(1.dp, PaperColors.paperRule, RoundedCornerShape(2.dp)).padding(16.dp)
    ) {
        Text(state.paperCompany, color = PaperColors.paperInk, fontWeight = FontWeight.Bold, style = TransportTypeScale.titleMedium)
        Text(state.paperDocType, color = PaperColors.paperInk, letterSpacing = 2.sp, style = TransportTypeScale.labelMedium)
        if (preview) {
            Box(modifier = Modifier.rotate(-3f).border(2.dp, PaperColors.stampRed, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 2.dp)) {
                Text("DRAFT", color = PaperColors.stampRed, fontSize = 10.sp, letterSpacing = 1.2.sp)
            }
        }
        Text("Bill No. — issued on confirm", color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
        Text(state.party, color = PaperColors.paperInk, style = TransportTypeScale.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        Text("GSTIN ${state.gstin}", color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
        Spacer(Modifier.height(8.dp))
        state.removalRows.forEach {
            Text("${it.bilty} · ${it.amount}", color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
        }
        Text("… and 18 more", color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(PaperColors.paperRule))
        Text("TOTAL  ${state.total}", color = PaperColors.paperInk, fontWeight = FontWeight.Bold, style = TransportTypeScale.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        Text(state.paperInvoiceLine, color = PaperColors.paperInk, style = TransportTypeScale.bodySmall)
    }
}

@Composable
private fun BillFigure(label: String, value: String) {
    Column {
        Text(label, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun IssuedAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Text(label, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun FreightBillPreview() {
    com.example.transportapp.core.designsystem.theme.TransportAppTheme {
        FreightBillContent(
            state = FreightBillUiState(),
            onEvent = {},
            onBack = {}
        )
    }
}
