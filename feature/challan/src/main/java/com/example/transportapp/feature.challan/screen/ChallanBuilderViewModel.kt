package com.example.transportapp.feature.challan.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChallanBuilderViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChallanBuilderUiState())
    val uiState: StateFlow<ChallanBuilderUiState> = _uiState.asStateFlow()

    fun onEvent(event: ChallanBuilderEvent) {
        when (event) {
            is ChallanBuilderEvent.ToggleSelectAll -> _uiState.update {
                val all = if (it.selectedBilties.size == it.loadable.size) emptySet() else it.loadable.map { c -> c.docNumber }.toSet()
                it.copy(selectedBilties = all)
            }
            is ChallanBuilderEvent.ToggleConsignment -> _uiState.update {
                val today = it.selectedBilties
                it.copy(selectedBilties = if (event.bilty in today) today - event.bilty else today + event.bilty)
            }
            is ChallanBuilderEvent.SelectFilter -> _uiState.update { it.copy(selectedFilter = event.filter) }
        }
    }
}
