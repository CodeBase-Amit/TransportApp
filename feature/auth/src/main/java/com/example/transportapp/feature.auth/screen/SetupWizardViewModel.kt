package com.example.transportapp.feature.auth.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SetupWizardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SetupWizardUiState())
    val uiState: StateFlow<SetupWizardUiState> = _uiState.asStateFlow()

    fun onEvent(event: SetupWizardEvent) {
        when (event) {
            SetupWizardEvent.Next -> _uiState.update { it.copy(step = (it.step + 1).coerceAtMost(it.stepLabels.lastIndex)) }
            is SetupWizardEvent.SelectOwnership -> _uiState.update { it.copy(ownership = event.value) }
            is SetupWizardEvent.SelectGstOption -> _uiState.update { it.copy(gstOption = event.option) }
            SetupWizardEvent.Finish -> Unit
        }
    }
}
