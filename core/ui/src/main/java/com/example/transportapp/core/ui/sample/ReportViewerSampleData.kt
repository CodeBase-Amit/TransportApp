package com.example.transportapp.core.ui.sample

data class ReportViewerRow(
    val bilty: String,
    val date: String,
    val consignor: String,
    val weight: String,
    val amount: String,
    val status: String
)

/**
 * T22 Report viewer (Freight register) demo data. UiState defaults all come from
 * here so the screen stays stateless and the Content composable never touches
 * sample data.
 */
object ReportViewerSampleData {

    const val TITLE = "Freight register"
    const val SUBTITLE = "1 Apr – 25 Aug 2026 · Indore · 1,842 rows"

    const val TOTAL_LABEL = "TOTAL · 1,842"
    const val TOTAL_WEIGHT = "22,500 kg"
    const val TOTAL_AMOUNT = "₹2,25,000"

    const val FILTER_LABEL = "Filter"
    const val CLEAR_ALL_LABEL = "Clear all"
    const val EXPORT_EXCEL = "Export to Excel"
    const val EXPORT_PDF = "Export to PDF"

    val columns = listOf("Bilty no.", "Date", "Consignor", "Weight", "Amount", "Status")

    val filters = listOf("Indore branch", "To Pay only", "Over 1,000 kg")

    val rows = listOf(
        ReportViewerRow("IND/2627/04188", "01 Apr", "Tata Steel Ltd.", "1,250 kg", "₹12,500", "To Pay"),
        ReportViewerRow("04189", "02 Apr", "Reliance Ind.", "2,100 kg", "₹21,000", "Paid"),
        ReportViewerRow("04190", "05 Apr", "Bajaj Auto", "1,050 kg", "₹10,500", "To Pay"),
        ReportViewerRow("04191", "12 Apr", "Mahindra Logistics", "3,500 kg", "₹35,000", "Paid"),
        ReportViewerRow("04192", "18 Apr", "Adani Wilmar", "4,200 kg", "₹42,000", "To Pay"),
        ReportViewerRow("04193", "22 Apr", "ITC Limited", "1,800 kg", "₹18,000", "To Pay"),
        ReportViewerRow("04194", "01 May", "L&T Construction", "5,000 kg", "₹50,000", "Paid"),
        ReportViewerRow("04195", "10 May", "Godrej Consumer", "1,100 kg", "₹11,000", "To Pay"),
        ReportViewerRow("04196", "15 May", "Amul India", "2,500 kg", "₹25,000", "Paid")
    )
}
