package com.example.transportapp.feature.billing.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FreightBillViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FreightBillUiState())
    val uiState: StateFlow<FreightBillUiState> = _uiState.asStateFlow()

    fun onEvent(event: FreightBillEvent) {
        when (event) {
            is FreightBillEvent.ChangeState -> _uiState.update { it.copy(state = event.state) }
        }
    }
}
