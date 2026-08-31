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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.common.Money
import com.example.transportapp.core.designsystem.component.AppOutlinedButton
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PaperColors
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * T14 — freight bill builder and detail (§12.1). The #DAE1DC draft bar is the draft's most
 * important element: a draft and an issued bill must never read as the same object.
 */
@Composable
fun FreightBillScreen(
    onBack: () -> Unit,
    onRecordReceipt: () -> Unit,
    viewModel: FreightBillViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    FreightBillContent(state = state, onEvent = viewModel::onEvent, onBack = onBack, onRecordReceipt = onRecordReceipt)
}

@Composable
fun FreightBillContent(
    state: FreightBillUiState,
    onEvent: (FreightBillEvent) -> Unit,
    onBack: () -> Unit,
    onRecordReceipt: () -> Unit,
) {
    val bill = state.bill
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface) }
            Text(
                if (state.stage == FreightBillUiState.Stage.ISSUED) "Freight bill" else "New freight bill",
                style = TransportTypeScale.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurface) }
        }

        if (state.stage == FreightBillUiState.Stage.DRAFT || state.stage == FreightBillUiState.Stage.PREVIEW) {
            Box(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("DRAFT · NOT ISSUED · NO NUMBER YET", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing),
        ) {
            when (state.stage) {
                FreightBillUiState.Stage.DRAFT -> DraftBillBody(state, onEvent)
                FreightBillUiState.Stage.PREVIEW -> BillPaperPreview(state)
                FreightBillUiState.Stage.ISSUED -> IssuedBillBody(state, onRecordReceipt)
            }
        }

        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)) {
            state.issueError?.let { message ->
                Text(
                    message,
                    style = TransportTypeScale.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }
            when (state.stage) {
                FreightBillUiState.Stage.DRAFT -> AppPrimaryButton("Preview and issue", onClick = { onEvent(FreightBillEvent.ShowPreview) }, modifier = Modifier.fillMaxWidth())
                FreightBillUiState.Stage.PREVIEW -> {
                    Text(
                        // No concrete number is promised: the whole reason issuing is server-side
                        // is that a number cannot be reserved offline (§9, §12.1).
                        "Issuing assigns the next FB number and locks the ${state.rows.size} consignments. Needs a connection.",
                        style = TransportTypeScale.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppOutlinedButton("Back to edit", onClick = { onEvent(FreightBillEvent.BackToEdit) }, modifier = Modifier.weight(1f))
                        AppPrimaryButton("Issue this bill", onClick = { onEvent(FreightBillEvent.Issue) }, modifier = Modifier.weight(1f))
                    }
                }
                FreightBillUiState.Stage.ISSUED -> Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    IssuedAction(Icons.Rounded.Print, "Print", onClick = {})
                    IssuedAction(Icons.Rounded.Share, "Share", onClick = {})
                    IssuedAction(Icons.Rounded.Payments, "Receipt", onClick = onRecordReceipt)
                    IssuedAction(Icons.Rounded.MoreVert, "More", onClick = {})
                }
            }
        }
    }
}

private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)

