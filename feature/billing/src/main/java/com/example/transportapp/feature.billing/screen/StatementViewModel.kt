package com.example.transportapp.feature.billing.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class StatementViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StatementUiState())
    val uiState: StateFlow<StatementUiState> = _uiState.asStateFlow()

    fun onEvent(event: StatementEvent) {
        when (event) {
            is StatementEvent.Download -> _uiState.update { it }
            is StatementEvent.ChangePeriod -> _uiState.update { it }
            is StatementEvent.SendPdf -> _uiState.update { it }
        }
    }
}
