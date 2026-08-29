package com.example.transportapp.feature.reports.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ReportViewerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReportViewerUiState())
    val uiState: StateFlow<ReportViewerUiState> = _uiState.asStateFlow()

    fun onEvent(event: ReportViewerEvent) {
        when (event) {
            is ReportViewerEvent.RemoveFilter -> _uiState.update { it.copy(filters = it.filters - event.filter) }
            ReportViewerEvent.ClearAll -> _uiState.update { it.copy(filters = emptyList()) }
            ReportViewerEvent.OpenFilters -> _uiState.update { it }
            ReportViewerEvent.ExportExcel -> _uiState.update { it }
            ReportViewerEvent.ExportPdf -> _uiState.update { it }
        }
    }
}
