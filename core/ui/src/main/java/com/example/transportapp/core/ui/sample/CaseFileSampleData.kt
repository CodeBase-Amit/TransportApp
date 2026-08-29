package com.example.transportapp.core.ui.sample

data class CaseFileStat(
    val label: String,
    val value: String
)

data class CaseFileMoneyLine(
    val label: String,
    val value: String,
    val strong: Boolean = false
)

/**
 * T8 Case file demo data — stat blocks, the WHERE-IT-IS event timeline, and the
 * money breakdown. Events reuse the shared SampleData.caseFileEvents list.
 */
object CaseFileSampleData {

    val stats = listOf(
        CaseFileStat("Packages", "12"),
        CaseFileStat("Chargeable", "780 kg"),
        CaseFileStat("Freight", "3,944.00"),
        CaseFileStat("Expected", "27 Aug")
    )

    val events = SampleData.caseFileEvents

    val moneyRows = listOf(
        CaseFileMoneyLine("Freight", "3,510.00"),
        CaseFileMoneyLine("Charges", "246.00"),
        CaseFileMoneyLine("GST 5%", "187.80"),
        CaseFileMoneyLine("Total to collect", "3,944.00", strong = true)
    )

    const val FROM_STATION = SampleData.BRANCH_INDORE
    const val TO_STATION = SampleData.CONSIGNEE_STATION
    const val DISTANCE = "585 km"
    const val BOOKED_TEXT = "booked 25 Aug, 11:42 AM by ${SampleData.USER_NAME}"
    const val TOPAY_CALLOUT = "To Pay — collect 3,944.00 at Nashik before handing over the goods."
}
