package com.example.transportapp.core.ui.sample

data class TemplateRow(
    val name: String,
    val type: String,
    val copies: String,
    val paper: String,
    val version: String,
    val status: String,
    val description: String,
    val isDefault: Boolean = false,
    val neverPrinted: Boolean = false,
    val archived: Boolean = false,
    val tags: List<String> = emptyList()
)

data class VersionHistory(val version: String, val date: String, val author: String, val change: String)

/**
 * T29 Templates demo data. UiState defaults all come from here so the screen
 * stays stateless and the Content composable never touches sample data.
 */
object TemplatesSampleData {

    const val TITLE = "Templates"
    const val SUBTITLE = "A template decides what a printed document looks like. Documents already issued keep the version they were printed with."
    const val REQUEST_TEMPLATE = "Request template"
    const val VERSION_HISTORY_HEADING = "VERSION HISTORY"

    val templates: List<TemplateRow> = listOf(
        TemplateRow(
            name = "Shivshakti Bilty 4-copy",
            type = "Bilty",
            copies = "4 copies",
            paper = "A4 portrait",
            version = "v3",
            status = "4,188 documents",
            description = "Standard transport receipt with consignor/consignee details.",
            isDefault = true,
            tags = listOf("GST")
        ),
        TemplateRow(
            name = "Express Cargo Waybill",
            type = "Bilty",
            copies = "4 copies",
            paper = "A4 landscape",
            version = "v1",
            status = "Never printed",
            description = "",
            neverPrinted = true
        ),
        TemplateRow(
            name = "Monthly Invoice",
            type = "Invoice",
            copies = "1 copy",
            paper = "A4 portrait",
            version = "v2",
            status = "—",
            description = "Summary of all bilties for a selected party."
        ),
        TemplateRow(
            name = "Custom Manifest",
            type = "Manifest",
            copies = "1 copy",
            paper = "A4 landscape",
            version = "v1",
            status = "Archived",
            description = "Archived layout. No longer in active use.",
            archived = true
        ),
        TemplateRow(
            name = "Consignment Manifest",
            type = "Manifest",
            copies = "1 copy",
            paper = "A4 portrait",
            version = "v1",
            status = "Never printed",
            description = "",
            neverPrinted = true
        )
    )

    val versionHistory: List<VersionHistory> = listOf(
        VersionHistory("v3", "02 Aug 2026", "Shivshakti support", "Added GST columns. Consignee block widened by 12mm."),
        VersionHistory("v2", "19 May 2026", "Shivshakti support", "Font size increase. Freight table rows reduced from 10 to 8 to fit."),
        VersionHistory("v1", "14 Apr 2026", "Shivshakti support", "Initial Layout. First version.")
    )
}
