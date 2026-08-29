package com.example.transportapp.feature.settings.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CompanyProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CompanyProfileUiState())
    val uiState: StateFlow<CompanyProfileUiState> = _uiState.asStateFlow()

    fun onEvent(event: CompanyProfileEvent) {
        when (event) {
            is CompanyProfileEvent.ChangeLegalName -> _uiState.update { it.copy(legalName = event.value) }
            is CompanyProfileEvent.ChangeTradeName -> _uiState.update { it.copy(tradeName = event.value) }
            is CompanyProfileEvent.ChangeConstitution -> _uiState.update { it.copy(constitution = event.value) }
            is CompanyProfileEvent.ChangeAddress -> _uiState.update { it.copy(address = event.value) }
            is CompanyProfileEvent.ChangeCity -> _uiState.update { it.copy(city = event.value) }
            is CompanyProfileEvent.ChangePincode -> _uiState.update { it.copy(pincode = event.value) }
            is CompanyProfileEvent.ChangeState -> _uiState.update { it.copy(state = event.value) }
            is CompanyProfileEvent.ChangeGstin -> _uiState.update { it.copy(gstin = event.value) }
            is CompanyProfileEvent.ChangePan -> _uiState.update { it.copy(pan = event.value) }
            is CompanyProfileEvent.ChangeTransporterId -> _uiState.update { it.copy(transporterId = event.value) }
            is CompanyProfileEvent.ChangePhone -> _uiState.update { it.copy(phone = event.value) }
            is CompanyProfileEvent.ChangeAltPhone -> _uiState.update { it.copy(altPhone = event.value) }
            is CompanyProfileEvent.ChangeEmail -> _uiState.update { it.copy(email = event.value) }
            is CompanyProfileEvent.ChangeWebsite -> _uiState.update { it.copy(website = event.value) }
            is CompanyProfileEvent.ChangeFooter -> _uiState.update { it.copy(footerClause = event.value) }
            CompanyProfileEvent.Save -> _uiState.update { it }
            is CompanyProfileEvent.RequestDelete -> _uiState.update { it }
        }
    }
}
