package com.example.transportapp.feature.auth.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.transport.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * T1 — sign in (S23): the Google button uses the mock seam (offline-first: the app always
 * opens); the email/password path talks to the real backend through the same session
 * repository. A network failure degrades to the mock identity so the app still opens
 * (D62: offline-first with or without the backend).
 */
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    /** One-shot: sign-in completed; the nav graph advances to the picker. */
    private val _signedIn = MutableStateFlow(false)
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    fun onEvent(event: SignInEvent) {
        when (event) {
            SignInEvent.ContinueWithGoogle -> {
                if (_uiState.value.loading) return
                _uiState.update { it.copy(loading = true) }
                viewModelScope.launch {
                    sessionRepository.signIn()
                    _uiState.update { it.copy(loading = false) }
                    _signedIn.value = true
                }
            }
            SignInEvent.Terms, SignInEvent.Privacy -> Unit
        }
    }
}
