package com.example.transportapp.feature.booking.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transportapp.core.designsystem.component.RouteLine
import com.example.transportapp.core.designsystem.component.RouteLineStep
import com.example.transportapp.core.designsystem.component.StepState
import com.example.transportapp.core.designsystem.theme.PaperColors
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

@Composable
fun BiltyPreviewContent(
    state: BiltyPreviewUiState,
    onBack: () -> Unit,
    onPrint: () -> Unit,
    onShare: () -> Unit,
    onSaveNew: () -> Unit,
    onDone: () -> Unit
) {
    var currentCopy by remember { mutableIntStateOf(0) }
    val copies = listOf(
        CopyConfig("CONSIGNOR COPY", PaperColors.paperWhite, "Consignor copy"),
        CopyConfig("CONSIGNEE COPY", PaperColors.paperPink, "Consignee copy"),
        CopyConfig("DRIVER COPY", PaperColors.paperYellow, "Driver copy"),
        CopyConfig("OFFICE COPY", PaperColors.paperGreen, "Office copy")
    )
    val front = copies[currentCopy]

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.MoreVert, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) }
            Text("Bilty ${state.biltyNo}", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurface) }
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            for (i in 3 downTo 1) {
                Box(
                    modifier = Modifier
                        .offset(x = (i * 6).dp, y = (i * 6).dp)
                        .fillMaxWidth()
                        .aspectRatio(0.71f)
                        .shadow(1.dp, shape = RoundedCornerShape(2.dp), ambientColor = Color(0x14111111), spotColor = Color(0x14111111))
                        .background(copies[i].paper, RoundedCornerShape(2.dp))
                ) {
                    Text(copies[i].label, modifier = Modifier.align(Alignment.BottomEnd).rotate(90f).padding(end = 4.dp, bottom = 8.dp), color = PaperColors.paperInk, fontSize = 9.sp, letterSpacing = 1.sp)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.71f)
                    .shadow(1.dp, shape = RoundedCornerShape(2.dp), ambientColor = Color(0x14111111), spotColor = Color(0x14111111))
                    .background(front.paper, RoundedCornerShape(2.dp))
                    .padding(12.dp)
            ) {
                BiltyPaperContent(front.label)
            }
        }

        val pagerSteps = List(4) { i -> RouteLineStep("", when { i == currentCopy -> StepState.CURRENT; i < currentCopy -> StepState.DONE; else -> StepState.UPCOMING }) }
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RouteLine(pagerSteps, showTruck = false, showLabels = false, modifier = Modifier.width(88.dp))
            Text("${front.caption} · ${currentCopy + 1} of 4", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BiltyActionItem(Icons.Rounded.Print, "Print all 4", isPrimary = true, onClick = onPrint)
            BiltyActionItem(Icons.Rounded.Share, "Share PDF", onClick = onShare)
            BiltyActionItem(Icons.Rounded.NoteAdd, "Save and new", onClick = onSaveNew)
            BiltyActionItem(Icons.Rounded.Check, "Done", onClick = onDone)
        }
    }
}

private data class CopyConfig(val label: String, val paper: Color, val caption: String)

@Composable
fun BiltyPaperContent(copyLabel: String) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("SHIVSHAKTI ROADLINES", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PaperColors.paperInk, letterSpacing = 0.5.sp)
            Text("Plot 14, Transport Nagar, Indore 452003", fontSize = 8.sp, color = PaperColors.paperInk, textAlign = TextAlign.Center)
            Box(Modifier.fillMaxWidth().height(1.dp).padding(vertical = 4.dp).background(PaperColors.paperRule))
            Text("CONSIGNMENT NOTE", fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold, color = PaperColors.paperInk)
            Text(copyLabel, fontSize = 8.sp, letterSpacing = 1.sp, color = PaperColors.paperInk)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { PaperText("GR No.  IND/2627/04188"); PaperText("Date  25.08.2026") }
            Column(horizontalAlignment = Alignment.End) { PaperText("From  INDORE"); PaperText("To  NASHIK") }
        }
        Box(Modifier.fillMaxWidth().border(1.dp, PaperColors.paperRule, RoundedCornerShape(1.dp)).padding(6.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    PaperText("CONSIGNOR", bold = true); PaperText("Deepak Steel Traders")
                    PaperText("Indore · +91 94250 61183"); PaperText("GSTIN 23AACDS8812K1Z4")
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    PaperText("CONSIGNEE", bold = true); PaperText("Nashik Hardware Mart")
                    PaperText("Nashik · +91 98600 27419"); PaperText("GSTIN 27AAFCN3390L1Z8")
                }
            }
        }
        Box(Modifier.fillMaxWidth().border(1.dp, PaperColors.paperRule, RoundedCornerShape(1.dp)).padding(6.dp)) {
            Column {
                Row { listOf("Pkgs", "Description", "A. wt", "C. wt", "Rate", "Freight").forEach { Text(it, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = PaperColors.paperInk, modifier = Modifier.weight(1f)) } }
                Box(Modifier.fillMaxWidth().height(1.dp).padding(vertical = 2.dp).background(PaperColors.paperRule))
                Row { listOf("12", "MS PIPES", "780 kg", "780 kg", "4.50", "3,510.00").forEach { Text(it, fontSize = 8.sp, color = PaperColors.paperInk, modifier = Modifier.weight(1f)) } }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) { Text("Rupees three thousand nine hundred forty four only", fontSize = 8.sp, color = PaperColors.paperInk) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                PaperText("Hamali  96.00"); PaperText("Door delivery  150.00"); PaperText("Taxable  3,756.00")
                PaperText("GST 5%  187.80"); PaperText("Rounding  0.20")
                Box(Modifier.fillMaxWidth(0.6f).align(Alignment.End).height(1.dp).padding(vertical = 2.dp).background(PaperColors.paperRule))
                Text("TOTAL  3,944.00", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PaperColors.paperInk)
            }
        }
        Box(
            modifier = Modifier
                .rotate(-3f)
                .border(2.dp, PaperColors.stampViolet, RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text("TO PAY", color = PaperColors.stampViolet, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium)
        }
        Text("At owner's risk · Door delivery · Private mark DST-114 · E-way bill 281047556392", fontSize = 7.sp, color = PaperColors.paperInk)
    }
}

@Composable
private fun PaperText(text: String, bold: Boolean = false) {
    Text(text, fontSize = 9.sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal, color = PaperColors.paperInk)
}

@Composable
private fun BiltyActionItem(icon: ImageVector, label: String, isPrimary: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(8.dp)) {
        if (isPrimary) {
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(percent = 100)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        } else {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
        Text(label, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}