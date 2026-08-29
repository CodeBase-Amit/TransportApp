package com.example.transportapp.feature.consignment.screen

import com.example.transportapp.core.ui.sample.RegisterListItem
import com.example.transportapp.core.ui.sample.RegisterSampleData

data class RegisterUiState(
    val searchQuery: String = "",
    val selectedFilter: String = RegisterSampleData.DEFAULT_FILTER,
    val filterOptions: List<String> = RegisterSampleData.filterOptions,
    val items: List<RegisterListItem> = RegisterSampleData.items,
    val summaryFigures: List<Pair<String, String>> = RegisterSampleData.summaryFigures
)

sealed interface RegisterEvent {
    data class ChangeFilter(val filter: String) : RegisterEvent
    data class ChangeSearchQuery(val query: String) : RegisterEvent
}
