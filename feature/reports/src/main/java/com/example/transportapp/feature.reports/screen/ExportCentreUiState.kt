package com.example.transportapp.feature.reports.screen

import com.example.transportapp.core.ui.sample.ExportSheet
import com.example.transportapp.core.ui.sample.ExportCentreSampleData
import com.example.transportapp.core.ui.sample.RecentExport

data class ExportCentreUiState(
    val title: String = ExportCentreSampleData.TITLE,
    val buildHeading: String = ExportCentreSampleData.BUILD_HEADING,
    val fy: String = ExportCentreSampleData.FY,
    val quarters: List<String> = ExportCentreSampleData.quarters,
    val selectedQuarter: String = ExportCentreSampleData.DEFAULT_QUARTER,
    val includeHeading: String = ExportCentreSampleData.INCLUDE_HEADING,
    val sheets: List<ExportSheet> = ExportCentreSampleData.sheets,
    val includedIndices: Set<Int> = ExportCentreSampleData.sheets.indices.toSet(),
    val uncheckAll: String = ExportCentreSampleData.UNCHECK_ALL,
    val formatHeading: String = ExportCentreSampleData.FORMAT_HEADING,
    val formats: List<String> = ExportCentreSampleData.formats,
    val selectedFormat: String = ExportCentreSampleData.DEFAULT_FORMAT,
    val formatNote: String = ExportCentreSampleData.FORMAT_NOTE,
    val buildLabel: String = ExportCentreSampleData.BUILD_LABEL,
    val buildTitle: String = ExportCentreSampleData.BUILD_TITLE,
    val buildNote: String = ExportCentreSampleData.BUILD_NOTE,
    val buildCancel: String = ExportCentreSampleData.BUILD_CANCEL,
    val buildRowsNote: String = ExportCentreSampleData.BUILD_ROWS_NOTE,
    val totalRows: Int = ExportCentreSampleData.totalRows,
    val recentHeading: String = ExportCentreSampleData.RECENT_HEADING,
    val recentExports: List<RecentExport> = ExportCentreSampleData.recentExports,
    val building: Boolean = false,
    val progress: Int = 0
)

sealed interface ExportCentreEvent {
    data class SelectQuarter(val value: String) : ExportCentreEvent
    data class SelectFormat(val value: String) : ExportCentreEvent
    data class ToggleSheet(val index: Int) : ExportCentreEvent
    data object UncheckAll : ExportCentreEvent
    data object StartBuild : ExportCentreEvent
    data object CancelBuild : ExportCentreEvent
}
