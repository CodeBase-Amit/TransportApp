package com.example.transportapp.feature.auth.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.transport.company.CompanyRepository
import com.example.transportapp.data.transport.numbering.NumberingRepository
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
 * T3 — Company setup wizard (S18). Finish persists company + head-office branch + Owner
 * membership (one transaction, outbox rows with prerequisites) and provisions the branch's
 * BILTY numbering series, so the first booking works without the demo dataset.
 */
@HiltViewModel
class SetupWizardViewModel @Inject constructor(
    private val companyRepository: CompanyRepository,
    private val sessionRepository: SessionRepository,
    private val numberingRepository: NumberingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupWizardUiState())
    val uiState: StateFlow<SetupWizardUiState> = _uiState.asStateFlow()

    fun onEvent(event: SetupWizardEvent) {
        when (event) {
            SetupWizardEvent.Next -> _uiState.update { it.copy(step = (it.step + 1).coerceAtMost(it.stepLabels.lastIndex)) }
            is SetupWizardEvent.EditField -> _uiState.update { it.copy(field = event.field, value = event.value) }
            is SetupWizardEvent.SelectOwnership -> _uiState.update { it.copy(ownership = event.value) }
            is SetupWizardEvent.SelectGstOption -> _uiState.update { it.copy(gstOption = event.option) }
            SetupWizardEvent.Finish -> viewModelScope.launch { finishSetup() }
        }
    }

    private fun SetupWizardUiState.copy(field: SetupField, value: String): SetupWizardUiState = when (field) {
        SetupField.COMPANY_NAME -> copy(companyName = value)
        SetupField.HEAD_OFFICE -> copy(headOffice = value, branchAddress = value.takeIf { branchAddress.isBlank() || branchAddress == "Same as head office" } ?: branchAddress)
        SetupField.PHONE -> copy(phone = value)
        SetupField.EMAIL -> copy(email = value)
        SetupField.GSTIN -> copy(gstin = value.uppercase())
        SetupField.PAN -> copy(pan = value.uppercase())
        SetupField.BRANCH_NAME -> copy(branchName = value)
        SetupField.BRANCH_ADDRESS -> copy(branchAddress = value)
        SetupField.BRANCH_CODE -> copy(branchCode = value.uppercase())
        SetupField.VEHICLE_NUMBER -> copy(vehicleNumber = value)
        SetupField.CAPACITY -> copy(capacity = value)
        SetupField.DRIVER_NAME -> copy(driverName = value)
        SetupField.DRIVER_PHONE -> copy(driverPhone = value)
    }

    private suspend fun finishSetup() {
        val state = _uiState.value
        if (state.companyName.isBlank() || state.branchCode.isBlank()) {
            _uiState.update { it.copy(error = "Company name and branch code are required") }
            return
        }
        val session = sessionRepository.session.first()
        if (!session.isSignedIn) {
            _uiState.update { it.copy(error = "Sign in before setting up a company") }
            return
        }
        _uiState.update { it.copy(error = null) }
        when (val result = companyRepository.registerCompany(
            RegisterCompanyRequest(
                companyName = state.companyName.trim(),
                address = state.headOffice.trim().ifBlank { state.branchAddress.trim() },
                gstin = state.gstin.trim().takeIf { it.isNotBlank() },
                branchName = state.branchName.trim().ifBlank { "Head office" },
                branchCode = state.branchCode.trim().uppercase(),
                ownerUserName = session.name,
                ownerUserEmail = session.email,
            ),
        )) {
            is com.example.transportapp.core.common.Result.Success -> {
                // §9: provision the branch's bilty series so booking works from minute one.
                val ctx = sessionRepository.session.first()
                numberingRepository.ensureSeries(ctx.companyId, ctx.branchId, "BILTY", state.branchCode.trim().uppercase())
                _uiState.update { it.copy(step = it.stepLabels.lastIndex, justFinished = true) }
            }
            is com.example.transportapp.core.common.Result.Failure ->
                _uiState.update { it.copy(error = result.message ?: result.code.name) }
        }
    }
}
