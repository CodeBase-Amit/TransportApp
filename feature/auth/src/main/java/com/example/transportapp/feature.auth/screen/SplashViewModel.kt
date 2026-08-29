package com.example.transportapp.feature.auth.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.ui.sample.SplashSampleData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
        viewModelScope.launch {
            SplashSampleData.RESOLUTION_STEPS.forEachIndexed { index, name ->
                _uiState.update { SplashUiState(phase = SplashPhase.RESOLVING, stepName = name, stepIndex = index) }
                delay(500)
            }
            // Resolution complete — navigation happens via the screen observing stepIndex == 3
        }
    }
}