@Composable
private fun DraftBillBody(state: FreightBillUiState, onEvent: (FreightBillEvent) -> Unit) {
    val bill = state.bill ?: return
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.width(140.dp).height(20.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("number on issue", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
        }
        Text(bill.partyName, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
        Text(
            "Indore · GSTIN ${bill.partyGstin ?: "—"}",
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            BillFigure("CONSIGNMENTS", state.rows.size.toString())
            BillFigure("PERIOD", "${dateFormat.format(bill.periodStart)} – ${dateFormat.format(bill.periodEnd)}")
            BillFigure("FREIGHT", Money(bill.freightPaise).formatted())
            BillFigure("GST 5%", Money(bill.gstPaise).formatted())
        }
    }

    Column {
        GroupHeading("WHAT'S ON IT · ${state.rows.size}", modifier = Modifier.padding(bottom = 8.dp))
        ContentCard {
            state.rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(row.displayNo, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text(
                        "${row.fromStation}–${row.toStation} · ${dateFormat.format(row.bookedAt)}",
                        style = TransportTypeScale.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(Money(row.totalPaise).formatted(), style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = { onEvent(FreightBillEvent.RemoveRow(row.localId)) }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    Column {
        GroupHeading("TOTALS", modifier = Modifier.padding(bottom = 8.dp))
        ContentCard {
            BillTotals(state)
            Text(
                bill.gstTreatment + ". Every consignment on this bill carries the same treatment — mixed treatments have to go on separate bills.",
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }

    Column {
        GroupHeading("TERMS", modifier = Modifier.padding(bottom = 8.dp))
        ContentCard {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Payment due", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(
                    bill.dueAt?.let { "30 days · ${dateFormat.format(it)}" } ?: "—",
                    style = TransportTypeScale.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Notes on the bill", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(bill.notes ?: "Optional, printed under the totals", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BillTotals(state: FreightBillUiState) {
    val bill = state.bill ?: return
    listOf(
        "Freight" to Money(bill.freightPaise).formatted(),
        "Other charges" to Money(bill.otherChargesPaise).formatted(),
        "Taxable" to Money(bill.taxablePaise).formatted(),
        "GST — we pay, forward charge" to Money(bill.gstPaise).formatted(),
    ).forEach { (label, value) ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(value, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Total", style = TransportTypeScale.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(Money(bill.totalPaise).formatted(), style = TransportTypeScale.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun IssuedBillBody(state: FreightBillUiState, onRecordReceipt: () -> Unit) {
    val bill = state.bill ?: return
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(bill.billNo ?: "—", style = TransportTypeScale.dataLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Box(modifier = Modifier.rotate(-3f).border(2.dp, transportColors().haulAmber, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 2.dp)) {
                Text("UNPAID", color = transportColors().haulAmber, fontSize = 12.sp, letterSpacing = 1.2.sp)
            }
        }
        Text(
            buildString {
                append("issued ${bill.issuedAt?.let { dateFormat.format(it) } ?: "—"} by ${bill.issuedByName ?: "—"}")
                append(" · due ${bill.dueAt?.let { dateFormat.format(it) } ?: "—"}")
            },
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            BillFigure("CONSIGNMENTS", state.rows.size.toString())
            BillFigure("PERIOD", "${dateFormat.format(bill.periodStart)} – ${dateFormat.format(bill.periodEnd)}")
            BillFigure("FREIGHT", Money(bill.freightPaise).formatted())
            BillFigure("GST 5%", Money(bill.gstPaise).formatted())
        }
    }
    Column {
        GroupHeading("TOTALS", modifier = Modifier.padding(bottom = 8.dp))
        ContentCard {
            BillTotals(state)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).background(transportColors().haulAmberContainer, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Payments, contentDescription = null, tint = transportColors().onHaulAmber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "${Money(state.outstandingPaise).formatted()} outstanding of ${Money(bill.totalPaise).formatted()}",
                style = TransportTypeScale.bodyMedium,
                color = transportColors().onHaulAmber,
                modifier = Modifier.weight(1f),
            )
            com.example.transportapp.core.designsystem.component.AppTextButton("Record a receipt", onClick = onRecordReceipt, color = transportColors().onHaulAmber)
        }
        Column {
            state.rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(row.displayNo, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text("${row.fromStation}–${row.toStation}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(Money(row.totalPaise).formatted(), style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun BillPaperPreview(state: FreightBillUiState) {
    val bill = state.bill ?: return
    Column(
        modifier = Modifier.fillMaxWidth().background(PaperColors.paperWhite, RoundedCornerShape(2.dp)).border(1.dp, PaperColors.paperRule, RoundedCornerShape(2.dp)).padding(16.dp),
    ) {
        Text("SHIVSHAKTI ROADLINES", color = PaperColors.paperInk, fontWeight = FontWeight.Bold, style = TransportTypeScale.titleMedium)
        Text("FREIGHT BILL", color = PaperColors.paperInk, letterSpacing = 2.sp, style = TransportTypeScale.labelMedium)
        Box(modifier = Modifier.rotate(-3f).border(2.dp, PaperColors.stampRed, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 2.dp)) {
            Text("DRAFT", color = PaperColors.stampRed, fontSize = 10.sp, letterSpacing = 1.2.sp)
        }
        Text("Bill No. — issued on confirm", color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
        Text(bill.partyName, color = PaperColors.paperInk, style = TransportTypeScale.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        Text("GSTIN ${bill.partyGstin ?: "—"}", color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
        Spacer(Modifier.height(8.dp))
        state.rows.take(5).forEach {
            Text("${it.displayNo} · ${Money(it.totalPaise).formatted()}", color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
        }
        if (state.rows.size > 5) Text("… and ${state.rows.size - 5} more", color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(PaperColors.paperRule))
        Text("TOTAL  ${Money(bill.totalPaise).formatted()}", color = PaperColors.paperInk, fontWeight = FontWeight.Bold, style = TransportTypeScale.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        Text(Money(bill.totalPaise).inWordsLedger(), color = PaperColors.paperInk, style = TransportTypeScale.bodySmall)
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
