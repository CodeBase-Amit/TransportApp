package com.example.transportapp.feature.reports.screen

import com.example.transportapp.core.ui.sample.ReportGroup
import com.example.transportapp.core.ui.sample.ReportsHubSampleData

data class ReportsHubUiState(
    val title: String = ReportsHubSampleData.TITLE,
    val period: String = ReportsHubSampleData.PERIOD,
    val scope: String = ReportsHubSampleData.SCOPE,
    val periodNote: String = ReportsHubSampleData.PERIOD_NOTE,
    val groups: List<ReportGroup> = ReportsHubSampleData.groups
)

sealed interface ReportsHubEvent {
    data object ChangePeriod : ReportsHubEvent
}
