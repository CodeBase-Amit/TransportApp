package com.example.transportapp.feature.settings.screen

import com.example.transportapp.core.ui.sample.NumberingSampleData
import com.example.transportapp.core.ui.sample.SeriesRow

data class NumberingUiState(
    val title: String = NumberingSampleData.TITLE,
    val subtitle: String = NumberingSampleData.SUBTITLE,
    val editLabel: String = NumberingSampleData.EDIT,
    val series: List<SeriesRow> = NumberingSampleData.rows
)

sealed interface NumberingEvent {
    data object Edit : NumberingEvent
    data object SeriesMore : NumberingEvent
}
