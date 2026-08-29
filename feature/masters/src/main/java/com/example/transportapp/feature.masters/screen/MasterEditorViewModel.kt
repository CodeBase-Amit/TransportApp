package com.example.transportapp.feature.masters.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MasterEditorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MasterEditorUiState())
    val uiState: StateFlow<MasterEditorUiState> = _uiState.asStateFlow()

    fun onEvent(event: MasterEditorEvent) {
        when (event) {
            is MasterEditorEvent.ChangeName -> _uiState.update { it.copy(name = event.value) }
            is MasterEditorEvent.ChangeEmail -> _uiState.update { it.copy(email = event.value) }
            is MasterEditorEvent.ChangePhone -> _uiState.update { it.copy(phone = event.value) }
            is MasterEditorEvent.ChangeStreet -> _uiState.update { it.copy(street = event.value) }
            is MasterEditorEvent.ChangeStation -> _uiState.update { it.copy(station = event.value) }
            is MasterEditorEvent.ChangePincode -> _uiState.update { it.copy(pincode = event.value) }
            is MasterEditorEvent.ChangeGstin -> _uiState.update { it.copy(gstin = event.value) }
            is MasterEditorEvent.ChangeRoute -> _uiState.update { it.copy(usualRoute = event.value) }
            is MasterEditorEvent.ChangeRateCard -> _uiState.update { it.copy(rateCard = event.value) }
            is MasterEditorEvent.SelectType -> _uiState.update { it.copy(type = event.value) }
            is MasterEditorEvent.SelectPayment -> _uiState.update { it.copy(payment = event.value) }
            MasterEditorEvent.Save -> _uiState.update { it }
            MasterEditorEvent.Delete -> _uiState.update { it }
        }
    }
}
