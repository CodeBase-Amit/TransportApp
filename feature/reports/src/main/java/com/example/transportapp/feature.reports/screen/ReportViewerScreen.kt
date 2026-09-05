package com.example.transportapp.feature.reports.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.transportapp.core.designsystem.component.AppOutlinedButton
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.AppShapes
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PlexMonoFamily
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ReportViewerScreen(onBack: () -> Unit, viewModel: ReportViewerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    ReportViewerContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@Composable
fun ReportViewerContent(
    state: ReportViewerUiState,
    onEvent: (ReportViewerEvent) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = state.title, onNavigationClick = onBack, trailingIcons = {
            // S27: the Tune icon fired a VM no-op — removed until report filters exist.
            IconButton(onClick = { onEvent(ReportViewerEvent.ExportExcel) }) {
                Icon(
                    Icons.Rounded.FileDownload,
                    contentDescription = "Export",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        })

        Text(
            state.subtitle,
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.screenPadding)
        )

        // Active filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(
                    horizontal = Dimens.screenPadding,
                    vertical = Dimens.chipGap
                ),
            horizontalArrangement = Arrangement.spacedBy(Dimens.chipGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            state.filters.forEach { filter ->
                Row(
                    modifier = Modifier
                        .height(Dimens.filterChipHeight)
                        .clip(RoundedCornerShape(percent = 100))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        filter,
                        style = TransportTypeScale.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Remove filter",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onEvent(ReportViewerEvent.RemoveFilter(filter)) }
                    )
                }
            }

            if (state.filters.isNotEmpty()) {
                Text(
                    state.clearAll,
                    style = TransportTypeScale.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onEvent(ReportViewerEvent.ClearAll) }
                )
            }
        }

        // Table header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(vertical = Dimens.chipGap)
        ) {
            Text(
                state.columns[0],
                style = TransportTypeScale.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(140.dp).padding(start = Dimens.screenPadding)
            )
            Text(
                state.columns[1],
                style = TransportTypeScale.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(70.dp)
            )
            Text(
                state.columns[2],
                style = TransportTypeScale.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                state.columns[3],
                style = TransportTypeScale.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(90.dp)
            )
            Text(
                state.columns[4],
                style = TransportTypeScale.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(90.dp)
            )
            Text(
                state.columns[5],
                style = TransportTypeScale.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(80.dp).padding(end = Dimens.screenPadding)
            )
        }

        // Table rows
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = Dimens.chipGap)
        ) {
            itemsIndexed(state.rows) { index, row ->
                ReportViewerRowItem(
                    row = row,
                    background = if (index % 2 == 0)
                        MaterialTheme.colorScheme.surface
                    else
                        MaterialTheme.colorScheme.surfaceContainerLow
                )
            }
        }

        // Summary totals row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = AppShapes.contentCardRadius, topEnd = AppShapes.contentCardRadius))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(vertical = 10.dp)
        ) {
            Text(
                state.totalLabel,
                style = TransportTypeScale.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.width(140.dp).padding(start = Dimens.screenPadding)
            )
            Spacer(Modifier.width(70.dp))
            Spacer(Modifier.weight(1f))
            Text(
                state.totalWeight,
                style = TransportTypeScale.dataSmall,
                fontFamily = PlexMonoFamily,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.width(90.dp)
            )
            Text(
                state.totalAmount,
                style = TransportTypeScale.dataSmall,
                fontFamily = PlexMonoFamily,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.width(90.dp)
            )
            Spacer(Modifier.width(80.dp).padding(end = Dimens.screenPadding))
        }

        // Export actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(Dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.fieldGap)
        ) {
            AppOutlinedButton(
                state.exportExcel,
                onClick = { onEvent(ReportViewerEvent.ExportExcel) },
                leadingIcon = Icons.Rounded.TableChart,
                modifier = Modifier.weight(1f),
                height = 48.dp
            )
            AppOutlinedButton(
                state.exportPdf,
                onClick = { onEvent(ReportViewerEvent.ExportPdf) },
                leadingIcon = Icons.Rounded.PictureAsPdf,
                modifier = Modifier.weight(1f),
                height = 48.dp
            )
        }

        // S27: the notice (PDF refusal, CSV answer) was computed and never rendered —
        // the PDF button looked completely dead.
        state.notice?.let { message ->
            Text(
                message,
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEvent(ReportViewerEvent.DismissNotice) }
                    .padding(horizontal = Dimens.screenPadding, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ReportViewerRowItem(row: RegisterRowUi, background: Color) {
    val statusColor = if (row.status == "CANCELLED")
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            row.bilty,
            style = TransportTypeScale.dataSmall,
            fontFamily = PlexMonoFamily,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(140.dp).padding(start = Dimens.screenPadding)
        )
        Text(
            row.date,
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(70.dp)
        )
        Text(
            row.consignor,
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            row.weight,
            style = TransportTypeScale.dataSmall,
            fontFamily = PlexMonoFamily,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(90.dp)
        )
        Text(
            row.amount,
            style = TransportTypeScale.dataSmall,
            fontFamily = PlexMonoFamily,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(90.dp)
        )
        Text(
            row.status,
            style = TransportTypeScale.labelMedium,
            color = statusColor,
            modifier = Modifier.width(80.dp).padding(end = Dimens.screenPadding)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportViewerPreview() {
    TransportAppTheme {
        ReportViewerContent(
            state = ReportViewerUiState(),
            onEvent = {},
            onBack = {}
        )
    }
}
