package com.example.transportapp.feature.challan.screen

import com.example.transportapp.core.ui.sample.VehicleBoardSampleData

typealias VehicleRow = VehicleBoardSampleData.VehicleRow

data class VehicleBoardUiState(
    val title: String = VehicleBoardSampleData.title,
    val filterChips: List<String> = VehicleBoardSampleData.filterChips,
    val selectedFilter: String = VehicleBoardSampleData.filterChips.first(),
    val summaryRunning: String = VehicleBoardSampleData.summaryRunning,
    val summaryIdle: String = VehicleBoardSampleData.summaryIdle,
    val summaryLate: String = VehicleBoardSampleData.summaryLate,
    val vehicles: List<VehicleBoardSampleData.VehicleRow> = VehicleBoardSampleData.vehicles,
    val loadIt: String = VehicleBoardSampleData.loadIt,
    val newChallan: String = VehicleBoardSampleData.newChallan
)

sealed interface VehicleBoardEvent {
    data class SelectFilter(val filter: String) : VehicleBoardEvent
}
