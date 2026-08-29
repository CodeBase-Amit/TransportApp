package com.example.transportapp.core.ui.sample

data class ExportSheet(val name: String, val count: Int)

enum class ExportKind { READY, RECENT, EXPIRED }

data class RecentExport(val name: String, val statusText: String, val kind: ExportKind)

/**
 * T23 Export centre demo data. UiState defaults all come from here so the screen
 * stays stateless and the Content composable never touches sample data.
 */
object ExportCentreSampleData {

    const val TITLE = "Exports"

    const val BUILD_HEADING = "Build a pack"
    const val FY = "FY 2026-27"

    val quarters = listOf("Q1", "Q2", "Q3", "Q4")
    const val DEFAULT_QUARTER = "Q1"

    const val INCLUDE_HEADING = "WHAT TO INCLUDE"
    const val UNCHECK_ALL = "Uncheck all"
    const val FORMAT_HEADING = "FORMAT"
    val formats = listOf("Excel", "CSV", "Tally")
    const val DEFAULT_FORMAT = "Excel"
    const val FORMAT_NOTE = "Excel keeps one sheet per item and a cover sheet with your GSTIN and the period."
    const val BUILD_LABEL = "Build the pack"
    const val BUILD_TITLE = "Building your pack"
    const val BUILD_NOTE = "Keep this screen open. Large packs take about a minute."
    const val BUILD_CANCEL = "Cancel"
    const val BUILD_ROWS_NOTE = "4,412 rows written"

    val sheets = listOf(
        ExportSheet("Freight register", 1242),
        ExportSheet("Lorry hire register", 804),
        ExportSheet("Dockets", 4210),
        ExportSheet("Party ledger", 54),
        ExportSheet("Cash book", 320),
        ExportSheet("Bank book", 210),
        ExportSheet("Trial balance", 18),
        ExportSheet("GST report", 156),
        ExportSheet("TDS report", 42),
        ExportSheet("Profit & loss", 24),
        ExportSheet("Balance sheet", 14),
        ExportSheet("Outstanding summary", 112)
    )

    val totalRows: Int = sheets.sumOf { it.count }

    const val RECENT_HEADING = "Recent exports"
    val recentExports = listOf(
        RecentExport("Shivshakti-FY2627-Q1.xlsx", "Ready · 4.2 MB", ExportKind.READY),
        RecentExport("Shivshakti-May2023.csv", "3 days ago · 1.8 MB", ExportKind.RECENT),
        RecentExport("Q4-Summary-Export.csv", "Expired · Link invalid", ExportKind.EXPIRED)
    )
}
