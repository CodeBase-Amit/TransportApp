package com.example.transportapp.feature.settings.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NumberingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NumberingUiState())
    val uiState: StateFlow<NumberingUiState> = _uiState.asStateFlow()

    fun onEvent(event: NumberingEvent) {
        when (event) {
            NumberingEvent.Edit -> _uiState.update { it }
            NumberingEvent.SeriesMore -> _uiState.update { it }
        }
    }
}
