package com.example.transportapp.feature.reports.screen

import com.example.transportapp.core.ui.sample.ReportViewerRow
import com.example.transportapp.core.ui.sample.ReportViewerSampleData

data class ReportViewerUiState(
    val title: String = ReportViewerSampleData.TITLE,
    val subtitle: String = ReportViewerSampleData.SUBTITLE,
    val filters: List<String> = ReportViewerSampleData.filters,
    val clearAll: String = ReportViewerSampleData.CLEAR_ALL_LABEL,
    val filterLabel: String = ReportViewerSampleData.FILTER_LABEL,
    val columns: List<String> = ReportViewerSampleData.columns,
    val rows: List<ReportViewerRow> = ReportViewerSampleData.rows,
    val totalLabel: String = ReportViewerSampleData.TOTAL_LABEL,
    val totalWeight: String = ReportViewerSampleData.TOTAL_WEIGHT,
    val totalAmount: String = ReportViewerSampleData.TOTAL_AMOUNT,
    val exportExcel: String = ReportViewerSampleData.EXPORT_EXCEL,
    val exportPdf: String = ReportViewerSampleData.EXPORT_PDF
)

sealed interface ReportViewerEvent {
    data class RemoveFilter(val filter: String) : ReportViewerEvent
    data object ClearAll : ReportViewerEvent
    data object OpenFilters : ReportViewerEvent
    data object ExportExcel : ReportViewerEvent
    data object ExportPdf : ReportViewerEvent
}
