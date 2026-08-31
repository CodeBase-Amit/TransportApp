package com.example.transportapp.feature.auth.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.transport.company.CompanyRepository
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.domain.transport.org.RegisterCompanyRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * T3 — Company setup wizard (Phase2.md S2). Finish persists company + head-office branch +
 * Owner membership for the signed-in user (one transaction, outbox rows with prerequisites).
 */
@HiltViewModel
class SetupWizardViewModel @Inject constructor(
    private val companyRepository: CompanyRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupWizardUiState())
    val uiState: StateFlow<SetupWizardUiState> = _uiState.asStateFlow()

    fun onEvent(event: SetupWizardEvent) {
        when (event) {
            SetupWizardEvent.Next -> _uiState.update { it.copy(step = (it.step + 1).coerceAtMost(it.stepLabels.lastIndex)) }
            is SetupWizardEvent.SelectOwnership -> _uiState.update { it.copy(ownership = event.value) }
            is SetupWizardEvent.SelectGstOption -> _uiState.update { it.copy(gstOption = event.option) }
            SetupWizardEvent.Finish -> viewModelScope.launch { finishSetup() }
        }
    }

    private suspend fun finishSetup() {
        val state = _uiState.value
        val session = sessionRepository.session.first()
        _uiState.update { it.copy(error = null) }
        when (val result = companyRepository.registerCompany(
            RegisterCompanyRequest(
                companyName = state.companyName,
                address = state.headOffice,
                gstin = state.gstin.takeIf { it.isNotBlank() },
                branchName = state.branchName,
                branchCode = state.branchCode,
                ownerUserName = session.name,
                ownerUserEmail = session.email,
            ),
        )) {
            is com.example.transportapp.core.common.Result.Success ->
                _uiState.update { it.copy(step = it.stepLabels.lastIndex, justFinished = true) }
            is com.example.transportapp.core.common.Result.Failure ->
                _uiState.update { it.copy(error = result.message ?: result.code.name) }
        }
    }
}
