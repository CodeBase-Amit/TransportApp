package com.example.transportapp.feature.reports.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ExportCentreViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ExportCentreUiState())
    val uiState: StateFlow<ExportCentreUiState> = _uiState.asStateFlow()

    fun onEvent(event: ExportCentreEvent) {
        when (event) {
            is ExportCentreEvent.SelectQuarter -> _uiState.update { it.copy(selectedQuarter = event.value) }
            is ExportCentreEvent.SelectFormat -> _uiState.update { it.copy(selectedFormat = event.value) }
            is ExportCentreEvent.ToggleSheet -> _uiState.update {
                val newSet = it.includedIndices.toMutableSet()
                if (!newSet.add(event.index)) newSet.remove(event.index)
                it.copy(includedIndices = newSet)
            }
            ExportCentreEvent.UncheckAll -> _uiState.update { it.copy(includedIndices = emptySet()) }
            ExportCentreEvent.StartBuild -> _uiState.update { it.copy(building = true, progress = 0) }
            ExportCentreEvent.CancelBuild -> _uiState.update { it.copy(building = false) }
        }
    }
}
