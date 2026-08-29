package com.example.transportapp.feature.masters.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MastersHubViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MastersHubUiState())
    val uiState: StateFlow<MastersHubUiState> = _uiState.asStateFlow()

    fun onEvent(event: MastersHubEvent) {
        when (event) {
            MastersHubEvent.ReviewDuplicates -> _uiState.update { it }
        }
    }
}
