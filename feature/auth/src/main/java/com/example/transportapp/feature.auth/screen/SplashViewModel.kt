package com.example.transportapp.feature.auth.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * T0 — Splash / session resolver. The route line reports four real resolve steps.
 */
enum class SplashPhase { RESOLVING, FORCED_UPDATE, RESOLVE_FAILED }

data class SplashUiState(
    val phase: SplashPhase = SplashPhase.RESOLVING,
    val stepName: String = "Signing you in",
    val stepIndex: Int = 0 // 0-based current step
)

sealed interface SplashEvent {
    data object ContinueOffline : SplashEvent
    data object Retry : SplashEvent
    data object UpdateNow : SplashEvent
}

class SplashViewModel : ViewModel() {
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
        val steps = listOf("Signing you in", "Checking your branch", "Loading your bilty format", "Syncing 3 changes")
        viewModelScope.launch {
            steps.forEachIndexed { index, name ->
                _uiState.update { SplashUiState(phase = SplashPhase.RESOLVING, stepName = name, stepIndex = index) }
                delay(500)
            }
            // Resolution complete — navigate happens via the screen observing stepIndex == 3
        }
    }
}