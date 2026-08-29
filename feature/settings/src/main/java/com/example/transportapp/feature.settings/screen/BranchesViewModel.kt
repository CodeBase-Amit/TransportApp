package com.example.transportapp.feature.settings.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BranchesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BranchesUiState())
    val uiState: StateFlow<BranchesUiState> = _uiState.asStateFlow()

    fun onEvent(event: BranchesEvent) {
        when (event) {
            BranchesEvent.AddBranch -> _uiState.update { it }
            BranchesEvent.BranchMore -> _uiState.update { it }
        }
    }
}
