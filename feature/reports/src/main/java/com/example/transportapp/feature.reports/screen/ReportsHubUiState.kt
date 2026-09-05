package com.example.transportapp.feature.reports.screen

import androidx.compose.runtime.Stable
data class ReportRowUi(
    val id: String,
    val label: String,
    val desc: String,
    val figure: String?,
)

@Stable
data class ReportGroupUi(
    val heading: String,
    val reports: List<ReportRowUi>,
)

@Stable
data class ReportsHubUiState(
    val title: String = "Reports",
    val period: String = "",
    val scope: String = "All branches",
    val periodNote: String = "Every report below uses this period and branch.",
    val groups: List<ReportGroupUi> = emptyList(),
    val loading: Boolean = true,
)

// S27: ChangePeriod removed with its dead icon — the VM branch was a literal no-op.
