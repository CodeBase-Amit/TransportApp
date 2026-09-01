package com.example.transportapp.feature.settings.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.transport.account.SettingsRepository
import com.example.transportapp.data.transport.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class CompanyProfileViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanyProfileUiState())
    val uiState: StateFlow<CompanyProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val s = sessionRepository.session.first()
            val profile = settingsRepository.companyProfile(s.companyId) ?: return@launch
            _uiState.update { state ->
                state.copy(
                    legalName = profile.legalName ?: profile.name,
                    tradeName = profile.name,
                    address = profile.address ?: "",
                    gstin = profile.gstin ?: "",
                    pan = profile.pan ?: "",
                    transporterId = profile.transporterId ?: "",
                )
            }
        }
    }

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
            CompanyProfileEvent.Save -> save()
            is CompanyProfileEvent.RequestDelete -> _uiState.update { it }
        }
    }

    /** §17.4.1: the profile is Owner data; the save updates COMPANY_E and queues the sync. */
    private fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            val s = sessionRepository.session.first()
            if (s.role != "OWNER" && s.role != "MANAGER") return@launch
            val profile = settingsRepository.companyProfile(s.companyId) ?: return@launch
            settingsRepository.saveCompanyProfile(
                companyId = s.companyId,
                name = state.tradeName.ifBlank { profile.name },
                legalName = state.legalName.ifBlank { profile.legalName ?: "" },
                address = state.address.ifBlank { profile.address ?: "" },
                gstin = state.gstin.ifBlank { profile.gstin ?: "" },
                pan = state.pan.ifBlank { profile.pan ?: "" },
                transporterId = state.transporterId ?: profile.transporterId,
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
