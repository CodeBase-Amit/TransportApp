package com.example.transportapp.feature.masters.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MasterListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MasterListUiState())
    val uiState: StateFlow<MasterListUiState> = _uiState.asStateFlow()

    fun onEvent(event: MasterListEvent) {
        when (event) {
            is MasterListEvent.SelectFilter -> _uiState.update { it.copy(selectedFilterIndex = event.index) }
            is MasterListEvent.SelectLetter -> _uiState.update { it.copy(selectedLetterIndex = event.index) }
            MasterListEvent.MergeDuplicates -> _uiState.update { it }
        }
    }
}
