package com.example.transportapp.feature.reports.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppOutlinedButton
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PlexMonoFamily
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.ui.sample.SampleData

/**
 * T22 — Report viewer (Freight register). Frozen first column, pinned totals.
 */
@Composable
fun ReportViewerScreen(onBack: () -> Unit) {
    val rows = SampleData.freightRows
    val hScroll = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = "Freight register", onNavigationClick = onBack, trailingIcons = {
            IconButton(onClick = {}) { Icon(Icons.Rounded.Tune, contentDescription = "Filter", tint = MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = {}) { Icon(Icons.Rounded.FileDownload, contentDescription = "Export", tint = MaterialTheme.colorScheme.onSurface) }
        })
        Text("1 Apr – 25 Aug 2026 · Indore · 1,842 rows", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Dimens.screenPadding))

        // Applied filter chips
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Dimens.screenPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Indore branch", "To Pay only", "Over 1,000 kg").forEach {
                Row(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(percent = 100)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(it, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.Close, contentDescription = "Remove filter", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            Text("Clear all", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
        }

        // Header row (frozen Bilty no. column + scrollable rest)
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(vertical = 8.dp)) {
            Text("Bilty no.", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(140.dp).padding(start = 16.dp))
            Row(modifier = Modifier.horizontalScroll(hScroll)) {
                listOf("Date", "Consignor", "Consignee", "Route", "Pkg", "Weight", "Freight", "GST", "Total").forEach {
                    Text(it, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(100.dp))
                }
            }
        }

        // Rows
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp)) {
            itemsIndexed(rows) { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).background(if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.width(140.dp).padding(start = 16.dp)) {
                        Text(row.bilty, style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface)
                        if (row.cancelled) Text("CANCELLED", style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.error)
                    }
                    Row(modifier = Modifier.horizontalScroll(hScroll)) {
                        Text(row.date, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(100.dp))
                        Text(row.consignor, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(100.dp))
                        Text(row.consignee, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(100.dp))
                        Text(row.route, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(100.dp))
                        Text(row.pkg, style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(60.dp))
                        Text(row.weight, style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(80.dp))
                        Text(row.freight, style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(90.dp))
                        Text(row.gst, style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(80.dp))
                        Text(row.total, style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(90.dp))
                    }
                }
            }
        }

        // Pinned totals
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(vertical = 10.dp)) {
            Text("TOTAL · 1,842", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.width(140.dp).padding(start = 16.dp))
            Row(modifier = Modifier.horizontalScroll(hScroll)) {
                Text("", modifier = Modifier.width(100.dp))
                Text("", modifier = Modifier.width(100.dp))
                Text("", modifier = Modifier.width(100.dp))
                Text("", modifier = Modifier.width(100.dp))
                Text("18,204", style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.width(60.dp))
                Text("9,84,120 kg", style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.width(80.dp))
                Text("39,86,420.00", style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.width(90.dp))
                Text("1,99,321.00", style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.width(80.dp))
                Text("41,85,741.00", style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.width(90.dp))
            }
        }

        // Export bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppOutlinedButton("Export to Excel", onClick = {}, leadingIcon = Icons.Rounded.TableChart, modifier = Modifier.weight(1f), height = 48.dp)
            AppOutlinedButton("Export to PDF", onClick = {}, leadingIcon = Icons.Rounded.PictureAsPdf, modifier = Modifier.weight(1f), height = 48.dp)
        }
    }
}