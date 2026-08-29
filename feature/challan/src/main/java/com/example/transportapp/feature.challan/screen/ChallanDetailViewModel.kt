package com.example.transportapp.feature.challan.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChallanDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChallanDetailUiState())
    val uiState: StateFlow<ChallanDetailUiState> = _uiState.asStateFlow()

    fun onEvent(event: ChallanDetailEvent) {
        when (event) {
            is ChallanDetailEvent.Dispatch -> _uiState.update { it.copy(isDispatched = true) }
            is ChallanDetailEvent.EditLoad -> _uiState.update { it }
            is ChallanDetailEvent.Print -> _uiState.update { it }
            is ChallanDetailEvent.Share -> _uiState.update { it }
            is ChallanDetailEvent.More -> _uiState.update { it }
        }
    }
}
