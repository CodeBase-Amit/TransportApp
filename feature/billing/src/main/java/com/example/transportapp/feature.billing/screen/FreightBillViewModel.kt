package com.example.transportapp.feature.billing.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.common.Result
import com.example.transportapp.data.transport.billing.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * T14 — the freight bill as one object that visibly changes state (§12.1): a draft with no
 * number, a preview, then the issued record. Issue is refused offline in Phase 2 — the
 * draft is saved and the number is never consumed locally.
 */
@HiltViewModel
class FreightBillViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val billingRepository: BillingRepository,
) : ViewModel() {

    private val billId: String = savedStateHandle.get<String>("billId").orEmpty()

    private val _uiState = MutableStateFlow(FreightBillUiState(billId = billId))
    val uiState: StateFlow<FreightBillUiState> = _uiState.asStateFlow()

    init {
        billingRepository.observeBill(billId)
            .onEach { detail ->
                _uiState.update {
                    it.copy(
                        bill = detail?.bill,
                        rows = detail?.rows.orEmpty(),
                        outstandingPaise = detail?.outstandingPaise ?: 0,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: FreightBillEvent) {
        when (event) {
            is FreightBillEvent.RemoveRow -> removeRow(event.consignmentId)
            FreightBillEvent.ShowPreview -> _uiState.update { it.copy(previewing = true, issueError = null) }
            FreightBillEvent.BackToEdit -> _uiState.update { it.copy(previewing = false) }
            FreightBillEvent.Issue -> issue()
            FreightBillEvent.DismissError -> _uiState.update { it.copy(issueError = null) }
        }
    }

    private fun removeRow(consignmentId: String) {
        if (_uiState.value.removing) return
        _uiState.update { it.copy(removing = true) }
        viewModelScope.launch {
            billingRepository.removeConsignmentFromDraft(billId, consignmentId, System.currentTimeMillis())
            _uiState.update { it.copy(removing = false) }
        }
    }

    private fun issue() {
        viewModelScope.launch {
            val result = billingRepository.issueBill(billId, System.currentTimeMillis())
            if (result is Result.Failure) {
                _uiState.update { it.copy(issueError = result.message) }
            } else {
                _uiState.update { it.copy(previewing = false) }
            }
        }
    }
}
