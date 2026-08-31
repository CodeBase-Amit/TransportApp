package com.example.transportapp.feature.masters.screen

data class RateCardResolutionStep(
    val number: String,
    val label: String,
    val note: String? = null,
)

data class RateCardRow(
    val localId: String,
    val route: String,
    val goods: String,
    val basis: String,
    val rate: String,
    val minQty: String,
)

data class RateChargeItem(
    val label: String,
    val value: String,
    val enabled: Boolean,
)

data class RateCardEditorUiState(
    val title: String = "Rate card",
    val subtitle: String = "",
    val resolutionHeading: String = "How a rate is chosen",
    val resolutionIntro: String = "The system applies rates in this exact order. It stops at the first match.",
    val resolutionSteps: List<RateCardResolutionStep> = listOf(
        RateCardResolutionStep("1", "Party + Route + Goods", "Most specific rate"),
        RateCardResolutionStep("2", "Party + Route", null),
        RateCardResolutionStep("3", "Company + Route + Goods", null),
        RateCardResolutionStep("4", "Company + Route", null),
        RateCardResolutionStep("5", "Company Default Rate", "Fallback if no specifics match"),
    ),
    val ratesHeading: String = "Rates · 12",
    val addRate: String = "Add Rate",
    val viewAll: String = "View all 12 rates",
    val tableHeaders: List<String> = listOf("Route", "Goods", "Basis", "Rate (₹)", "Min Qty"),
    val rateRows: List<RateCardRow> = emptyList(),
    val showAllRates: Boolean = false,
    val chargesHeading: String = "Charges that apply automatically",
    val charges: List<RateChargeItem> = emptyList(),
    val chargesNote: String = "Switched-on charges appear on the booking form already filled. The clerk can remove one, but never has to add it.",
    val saveLabel: String = "Save rate card",
    val isLoading: Boolean = true,
    val justSaved: Boolean = false,
)

sealed interface RateCardEditorEvent {
    data class ToggleCharge(val index: Int) : RateCardEditorEvent
    data object AddRate : RateCardEditorEvent
    data object ViewAllRates : RateCardEditorEvent
    data object SaveRateCard : RateCardEditorEvent
}
