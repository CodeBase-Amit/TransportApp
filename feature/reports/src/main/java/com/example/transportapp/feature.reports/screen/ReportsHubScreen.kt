package com.example.transportapp.feature.reports.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.History
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PlexMonoFamily
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

@Composable
fun ReportsHubScreen(onBack: () -> Unit, onReportClick: (String) -> Unit, viewModel: ReportsHubViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    ReportsHubContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onReportClick = onReportClick
    )
}

@Composable
fun ReportsHubContent(
    state: ReportsHubUiState,
    onEvent: (ReportsHubEvent) -> Unit,
    onBack: () -> Unit,
    onReportClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = state.title, onNavigationClick = onBack, trailingIcons = {
        })

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Dimens.screenPadding)
        ) {
            Text(
                state.period,
                style = TransportTypeScale.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "  ${state.scope}",
                style = TransportTypeScale.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onEvent(ReportsHubEvent.ChangePeriod) }) {
                Icon(
                    Icons.Rounded.DateRange,
                    contentDescription = "Change period",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            state.periodNote,
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                horizontal = Dimens.screenPadding,
                vertical = Dimens.chipGap
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.fieldGap)
        ) {
            state.groups.forEach { group ->
                GroupHeading(
                    group.heading,
                    modifier = Modifier.padding(top = Dimens.chipGap)
                )

                ContentCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = Dimens.cardPaddingNested
                ) {
                    group.reports.forEachIndexed { reportIndex, report ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onReportClick(report.id) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    report.label,
                                    style = TransportTypeScale.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    report.desc,
                                    style = TransportTypeScale.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            report.figure?.let {
                                Text(
                                    it,
                                    style = TransportTypeScale.dataSmall,
                                    fontFamily = PlexMonoFamily,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(Dimens.chipGap))
                            }
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (reportIndex < group.reports.lastIndex) {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportsHubPreview() {
    TransportAppTheme {
        ReportsHubContent(
            state = ReportsHubUiState(),
            onEvent = {},
            onBack = {},
            onReportClick = {}
        )
    }
}
