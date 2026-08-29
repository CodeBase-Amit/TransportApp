package com.example.transportapp.feature.consignment.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CaseFileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CaseFileUiState())
    val uiState: StateFlow<CaseFileUiState> = _uiState.asStateFlow()
}
