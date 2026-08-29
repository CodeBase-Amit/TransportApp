package com.example.transportapp.feature.consignment.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RegisterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.ChangeFilter -> _uiState.update { it.copy(selectedFilter = event.filter) }
            is RegisterEvent.ChangeSearchQuery -> _uiState.update { it.copy(searchQuery = event.query) }
        }
    }
}
