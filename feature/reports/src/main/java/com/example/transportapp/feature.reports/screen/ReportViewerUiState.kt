package com.example.transportapp.feature.reports.screen

data class RegisterRowUi(
    val bilty: String,
    val date: String,
    val consignor: String,
    val weight: String,
    val amount: String,
    val status: String,
)

data class ReportViewerUiState(
    val title: String = "Freight register",
    val subtitle: String = "",
    val filters: List<String> = emptyList(),
    val clearAll: String = "Clear all",
    val filterLabel: String = "Filters",
    val columns: List<String> = listOf("Bilty no.", "Date", "Consignor", "Weight", "Amount", "Status"),
    val rows: List<RegisterRowUi> = emptyList(),
    val totalLabel: String = "",
    val totalWeight: String = "",
    val totalAmount: String = "",
    val exportExcel: String = "Export to CSV",
    val exportPdf: String = "Export to PDF",
    val notice: String? = null,
    val loading: Boolean = true,
)

sealed interface ReportViewerEvent {
    data class RemoveFilter(val filter: String) : ReportViewerEvent
    data object ClearAll : ReportViewerEvent
    // S27: OpenFilters removed with its dead icon — no filter sheet exists yet.
    data object ExportExcel : ReportViewerEvent
    data object ExportPdf : ReportViewerEvent
    data object DismissNotice : ReportViewerEvent
}
