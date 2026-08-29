package com.example.transportapp.feature.challan.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VehicleBoardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleBoardUiState())
    val uiState: StateFlow<VehicleBoardUiState> = _uiState.asStateFlow()

    fun onEvent(event: VehicleBoardEvent) {
        when (event) {
            is VehicleBoardEvent.SelectFilter -> _uiState.update { it.copy(selectedFilter = event.filter) }
        }
    }
}
