package com.example.transportapp.core.ui.sample

object UnbilledPoolSampleData {

    data class UnbilledParty(
        val name: String,
        val total: String,
        val consignments: Int,
        val period: String,
        val branches: String,
        val ageBuckets: Triple<Int, Int, Int>, // 0-30, 31-60, 60+
        val oldestDays: Int,
        val selected: Boolean,
        val expanded: Boolean = false,
        val allOver60: Boolean = false,
        val rows: List<UnbilledRow> = emptyList()
    )

    data class UnbilledRow(val bilty: String, val route: String, val date: String, val amount: String)

    val title = "Unbilled"
    val filterChips = listOf("This quarter", "All branches", "Over 30 days", "Over 60 days")

    val summaryParties = "18"
    val summaryConsignments = "214"
    val summaryFreight = "3,86,540.00"
    val oldestCaption = "oldest 41 days"

    val parties = listOf(
        UnbilledParty(
            "Deepak Steel Traders", "86,420.00", 23, "1 Jul to 31 Jul", "Indore and Nagpur",
            Triple(15, 5, 3), 41, selected = true, expanded = true,
            rows = listOf(
                UnbilledRow("IND/2627/04188", "Indore – Nashik", "25 Aug", "3,944.00"),
                UnbilledRow("IND/2627/04189", "Indore – Pune", "26 Aug", "12,500.00")
            )
        ),
        UnbilledParty(
            "Vidarbha Traders", "1,24,180.00", 42, "15 Jun to 10 Aug", "Nagpur",
            Triple(10, 12, 12), 38, selected = false,
            rows = emptyList()
        ),
        UnbilledParty(
            "Bhusawal Cement Agency", "41,900.00", 8, "1 May to 15 May", "Bhusawal",
            Triple(0, 0, 8), 72, selected = false, allOver60 = true,
            rows = emptyList()
        )
    )

    val selectedLabel = "SELECTED"
    val buildBill = "Build the bill"
}
