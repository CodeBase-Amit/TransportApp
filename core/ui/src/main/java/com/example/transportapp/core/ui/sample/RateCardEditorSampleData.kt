package com.example.transportapp.core.ui.sample

data class RateCardResolutionStep(val label: String, val sub: String? = null)
data class RateCardRow(val route: String, val goods: String, val basis: String, val rate: String, val min: String, val note: String? = null)
data class RateChargeItem(val label: String, val detail: String, val isOn: Boolean)

/**
 * T20 Rate card editor demo data. UiState defaults all come from here so the
 * screen stays stateless and the Content composable never touches sample data.
 */
object RateCardEditorSampleData {

    const val TITLE = "Rate card"
    const val SUBTITLE = "Deepak Steel Traders · Rate card 2026-27 · 12 rates"
    const val RESOLUTION_HEADING = "How a rate is chosen"
    const val RESOLUTION_INTRO = "The system applies rates in this exact order. It stops at the first match."

    val resolutionSteps = listOf(
        RateCardResolutionStep("Party + Route + Goods", "Most specific rate"),
        RateCardResolutionStep("Party + Route"),
        RateCardResolutionStep("Company + Route + Goods"),
        RateCardResolutionStep("Company + Route"),
        RateCardResolutionStep("Company Default Rate", "Fallback if no specifics match")
    )

    const val RATES_HEADING = "Rates · 12"
    const val ADD_RATE = "Add rate"
    const val VIEW_ALL = "View all 12 rates"

    val tableHeaders = listOf("Route", "Goods", "Basis", "Rate (₹)", "Min Qty")

    val rateRows = listOf(
        RateCardRow("Indore – Nashik", "MS pipes", "Per kg", "4.50", "500 kg"),
        RateCardRow("Indore – Nashik", "Any", "Per kg", "5.00", "500 kg", note = "wider — used when goods don't match"),
        RateCardRow("Indore – Pune", "Angles", "Per Ton", "3,200.00", "1 Ton"),
        RateCardRow("Indore – Pune", "Any", "Per Ton", "3,500.00", "1 Ton"),
        RateCardRow("Indore – Mumbai", "TMT Bars", "Per Ton", "4,500.00", "5 Ton"),
        RateCardRow("Indore – Mumbai", "Any", "Per Ton", "4,000.00", "3 Ton"),
        RateCardRow("Indore – Bhiwandi", "MS pipes", "Per kg", "4.20", "1,000 kg"),
        RateCardRow("Nagpur – Nashik", "Cement", "Per Ton", "1,240.00", "3 t"),
        RateCardRow("Indore – Bhusawal", "Any", "Per Ton", "1,600.00", "2 t"),
        RateCardRow("Indore – Dhule", "Any", "Per Ton", "1,200.00", "1 t"),
        RateCardRow("Indore – Kalyan", "MS pipes", "Per kg", "4.80", "500 kg"),
        RateCardRow("Indore – Bhiwandi", "Angles", "Per Ton", "3,600.00", "1 Ton")
    )

    const val CHARGES_HEADING = "Charges that apply automatically"
    val charges = listOf(
        RateChargeItem("Hamali", "₹8.00 / art", true),
        RateChargeItem("Door delivery", "₹150.00 / LR", true),
        RateChargeItem("Surcharge", "5% of freight", true)
    )
    const val CHARGES_NOTE = "Switched-on charges appear on the booking form already filled. The clerk can remove one, but never has to add it."

    const val SAVE_LABEL = "Save rate card"
}
