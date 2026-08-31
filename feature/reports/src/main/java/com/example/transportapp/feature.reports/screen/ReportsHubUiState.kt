package com.example.transportapp.feature.reports.screen

data class ReportRowUi(
    val id: String,
    val label: String,
    val desc: String,
    val figure: String?,
)

data class ReportGroupUi(
    val heading: String,
    val reports: List<ReportRowUi>,
)

data class ReportsHubUiState(
    val title: String = "Reports",
    val period: String = "",
    val scope: String = "All branches",
    val periodNote: String = "Every report below uses this period and branch.",
    val groups: List<ReportGroupUi> = emptyList(),
    val loading: Boolean = true,
)

sealed interface ReportsHubEvent {
    data object ChangePeriod : ReportsHubEvent
}
