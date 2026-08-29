package com.example.transportapp.feature.reports.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ReportsHubViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsHubUiState())
    val uiState: StateFlow<ReportsHubUiState> = _uiState.asStateFlow()

    fun onEvent(event: ReportsHubEvent) {
        when (event) {
            ReportsHubEvent.ChangePeriod -> _uiState.update { it }
        }
    }
}
