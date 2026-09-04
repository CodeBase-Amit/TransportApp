package com.example.transportapp.feature.reports.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppOutlinedButton
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.RouteLine
import com.example.transportapp.core.designsystem.component.RouteLineStep
import com.example.transportapp.core.designsystem.component.SegmentedControl
import com.example.transportapp.core.designsystem.component.StepState
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.AppShapes
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PlexMonoFamily
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors



@Composable
fun ExportCentreScreen(onBack: () -> Unit, viewModel: ExportCentreViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    ExportCentreContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@Composable
fun ExportCentreContent(
    state: ExportCentreUiState,
    onEvent: (ExportCentreEvent) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = state.title, onNavigationClick = onBack, trailingIcons = {
        })

        if (state.building) {
            ExportBuildSheet(state = state, onCancel = { onEvent(ExportCentreEvent.CancelBuild) })
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing)
            ) {
                GroupHeading(state.buildHeading)

                ContentCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.fy,
                            style = TransportTypeScale.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(Dimens.chipGap))
                        Text(
                            state.selectedQuarter,
                            style = TransportTypeScale.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { onEvent(ExportCentreEvent.SelectQuarter(state.selectedQuarter)) }) {
                            Icon(
                                Icons.Rounded.DateRange,
                                contentDescription = "Change period",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(Dimens.fieldGap))

                    Text(
                        state.includeHeading,
                        style = TransportTypeScale.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    state.sheets.forEachIndexed { index, sheet ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = if (index in state.includedIndices)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onEvent(ExportCentreEvent.ToggleSheet(index)) }
                            )
                            Spacer(Modifier.width(Dimens.fieldGap))
                            Text(
                                sheet.name,
                                style = TransportTypeScale.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                sheet.count?.toString() ?: "—",
                                style = TransportTypeScale.dataSmall,
                                fontFamily = PlexMonoFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(modifier = Modifier.clickable { onEvent(ExportCentreEvent.UncheckAll) }) {
                        Text(
                            state.uncheckAll,
                            style = TransportTypeScale.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(Dimens.chipGap))

                    Text(
                        "${state.sheets.size} sheets · about ${state.totalRows} rows",
                        style = TransportTypeScale.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(Dimens.fieldGap))

                    Text(
                        state.formatHeading,
                        style = TransportTypeScale.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SegmentedControl(
                        state.formats.map { it to it },
                        state.selectedFormat,
                        { onEvent(ExportCentreEvent.SelectFormat(it)) }
                    )

                    Text(
                        state.formatNote,
                        style = TransportTypeScale.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(Modifier.height(Dimens.sectionSpacing))

                    AppPrimaryButton(
                        state.buildLabel,
                        onClick = { onEvent(ExportCentreEvent.StartBuild) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                GroupHeading(state.recentHeading)

                ContentCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = Dimens.cardPaddingNested
                ) {
                    state.recentExports.forEach { export ->
                        RecentExportRow(item = export)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentExportRow(item: RecentExportUi) {
    val timeFormat = remember { java.text.SimpleDateFormat("d MMM, h:mm a", java.util.Locale.ENGLISH) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.chipGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.FileCopy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(Dimens.fieldGap))
        Column(Modifier.weight(1f)) {
            Text(
                item.filename,
                style = TransportTypeScale.dataSmall,
                fontFamily = PlexMonoFamily,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Built ${timeFormat.format(item.builtAt)} · ${item.sizeBytes / 1024} KB",
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExportBuildSheet(state: ExportCentreUiState, onCancel: () -> Unit) {
    val steps = List(state.sheets.size) { i ->
        RouteLineStep(
            "",
            when {
                i < state.progress -> StepState.DONE
                i == state.progress -> StepState.CURRENT
                else -> StepState.UPCOMING
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.sectionSpacing),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ContentCard(
            modifier = Modifier.fillMaxWidth(),
            elevated = true
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    state.buildTitle,
                    style = TransportTypeScale.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Dimens.sectionSpacing))
                RouteLine(steps, showTruck = true, showLabels = false, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(Dimens.screenPadding))
                val currentSheet = state.sheets.getOrNull(state.progress)?.name ?: "…"
                Text(
                    "Writing sheet ${state.progress + 1} of ${state.sheets.size} · $currentSheet",
                    style = TransportTypeScale.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    state.buildRowsNote,
                    style = TransportTypeScale.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Dimens.fieldGap))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.nestedCard)
                        .background(transportColors().haulAmberContainer)
                        .padding(Dimens.fieldGap)
                ) {
                    Text(
                        state.buildNote,
                        style = TransportTypeScale.bodySmall,
                        color = transportColors().onHaulAmber
                    )
                }
                Spacer(Modifier.height(Dimens.screenPadding))
                AppOutlinedButton(
                    state.buildCancel,
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExportCentrePreview() {
    TransportAppTheme {
        ExportCentreContent(
            state = ExportCentreUiState(building = false),
            onEvent = {},
            onBack = {}
        )
    }
}
