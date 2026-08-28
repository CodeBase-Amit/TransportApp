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
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
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
import com.example.transportapp.core.designsystem.component.AppOutlinedButton
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PaperColors
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.core.ui.sample.SampleData

enum class BillState { DRAFT, PREVIEW, ISSUED }

@Composable
fun FreightBillScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf(BillState.DRAFT) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface) }
            Text(if (state == BillState.ISSUED) "Freight bill" else "New freight bill", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurface) }
        }

        if (state == BillState.DRAFT) {
            // Draft bar
            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text("DRAFT · NOT ISSUED · NO NUMBER YET", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing)
        ) {
            when (state) {
                BillState.DRAFT -> DraftBillBody()
                BillState.PREVIEW -> BillPaperPreview(state = BillState.PREVIEW)
                BillState.ISSUED -> IssuedBillBody()
            }
        }

        // Sticky bar
        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)
        ) {
            when (state) {
                BillState.DRAFT -> AppPrimaryButton("Preview and issue", onClick = { state = BillState.PREVIEW }, modifier = Modifier.fillMaxWidth())
                BillState.PREVIEW -> {
                    Text("Issuing assigns FB/IND/2627/00311 and locks the 23 consignments. Needs a connection.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppOutlinedButton("Back to edit", onClick = { state = BillState.DRAFT }, modifier = Modifier.weight(1f))
                        AppPrimaryButton("Issue this bill", onClick = { state = BillState.ISSUED }, modifier = Modifier.weight(1f))
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
private fun DraftBillBody() {
    // Docket header with empty number slot
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(140.dp).height(20.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                Text("number on issue", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Text("UNPAID", style = TransportTypeScale.labelMedium, color = transportColors().haulAmber)
        }
        Text(SampleData.BILL_PARTY, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
        Text("Indore · GSTIN ${SampleData.BILL_GSTIN}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            BillFigure("CONSIGNMENTS", SampleData.BILL_CONSIGNMENTS.toString())
            BillFigure("PERIOD", SampleData.BILL_PERIOD)
            BillFigure("FREIGHT", SampleData.BILL_FREIGHT)
            BillFigure("GST 5%", SampleData.BILL_GST)
        }
    }

    Column {
        GroupHeading("What's on it · 23", trailing = { AppTextButton("Remove some", onClick = {}) }, modifier = Modifier.padding(bottom = 8.dp))
        ContentCard {
            listOf(
                Triple("IND/2627/04180", "Indore → Nashik · 25 Aug", "3,944.00"),
                Triple("IND/2627/04179", "Indore → Nashik · 24 Aug", "4,120.00"),
                Triple("IND/2627/04178", "Indore → Bhiwandi · 23 Aug", "6,750.00")
            ).forEach { (bilty, route, amount) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(bilty, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text(route, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(amount, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = {}) { Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                }
            }
            Text("Show all 23", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }

    Column {
        GroupHeading("Totals", modifier = Modifier.padding(bottom = 8.dp))
        ContentCard {
            BillTotals()
            Text("Every consignment on this bill uses the same GST treatment. Mixed treatments have to go on separate bills.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
        }
    }

    Column {
        GroupHeading("Terms", modifier = Modifier.padding(bottom = 8.dp))
        ContentCard {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Payment due", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(SampleData.BILL_DUE, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Notes on the bill", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text("Optional, printed under the totals", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BillTotals() {
    listOf(
        "Freight" to SampleData.BILL_FREIGHT,
        "Other charges" to "0.00",
        "Taxable" to SampleData.BILL_FREIGHT,
        "GST 5% — we pay, forward charge" to SampleData.BILL_GST
    ).forEach { (label, value) ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(value, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Total", style = TransportTypeScale.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(SampleData.BILL_TOTAL, style = TransportTypeScale.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun IssuedBillBody() {
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(SampleData.FREIGHT_BILL_NO, style = TransportTypeScale.dataLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Box(modifier = Modifier.rotate(-3f).border(2.dp, transportColors().haulAmber, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 2.dp)) {
                Text("UNPAID", color = transportColors().haulAmber, fontSize = 12.sp, letterSpacing = 1.2.sp)
            }
        }
        Text("issued 25 Aug 2026 by Mahesh Patidar · due 30 Sep 2026", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            BillFigure("CONSIGNMENTS", SampleData.BILL_CONSIGNMENTS.toString())
            BillFigure("PERIOD", SampleData.BILL_PERIOD)
            BillFigure("FREIGHT", SampleData.BILL_FREIGHT)
            BillFigure("GST 5%", SampleData.BILL_GST)
        }
    }
    Column {
        GroupHeading("Totals", modifier = Modifier.padding(bottom = 8.dp))
        ContentCard {
            BillTotals()
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).background(transportColors().haulAmberContainer, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Payments, contentDescription = null, tint = transportColors().onHaulAmber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("90,741.00 outstanding · nothing received yet", style = TransportTypeScale.bodyMedium, color = transportColors().onHaulAmber, modifier = Modifier.weight(1f))
            AppTextButton("Record a receipt", onClick = {}, color = transportColors().onHaulAmber)
        }
    }
}

@Composable
private fun BillPaperPreview(state: BillState) {
    Column(
        modifier = Modifier.fillMaxWidth().background(PaperColors.paperWhite, RoundedCornerShape(2.dp)).border(1.dp, PaperColors.paperRule, RoundedCornerShape(2.dp)).padding(16.dp)
    ) {
        Text("SHIVSHAKTI ROADLINES", color = PaperColors.paperInk, fontWeight = FontWeight.Bold, style = TransportTypeScale.titleMedium)
        Text("FREIGHT BILL", color = PaperColors.paperInk, letterSpacing = 2.sp, style = TransportTypeScale.labelMedium)
        if (state == BillState.PREVIEW) {
            Box(modifier = Modifier.rotate(-3f).border(2.dp, PaperColors.stampRed, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 2.dp)) {
                Text("DRAFT", color = PaperColors.stampRed, fontSize = 10.sp, letterSpacing = 1.2.sp)
            }
        }
        Text("Bill No. — issued on confirm", color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
        Text(SampleData.BILL_PARTY, color = PaperColors.paperInk, style = TransportTypeScale.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        Text("GSTIN ${SampleData.BILL_GSTIN}", color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
        Spacer(Modifier.height(8.dp))
        listOf("IND/2627/04180 · 3,944.00", "IND/2627/04179 · 4,120.00", "IND/2627/04178 · 6,750.00", "… and 18 more").forEach {
            Text(it, color = PaperColors.paperInk, style = TransportTypeScale.dataSmall)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(PaperColors.paperRule))
        Text("TOTAL  ${SampleData.BILL_TOTAL}", color = PaperColors.paperInk, fontWeight = FontWeight.Bold, style = TransportTypeScale.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        Text("Rupees ninety thousand seven hundred forty one only", color = PaperColors.paperInk, style = TransportTypeScale.bodySmall)
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