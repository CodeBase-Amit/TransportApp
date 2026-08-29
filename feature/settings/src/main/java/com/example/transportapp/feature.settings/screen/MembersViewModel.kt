package com.example.transportapp.feature.settings.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MembersViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MembersUiState())
    val uiState: StateFlow<MembersUiState> = _uiState.asStateFlow()

    fun onEvent(event: MembersEvent) {
        when (event) {
            MembersEvent.Invite -> _uiState.update { it }
            MembersEvent.ToggleRoleMatrix -> _uiState.update { it.copy(showRoleMatrix = !it.showRoleMatrix) }
            MembersEvent.Resend -> _uiState.update { it }
        }
    }
}
