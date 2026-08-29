package com.example.transportapp.feature.templates.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TemplatesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TemplatesUiState())
    val uiState: StateFlow<TemplatesUiState> = _uiState.asStateFlow()

    fun onEvent(event: TemplatesEvent) {
        when (event) {
            is TemplatesEvent.Filter -> _uiState.update { it.copy(selectedFilter = event.label) }
            TemplatesEvent.RequestTemplate -> _uiState.update { it }
            is TemplatesEvent.More -> _uiState.update { it }
        }
    }
}
