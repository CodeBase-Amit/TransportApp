package com.example.transportapp.feature.billing.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PaymentsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentsUiState())
    val uiState: StateFlow<PaymentsUiState> = _uiState.asStateFlow()

    fun onEvent(event: PaymentsEvent) {
        when (event) {
            is PaymentsEvent.SelectTab -> _uiState.update { it.copy(tab = event.tab) }
        }
    }
}
