package com.example.transportapp.core.ui.sample

data class SeriesRow(
    val label: String,
    val nextNumber: String,
    val prefix: String,
    val fy: String,
    val lastUsed: String,
    val caption: String,
    val neverUsed: Boolean = false
)

/**
 * T28 Numbering demo data. UiState defaults all come from here so the screen
 * stays stateless and the Content composable never touches sample data.
 */
object NumberingSampleData {

    const val TITLE = "Numbering series"
    const val SUBTITLE = "One series per document type per branch."
    const val EDIT = "Edit"

    val rows: List<SeriesRow> = listOf(
        SeriesRow("Bilty · Indore", "IND/2627/04189", "IND/", "2627", "04189", "4,188 issued this year"),
        SeriesRow("Bilty · Bhiwandi", "BHI/2627/00000", "BHI/", "2627", "00000", "Never used. Change the format now.", neverUsed = true),
        SeriesRow("Invoice · Indore", "INV-26-0091", "INV-", "26", "0091", "91 issued this year"),
        SeriesRow("Loading Slip · All Branches", "LS-8842", "LS-", "—", "8842", "8,842 issued total"),
        SeriesRow("Manifest · Indore", "IND/MAN/022", "IND/MAN/", "—", "022", "22 issued total"),
        SeriesRow("Money Receipt · Indore", "MR-2627-01004", "MR-2627-", "2627", "01004", "1,004 issued this year")
    )
}
