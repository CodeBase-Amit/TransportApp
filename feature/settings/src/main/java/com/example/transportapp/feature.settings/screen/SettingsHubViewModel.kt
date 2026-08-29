package com.example.transportapp.feature.settings.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsHubViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsHubUiState())
    val uiState: StateFlow<SettingsHubUiState> = _uiState.asStateFlow()

    fun onEvent(event: SettingsHubEvent) {
        when (event) {
            SettingsHubEvent.SignOut -> _uiState.update { it }
            is SettingsHubEvent.RowClick -> _uiState.update { it }
        }
    }
}
