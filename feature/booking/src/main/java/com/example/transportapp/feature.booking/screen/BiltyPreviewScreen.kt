package com.example.transportapp.feature.booking.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.RouteLine
import com.example.transportapp.core.designsystem.component.RouteLineStep
import com.example.transportapp.core.designsystem.component.StepState
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.AppShapes
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.HaulMotion
import com.example.transportapp.core.designsystem.theme.PaperColors
import com.example.transportapp.core.designsystem.theme.PlexMonoFamily
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.core.ui.PrintStatus
import com.example.transportapp.core.ui.sample.BiltyPaperData

@Composable
fun BiltyPreviewScreen(
    onBack: () -> Unit,
    onSaveNew: () -> Unit,
    onDone: () -> Unit,
    viewModel: BiltyPreviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val printStatus by viewModel.printStatus.collectAsStateWithLifecycle()
    BiltyPreviewContent(
        state = state,
        printStatus = printStatus,
        onBack = onBack,
        onPrint = viewModel::print,
        onShare = viewModel::share,
        onDismissPrintStatus = viewModel::dismissPrintStatus,
        onSaveNew = onSaveNew,
        onDone = onDone
    )
}

@Composable
fun BiltyPreviewContent(
    state: BiltyPreviewUiState,
    printStatus: PrintStatus,
    onBack: () -> Unit,
    onPrint: () -> Unit,
    onShare: () -> Unit,
    onDismissPrintStatus: () -> Unit,
    onSaveNew: () -> Unit,
    onDone: () -> Unit
) {
    var currentCopy by remember { mutableIntStateOf(0) }
    val paperColors = listOf(
        PaperColors.paperWhite,
        PaperColors.paperPink,
        PaperColors.paperYellow,
        PaperColors.paperGreen
    )
    val copies = state.copyConfigs
    val front = copies.getOrElse(currentCopy) { copies.first() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        var showCopyMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        TransportTopAppBar(
            title = "Bilty ${state.biltyNo}",
            onNavigationClick = onBack,
            trailingIcons = {
                // S21: the menu jumps straight to a copy — the four copies are the point.
                Box {
                    IconButton(onClick = { showCopyMenu = !showCopyMenu }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Choose copy", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    androidx.compose.material3.DropdownMenu(expanded = showCopyMenu, onDismissRequest = { showCopyMenu = false }) {
                        copies.forEachIndexed { index, copy ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(copy.label, style = TransportTypeScale.bodyMedium) },
                                onClick = {
                                    currentCopy = index
                                    showCopyMenu = false
                                }
                            )
                        }
                    }
                }
            }
        )

        // S27: a missing snapshot used to fall through and render the demo sample paper
        // under a real bilty number. Loading and errors are now honest frames.
        if (state.error != null) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = Dimens.screenPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    state.error,
                    style = TransportTypeScale.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        } else if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = Dimens.screenPadding),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = Dimens.screenPadding),
            contentAlignment = Alignment.Center
        ) {
            val paperShadow = transportColors().paperShadow
            // Stacked paper copies behind the front sheet
            for (i in 3 downTo 1) {
                Box(
                    modifier = Modifier
                        .offset(x = Dimens.paperStackOffset * i, y = Dimens.paperStackOffset * i)
                        .fillMaxWidth()
                        .aspectRatio(0.71f)
                        .shadow(Dimens.paperShadowOffset * 6, shape = AppShapes.paper, ambientColor = paperShadow, spotColor = paperShadow)
                        .background(paperColors.getOrElse(i) { PaperColors.paperWhite }, AppShapes.paper)
                ) {
                    Text(
                        copies.getOrElse(i) { front }.label,
                        modifier = Modifier.align(Alignment.BottomEnd).rotate(90f).padding(end = 4.dp, bottom = 8.dp),
                        color = PaperColors.paperInk,
                        style = TransportTypeScale.dataSmall
                    )
                }
            }
            // Front sheet
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.71f)
                    .shadow(10.dp, shape = AppShapes.paper, ambientColor = paperShadow, spotColor = paperShadow)
                    .background(paperColors.getOrElse(currentCopy) { PaperColors.paperWhite }, AppShapes.paper)
                    .padding(12.dp)
            ) {
                BiltyPaperContent(paper = state.paper, copyLabel = front.label)
            }
        }
        } // S27: closes the loaded-paper branch of the loading/error frame

        if (state.error == null && !state.isLoading) {
        val pagerSteps = List(4) { i -> RouteLineStep("", when { i == currentCopy -> StepState.CURRENT; i < currentCopy -> StepState.DONE; else -> StepState.UPCOMING }) }
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.fieldGap), horizontalAlignment = Alignment.CenterHorizontally) {
            RouteLine(pagerSteps, showTruck = false, showLabels = false, modifier = Modifier.width(88.dp))
            Text("${front.caption} · ${currentCopy + 1} of 4", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Dimens.grid))
        }
        }

        // Render/share status
        when (val status = printStatus) {
            is PrintStatus.Rendering -> LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.screenPadding).padding(bottom = 4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
            is PrintStatus.Error -> Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onDismissPrintStatus).padding(horizontal = Dimens.screenPadding, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(status.message, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                Text("Dismiss", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            PrintStatus.Idle -> Unit
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(vertical = Dimens.fieldGap, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BiltyActionItem(Icons.Rounded.Print, "Print", isPrimary = true, onClick = onPrint)
            BiltyActionItem(Icons.Rounded.Share, "Share", onClick = onShare)
            BiltyActionItem(Icons.Rounded.NoteAdd, "Save & new", onClick = onSaveNew)
            BiltyActionItem(Icons.Rounded.Check, "Done", onClick = onDone)
        }
    }
}

@Composable
fun BiltyPaperContent(paper: BiltyPaperData, copyLabel: String) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(paper.companyName, style = TransportTypeScale.bodySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp), color = PaperColors.paperInk)
            Text(paper.addressLine, style = TransportTypeScale.bodySmall.copy(fontSize = 8.sp), color = PaperColors.paperInk, textAlign = TextAlign.Center)
            Text(paper.contactLine, style = TransportTypeScale.bodySmall.copy(fontSize = 8.sp), color = PaperColors.paperInk, textAlign = TextAlign.Center)
            Box(Modifier.fillMaxWidth().height(1.dp).padding(vertical = 4.dp).background(PaperColors.paperRule))
            Text("CONSIGNMENT NOTE", style = TransportTypeScale.labelMedium.copy(fontSize = 10.sp, letterSpacing = 2.sp), color = PaperColors.paperInk)
            Text(copyLabel, style = TransportTypeScale.bodySmall.copy(fontSize = 8.sp, letterSpacing = 1.sp), color = PaperColors.paperInk)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                PaperText("GR No: ${paper.docNo}")
                PaperText("Date: ${paper.date}")
            }
            Column(horizontalAlignment = Alignment.End) {
                PaperText("From: ${paper.fromStation}")
                PaperText("To: ${paper.toStation}")
            }
        }
        Box(Modifier.fillMaxWidth().border(1.dp, PaperColors.paperRule, AppShapes.paper).padding(6.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    PaperText("CONSIGNOR", bold = true); PaperText(paper.consignorName)
                    PaperText(paper.consignorContact); PaperText(paper.consignorGstin); PaperText(paper.consignorAddress)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    PaperText("CONSIGNEE", bold = true); PaperText(paper.consigneeName)
                    PaperText(paper.consigneeContact); PaperText(paper.consigneeGstin); PaperText(paper.consigneeAddress)
                }
            }
        }
        Box(Modifier.fillMaxWidth().border(1.dp, PaperColors.paperRule, AppShapes.paper).padding(6.dp)) {
            Column {
                Row {
                    paper.goodsHeaders.forEach {
                        Text(it, style = TransportTypeScale.labelMedium.copy(fontSize = 8.sp), color = PaperColors.paperInk, modifier = Modifier.weight(1f))
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).padding(vertical = 2.dp).background(PaperColors.paperRule))
                Row {
                    paper.goodsValues.forEach {
                        Text(it, style = TransportTypeScale.bodySmall.copy(fontSize = 8.sp), color = PaperColors.paperInk, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(paper.amountInWords, style = TransportTypeScale.bodySmall.copy(fontSize = 8.sp), color = PaperColors.paperInk)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                PaperText("Hamali  ${paper.hamali}")
                PaperText("Door delivery  ${paper.doorDelivery}")
                PaperText("Taxable  ${paper.taxable}")
                PaperText("GST 5%  ${paper.gst}")
                PaperText("Rounding  ${paper.rounding}")
                Box(Modifier.fillMaxWidth(0.6f).align(Alignment.End).height(1.dp).padding(vertical = 2.dp).background(PaperColors.paperRule))
                Text(
                    "${paper.totalLabel}  ${paper.grandTotal}",
                    style = TransportTypeScale.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = PaperColors.paperInk
                )
            }
        }
        // Payment stamp — violet ink stamp rotated 3 degrees on the paper
        Box(
            modifier = Modifier
                .rotate(-3f)
                .border(2.dp, PaperColors.stampViolet, AppShapes.stamp)
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text(
                paper.stamp,
                style = TransportTypeScale.labelMedium.copy(
                    fontFamily = PlexMonoFamily,
                    letterSpacing = 1.2.sp
                ),
                color = PaperColors.stampViolet
            )
        }
        Text(paper.footer, style = TransportTypeScale.bodySmall.copy(fontSize = 7.sp), color = PaperColors.paperInk)
    }
}

@Composable
private fun PaperText(text: String, bold: Boolean = false) {
    Text(
        text,
        style = TransportTypeScale.bodySmall.copy(
            fontSize = 9.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
        ),
        color = PaperColors.paperInk
    )
}

@Composable
private fun BiltyActionItem(icon: ImageVector, label: String, isPrimary: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = HaulMotion.press,
        label = "actionPressScale",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clickable(
                onClick = onClick,
                interactionSource = interaction,
                indication = null
            )
            .padding(8.dp)
    ) {
        if (isPrimary) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(percent = 100)),
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
