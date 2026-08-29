package com.example.transportapp.feature.masters.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RateCardEditorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RateCardEditorUiState())
    val uiState: StateFlow<RateCardEditorUiState> = _uiState.asStateFlow()

    fun onEvent(event: RateCardEditorEvent) {
        when (event) {
            is RateCardEditorEvent.ToggleCharge -> _uiState.update {
                val new = it.charges.toMutableList()
                val item = new[event.index]
                new[event.index] = item.copy(isOn = !item.isOn)
                it.copy(charges = new)
            }
            RateCardEditorEvent.AddRate -> _uiState.update { it }
            RateCardEditorEvent.ViewAllRates -> _uiState.update { it }
            RateCardEditorEvent.SaveRateCard -> _uiState.update { it }
        }
    }
}
