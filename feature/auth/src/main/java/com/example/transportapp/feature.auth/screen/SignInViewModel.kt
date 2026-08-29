package com.example.transportapp.feature.auth.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SignInViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun onEvent(event: SignInEvent) {
        when (event) {
            SignInEvent.ContinueWithGoogle -> _uiState.update { it.copy(loading = true) }
            SignInEvent.Terms, SignInEvent.Privacy -> Unit
        }
    }
}
