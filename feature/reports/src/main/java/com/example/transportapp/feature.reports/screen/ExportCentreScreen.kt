package com.example.transportapp.feature.reports.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppOutlinedButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.RouteLine
import com.example.transportapp.core.designsystem.component.RouteLineStep
import com.example.transportapp.core.designsystem.component.StepState
import com.example.transportapp.core.designsystem.component.SegmentedControl
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PlexMonoFamily
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.core.ui.sample.SampleData

/**
 * T23 — Export centre. Build the workbook with 12 sheets, route-line progress.
 */
@Composable
fun ExportCentreScreen(onBack: () -> Unit) {
    var building by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = "Exports", onNavigationClick = onBack, trailingIcons = {
            IconButton(onClick = {}) { Icon(Icons.Rounded.History, contentDescription = "History", tint = MaterialTheme.colorScheme.onSurface) }
        })

        if (building) {
            // Build sheet
            BuildSheet(progress = progress, onCancel = { building = false })
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing)
            ) {
                // Section A — Build a pack
                GroupHeading("Build a pack")
                Column(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("FY 2026-27", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(8.dp))
                        Text("Q1 · Apr–Jun", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {}) { Icon(Icons.Rounded.DateRange, contentDescription = "Change period", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("WHAT TO INCLUDE", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SampleData.exportSheets.forEachIndexed { i, sheet ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(sheet, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Text("0", style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("Uncheck all", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("12 sheets · about 16,000 rows", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("FORMAT", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SegmentedControl(listOf("Excel (.xlsx)" to "Excel (.xlsx)", "CSV (zip)" to "CSV (zip)", "Tally XML" to "Tally XML"), "Excel (.xlsx)", {})
                    Text("Excel keeps one sheet per item and a cover sheet with your GSTIN and the period.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(16.dp))
                    AppPrimaryButton("Build the pack", onClick = { building = true; progress = 0 }, modifier = Modifier.fillMaxWidth())
                }

                // Section B — Recent exports
                GroupHeading("Recent exports")
                Column(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FileCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Shivshakti-FY2627-Q1.xlsx", style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface)
                            Text("Built 12 Jul 6:42 PM · 12 sheets · 2.1 MB · by Sunita Jain", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {}) { Icon(Icons.Rounded.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.FileCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f), modifier = Modifier.size(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Shivshakti-FY2627-Q1.xlsx", style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
                            Text("Removed after 30 days — build it again", style = TransportTypeScale.bodySmall, color = transportColors().haulAmber)
                        }
                        IconButton(onClick = {}) { Icon(Icons.Rounded.History, contentDescription = "Rebuild", tint = transportColors().haulAmber) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildSheet(progress: Int, onCancel: () -> Unit) {
    val steps = List(12) { i ->
        RouteLineStep("", when {
            i < progress -> StepState.DONE
            i == progress -> StepState.CURRENT
            else -> StepState.UPCOMING
        })
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(28.dp)).padding(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Building your pack", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(24.dp))
                RouteLine(steps, showTruck = true, showLabels = false, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Text("Writing sheet ${progress + 1} of 12 · ${SampleData.exportSheets.getOrElse(progress) { "..." }}", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("4,412 rows written", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().background(transportColors().haulAmberContainer, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Text("Keep this screen open. Large packs take about a minute.", style = TransportTypeScale.bodySmall, color = transportColors().onHaulAmber)
                }
                Spacer(Modifier.height(16.dp))
                AppOutlinedButton("Cancel", onClick = onCancel, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}