package com.example.transportapp.feature.billing.screen

import com.example.transportapp.core.ui.sample.UnbilledPoolSampleData

typealias UnbilledParty = UnbilledPoolSampleData.UnbilledParty

data class UnbilledPoolUiState(
    val title: String = UnbilledPoolSampleData.title,
    val filterChips: List<String> = UnbilledPoolSampleData.filterChips,
    val selectedFilter: String = UnbilledPoolSampleData.filterChips.first(),
    val summaryParties: String = UnbilledPoolSampleData.summaryParties,
    val summaryConsignments: String = UnbilledPoolSampleData.summaryConsignments,
    val summaryFreight: String = UnbilledPoolSampleData.summaryFreight,
    val oldestCaption: String = UnbilledPoolSampleData.oldestCaption,
    val parties: List<UnbilledPoolSampleData.UnbilledParty> = UnbilledPoolSampleData.parties,
    val selectedLabel: String = UnbilledPoolSampleData.selectedLabel,
    val buildBill: String = UnbilledPoolSampleData.buildBill
)

sealed interface UnbilledPoolEvent {
    data object SelectAll : UnbilledPoolEvent
    data class ToggleSelect(val name: String) : UnbilledPoolEvent
    data class ToggleExpand(val name: String) : UnbilledPoolEvent
    data class SelectFilter(val filter: String) : UnbilledPoolEvent
}
