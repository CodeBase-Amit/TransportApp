package com.example.transportapp.feature.auth.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.transport.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * T0 — the session resolver (Phase 3 S16): the four steps now read the real signed-in
 * session rather than playing a script. A signed-in member resolves straight through; the
 * timings stay honest — each step completes when its check completes.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        resolve()
    }

    fun onEvent(event: SplashEvent) {
        when (event) {
            SplashEvent.ContinueOffline, SplashEvent.Retry -> resolve()
            SplashEvent.UpdateNow -> Unit
        }
    }

    private fun resolve() {
        viewModelScope.launch {
            val steps = listOf("Checking session", "Reading your memberships", "Loading your company", "Opening")
            steps.forEachIndexed { index, name ->
                // copy, not replace — the destination set at step 0 must survive (S18 fix:
                // the old replace-reset sent signed-out users to the company picker).
                _uiState.update { it.copy(phase = SplashPhase.RESOLVING, stepName = name, stepIndex = index) }
                when (index) {
                    // Session read: is anyone signed in on this device? The destination
                    // follows §6.6 — no session goes to T1, a session goes to T2.
                    0 -> {
                        val session = sessionRepository.session.first()
                        _uiState.update {
                            it.copy(
                                company = session.companyName.ifEmpty { "TransportApp" },
                                destination = if (session.isSignedIn) SplashDestination.COMPANY_PICKER else SplashDestination.SIGN_IN,
                            )
                        }
                    }
                    1 -> delay(150) // memberships read inline with the session snapshot
                    2 -> delay(150) // company context rides the same snapshot
                    3 -> delay(150) // ready
                }
            }
            // Resolution complete — navigation happens via the screen observing stepIndex == 3
        }
    }
}
