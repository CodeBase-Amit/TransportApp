package com.example.transportapp.core.ui.sample

sealed interface RegisterListItem {
    data class Header(val label: String) : RegisterListItem
    data class Row(val row: RegisterRow) : RegisterListItem
}

/**
 * T7 Register demo data — the six docket rows, the filter chip set, and the
 * day section headers. Row one carries the Pending-sync chip.
 */
object RegisterSampleData {

    val filterOptions = listOf("In transit", "This month", "All branches", "To Pay", "Unbilled", "Held", "Delivered")
    const val DEFAULT_FILTER = "This month"

    val summaryFigures = listOf(
        "MATCHING" to "61",
        "PACKAGES" to "812",
        "FREIGHT" to "2,41,880.00"
    )

    val items: List<RegisterListItem> = listOf(
        RegisterListItem.Header("TODAY · 25 AUG"),
        RegisterListItem.Row(SampleData.registerRows[0].copy(syncPending = true)),
        RegisterListItem.Header("YESTERDAY · 24 AUG"),
        RegisterListItem.Row(SampleData.registerRows[1]),
        RegisterListItem.Row(SampleData.registerRows[2]),
        RegisterListItem.Row(SampleData.registerRows[3]),
        RegisterListItem.Row(SampleData.registerRows[4]),
        RegisterListItem.Row(SampleData.registerRows[5])
    )
}
