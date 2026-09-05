package com.example.transportapp.feature.reports.screen

import androidx.compose.runtime.Stable
data class ExportSheetUi(
    val name: String,
    val count: Long?,
)

data class RecentExportUi(
    val filename: String,
    val builtAt: Long,
    val sizeBytes: Long,
    val isPack: Boolean,
)

@Stable
data class ExportCentreUiState(
    val title: String = "Exports",
    val buildHeading: String = "BUILD A PACK",
    val fy: String = "FY 2026-27",
    val quarters: List<String> = listOf("FY 2026-27", "Q1 · Apr–Jun"),
    val selectedQuarter: String = "FY 2026-27",
    val includeHeading: String = "WHAT TO INCLUDE",
    val sheets: List<ExportSheetUi> = emptyList(),
    val includedIndices: Set<Int> = emptySet(),
    val uncheckAll: String = "Uncheck all",
    val formatHeading: String = "FORMAT",
    val formats: List<String> = listOf("Excel (.xlsx)", "CSV (zip)", "Tally XML"),
    val selectedFormat: String = "CSV (zip)",
    val formatNote: String = "CSV (zip) keeps one file per sheet — Excel and Tally XML ship with the online tier.",
    val buildLabel: String = "Build the pack",
    val buildTitle: String = "Building your pack",
    val buildNote: String = "Keep this screen open. Large packs take about a minute.",
    val buildCancel: String = "Cancel",
    val buildRowsNote: String = "",
    val totalRows: Long = 0,
    val recentHeading: String = "RECENT EXPORTS",
    val recentExports: List<RecentExportUi> = emptyList(),
    val building: Boolean = false,
    val progress: Int = 0,
    val builtFile: String? = null,
    val notice: String? = null,
)

sealed interface ExportCentreEvent {
    data class SelectQuarter(val value: String) : ExportCentreEvent
    data class SelectFormat(val value: String) : ExportCentreEvent
    data class ToggleSheet(val index: Int) : ExportCentreEvent
    data object UncheckAll : ExportCentreEvent
    data object StartBuild : ExportCentreEvent
    data object CancelBuild : ExportCentreEvent
    data object DismissNotice : ExportCentreEvent
}
