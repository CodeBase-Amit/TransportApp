package com.example.transportapp.feature.masters.screen

import com.example.transportapp.core.ui.sample.RateCardResolutionStep
import com.example.transportapp.core.ui.sample.RateCardRow
import com.example.transportapp.core.ui.sample.RateChargeItem
import com.example.transportapp.core.ui.sample.RateCardEditorSampleData

data class RateCardEditorUiState(
    val title: String = RateCardEditorSampleData.TITLE,
    val subtitle: String = RateCardEditorSampleData.SUBTITLE,
    val resolutionHeading: String = RateCardEditorSampleData.RESOLUTION_HEADING,
    val resolutionIntro: String = RateCardEditorSampleData.RESOLUTION_INTRO,
    val resolutionSteps: List<RateCardResolutionStep> = RateCardEditorSampleData.resolutionSteps,
    val ratesHeading: String = RateCardEditorSampleData.RATES_HEADING,
    val addRate: String = RateCardEditorSampleData.ADD_RATE,
    val viewAll: String = RateCardEditorSampleData.VIEW_ALL,
    val tableHeaders: List<String> = RateCardEditorSampleData.tableHeaders,
    val rateRows: List<RateCardRow> = RateCardEditorSampleData.rateRows,
    val chargesHeading: String = RateCardEditorSampleData.CHARGES_HEADING,
    val charges: List<RateChargeItem> = RateCardEditorSampleData.charges,
    val chargesNote: String = RateCardEditorSampleData.CHARGES_NOTE,
    val saveLabel: String = RateCardEditorSampleData.SAVE_LABEL
)

sealed interface RateCardEditorEvent {
    data class ToggleCharge(val index: Int) : RateCardEditorEvent
    data object AddRate : RateCardEditorEvent
    data object ViewAllRates : RateCardEditorEvent
    data object SaveRateCard : RateCardEditorEvent
}
