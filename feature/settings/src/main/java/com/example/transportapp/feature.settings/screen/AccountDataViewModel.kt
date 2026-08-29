package com.example.transportapp.feature.settings.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AccountDataViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AccountDataUiState())
    val uiState: StateFlow<AccountDataUiState> = _uiState.asStateFlow()

    fun onEvent(event: AccountDataEvent) {
        when (event) {
            AccountDataEvent.TrySync -> _uiState.update { it }
            AccountDataEvent.ClearCached -> _uiState.update { it }
            AccountDataEvent.SignOut -> _uiState.update { it }
            AccountDataEvent.Leave -> _uiState.update { it }
            AccountDataEvent.RequestDelete -> _uiState.update { it.copy(showDeleteDialog = true) }
            AccountDataEvent.CancelDelete -> _uiState.update { it.copy(showDeleteDialog = false) }
            AccountDataEvent.ConfirmDelete -> _uiState.update { it.copy(showDeleteDialog = false) }
        }
    }
}
