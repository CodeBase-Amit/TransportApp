package com.example.transportapp.feature.settings.screen

import androidx.lifecycle.SavedStateHandle
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

/**
 * T25 — company profile (S19): every field writes through to [savedStateHandle]; a
 * half-edited profile survives process death and re-opens with the draft, not the record.
 * The draft is cleared once the save commits.
 */
class CompanyProfileViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val hasDraft: Boolean = savedStateHandle.get<Boolean>("cp_draft") == true

    private val _uiState = MutableStateFlow(CompanyProfileUiState())
    val uiState: StateFlow<CompanyProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (hasDraft) {
                _uiState.update { it.fromDraft(savedStateHandle) }
                return@launch
            }
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
            is CompanyProfileEvent.ChangeLegalName -> _uiState.update { it.copy(legalName = event.value) }.also { persistDraft() }
            is CompanyProfileEvent.ChangeTradeName -> _uiState.update { it.copy(tradeName = event.value) }.also { persistDraft() }
            is CompanyProfileEvent.ChangeConstitution -> _uiState.update { it.copy(constitution = event.value) }.also { persistDraft() }
            is CompanyProfileEvent.ChangeAddress -> _uiState.update { it.copy(address = event.value) }.also { persistDraft() }
            is CompanyProfileEvent.ChangeCity -> _uiState.update { it.copy(city = event.value) }.also { persistDraft() }
            is CompanyProfileEvent.ChangePincode -> _uiState.update { it.copy(pincode = event.value) }.also { persistDraft() }
            is CompanyProfileEvent.ChangeState -> _uiState.update { it.copy(state = event.value) }.also { persistDraft() }
            is CompanyProfileEvent.ChangeGstin -> _uiState.update { it.copy(gstin = event.value) }.also { persistDraft() }
            is CompanyProfileEvent.ChangePan -> _uiState.update { it.copy(pan = event.value) }.also { persistDraft() }
            is CompanyProfileEvent.ChangeTransporterId -> _uiState.update { it.copy(transporterId = event.value) }.also { persistDraft() }
            is CompanyProfileEvent.ChangePhone -> _uiState.update { it.copy(phone = event.value) }.also { persistDraft() }
            is CompanyProfileEvent.ChangeAltPhone -> _uiState.update { it.copy(altPhone = event.value) }.also { persistDraft() }
            is CompanyProfileEvent.ChangeEmail -> _uiState.update { it.copy(email = event.value) }.also { persistDraft() }
            is CompanyProfileEvent.ChangeWebsite -> _uiState.update { it.copy(website = event.value) }.also { persistDraft() }
            is CompanyProfileEvent.ChangeFooter -> _uiState.update { it.copy(footerClause = event.value) }.also { persistDraft() }
            CompanyProfileEvent.Save -> save()
            is CompanyProfileEvent.RequestDelete -> _uiState.update { it }
        }
    }

    private fun persistDraft() {
        val s = _uiState.value
        savedStateHandle["cp_draft"] = true
        savedStateHandle["cp_legal"] = s.legalName
        savedStateHandle["cp_trade"] = s.tradeName
        savedStateHandle["cp_constitution"] = s.constitution
        savedStateHandle["cp_address"] = s.address
        savedStateHandle["cp_city"] = s.city
        savedStateHandle["cp_pincode"] = s.pincode
        savedStateHandle["cp_state"] = s.state
        savedStateHandle["cp_gstin"] = s.gstin
        savedStateHandle["cp_pan"] = s.pan
        savedStateHandle["cp_tid"] = s.transporterId
        savedStateHandle["cp_phone"] = s.phone
        savedStateHandle["cp_alt"] = s.altPhone
        savedStateHandle["cp_email"] = s.email
        savedStateHandle["cp_web"] = s.website
        savedStateHandle["cp_footer"] = s.footerClause
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
            savedStateHandle["cp_draft"] = false
            _uiState.update { it.copy(saved = true) }
        }
    }
}

/** S19: re-open the half-edited draft from the process-death snapshot. */
private fun CompanyProfileUiState.fromDraft(h: SavedStateHandle) = copy(
    legalName = h["cp_legal"] ?: legalName,
    tradeName = h["cp_trade"] ?: tradeName,
    constitution = h["cp_constitution"] ?: constitution,
    address = h["cp_address"] ?: address,
    city = h["cp_city"] ?: city,
    pincode = h["cp_pincode"] ?: pincode,
    state = h["cp_state"] ?: state,
    gstin = h["cp_gstin"] ?: gstin,
    pan = h["cp_pan"] ?: pan,
    transporterId = h["cp_tid"] ?: transporterId,
    phone = h["cp_phone"] ?: phone,
    altPhone = h["cp_alt"] ?: altPhone,
    email = h["cp_email"] ?: email,
    website = h["cp_web"] ?: website,
    footerClause = h["cp_footer"] ?: footerClause,
)
