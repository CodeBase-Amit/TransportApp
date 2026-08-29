package com.example.transportapp.feature.templates.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TemplateRequestsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateRequestsUiState())
    val uiState: StateFlow<TemplateRequestsUiState> = _uiState.asStateFlow()

    fun onEvent(event: TemplateRequestsEvent) {
        when (event) {
            TemplateRequestsEvent.NewRequest -> _uiState.update { it }
            TemplateRequestsEvent.ApprovePay -> _uiState.update { it }
            TemplateRequestsEvent.Preview -> _uiState.update { it }
            TemplateRequestsEvent.OpenCapture -> _uiState.update { it.copy(showCapture = true) }
            TemplateRequestsEvent.CloseCapture -> _uiState.update { it.copy(showCapture = false) }
            TemplateRequestsEvent.Retake -> _uiState.update { it }
            TemplateRequestsEvent.AddPhoto -> _uiState.update { it }
            TemplateRequestsEvent.SendForChecking -> _uiState.update { it }
        }
    }
}
