package com.example.transportapp.feature.billing.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.ui.ErrorCopy
import com.example.transportapp.data.transport.billing.AllocationInput
import com.example.transportapp.data.transport.billing.BillingRepository
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.tracking.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * T15 — money coming in (§12.2): To Pay collections at this branch and receipts against
 * freight bills. Held consignments are not collectable until a Manager waives (the waiver
 * is an audit row); allocation is explicit, never inferred.
 */
@HiltViewModel
class PaymentsViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val statusRepository: StatusRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentsUiState())
    val uiState: StateFlow<PaymentsUiState> = _uiState.asStateFlow()

    init {
        billingRepository.observeTopayAwaiting()
            .onEach { rows -> _uiState.update { it.copy(toPayRows = rows) } }
            .launchIn(viewModelScope)
        billingRepository.observeRecentReceipts()
            .onEach { rows -> _uiState.update { it.copy(receipts = rows) } }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            val (sum, count) = billingRepository.receiptsSinceSummary(cal.timeInMillis)
            _uiState.update { it.copy(receivedThisMonthPaise = sum, receiptsCount = count) }
        }
    }

    fun onEvent(event: PaymentsEvent) {
        when (event) {
            is PaymentsEvent.SelectTab -> _uiState.update { it.copy(tab = event.tab) }

            is PaymentsEvent.OpenCollect -> _uiState.update { state ->
                state.copy(
                    collectSheet = CollectSheetState(
                        line = event.line,
                        amountText = rupeesText(event.line.amountPaise),
                        isManager = false,
                    ),
                )
            }.also { loadRole() }
            PaymentsEvent.DismissCollect -> _uiState.update { it.copy(collectSheet = null) }
            is PaymentsEvent.SetCollectAmount -> _uiState.update { it.copy(collectSheet = it.collectSheet?.copy(amountText = event.text, error = null)) }
            is PaymentsEvent.SetCollectMode -> _uiState.update { it.copy(collectSheet = it.collectSheet?.copy(mode = event.mode)) }
            is PaymentsEvent.SetCollectReference -> _uiState.update { it.copy(collectSheet = it.collectSheet?.copy(reference = event.text, error = null)) }
            is PaymentsEvent.SetWaiverReason -> _uiState.update { it.copy(collectSheet = it.collectSheet?.copy(waiverReason = event.text, error = null)) }
            PaymentsEvent.RecordWaiver -> recordWaiver()
            PaymentsEvent.SaveCollect -> saveCollect()

            PaymentsEvent.OpenAllocation -> openAllocation()
            PaymentsEvent.DismissAllocation -> _uiState.update { it.copy(allocationSheet = null) }
            is PaymentsEvent.SelectParty -> selectParty(event.partyId)
            is PaymentsEvent.SetAmount -> _uiState.update { it.copy(allocationSheet = it.allocationSheet?.copy(amountText = event.text)) }
            is PaymentsEvent.SetMode -> _uiState.update { it.copy(allocationSheet = it.allocationSheet?.copy(mode = event.mode)) }
            is PaymentsEvent.SetReference -> _uiState.update { it.copy(allocationSheet = it.allocationSheet?.copy(reference = event.text)) }
            is PaymentsEvent.SetApplied -> _uiState.update { it.copy(allocationSheet = it.allocationSheet?.copy(applied = it.allocationSheet!!.applied + (event.billId to event.text))) }
            PaymentsEvent.ParkTheRest -> parkTheRest()
            PaymentsEvent.SaveAllocation -> saveAllocation()
        }
    }

    private fun loadRole() {
        viewModelScope.launch {
            val role = sessionRepository.session.first().role
            val isManager = role == "OWNER" || role == "MANAGER"
            _uiState.update { it.copy(collectSheet = it.collectSheet?.copy(isManager = isManager)) }
        }
    }

    private fun recordWaiver() {
        val sheet = _uiState.value.collectSheet ?: return
        viewModelScope.launch {
            val result = statusRepository.waiveTopPay(sheet.line.displayNo, sheet.waiverReason, System.currentTimeMillis())
            when (result) {
                is Result.Success -> _uiState.update {
                    // The waiver unblocks collection in place — no reopen needed.
                    it.copy(collectSheet = it.collectSheet?.copy(line = sheet.line.copy(waived = true), waiving = false))
                }
                is Result.Failure -> _uiState.update { it.copy(collectSheet = it.collectSheet?.copy(error = ErrorCopy.resolve(result))) }
            }
        }
    }

    private fun saveCollect() {
        val sheet = _uiState.value.collectSheet ?: return
        val amount = parseRupees(sheet.amountText)
        if (amount <= 0) {
            _uiState.update { it.copy(collectSheet = sheet.copy(error = "Enter the amount received")) }
            return
        }
        _uiState.update { it.copy(collectSheet = sheet.copy(saving = true, error = null)) }
        viewModelScope.launch {
            val result = billingRepository.recordReceipt(
                payerPartyId = sheet.line.consigneePartyId,
                amountPaise = amount,
                instrument = sheet.mode,
                instrumentRef = sheet.reference.ifBlank { null },
                allocations = listOf(
                    AllocationInput(targetType = "TOPAY_CONSIGNMENT", consignmentId = sheet.line.localId, amountPaise = amount),
                ),
                now = System.currentTimeMillis(),
            )
            when (result) {
                is Result.Success -> _uiState.update { it.copy(collectSheet = null) }
                is Result.Failure -> _uiState.update { it.copy(collectSheet = it.collectSheet?.copy(saving = false, error = ErrorCopy.resolve(result))) }
            }
        }
    }

    private fun openAllocation() {
        _uiState.update { it.copy(allocationSheet = AllocationSheetState(loading = true)) }
        viewModelScope.launch {
            val parties = billingRepository.partiesWithIssuedBills().map { it.partyId to it.partyName }
            _uiState.update { it.copy(allocationSheet = it.allocationSheet?.copy(parties = parties, loading = false)) }
        }
    }

    private fun selectParty(partyId: String) {
        val sheet = _uiState.value.allocationSheet ?: return
        val name = sheet.parties.firstOrNull { it.first == partyId }?.second ?: ""
        _uiState.update { it.copy(allocationSheet = sheet.copy(partyId = partyId, partyName = name, bills = emptyList(), applied = emptyMap(), loading = true)) }
        viewModelScope.launch {
            val bills = billingRepository.outstandingBillsForParty(partyId)
            _uiState.update { it.copy(allocationSheet = it.allocationSheet?.copy(bills = bills, loading = false)) }
        }
    }

    private fun parkTheRest() {
        val sheet = _uiState.value.allocationSheet ?: return
        val rest = sheet.unappliedPaise
        if (rest <= 0) return
        // Parking is recorded as an explicit on-account allocation at save time.
        _uiState.update { it.copy(allocationSheet = sheet.copy(applied = sheet.applied + (ON_ACCOUNT_KEY to rupeesText(rest)))) }
    }

    private fun saveAllocation() {
        val sheet = _uiState.value.allocationSheet ?: return
        val partyId = sheet.partyId ?: return
        val amount = sheet.amountPaise
        val allocations = buildList {
            sheet.applied.forEach { (id, text) ->
                val paise = parseRupees(text)
                if (paise > 0) {
                    if (id == ON_ACCOUNT_KEY) {
                        add(AllocationInput(targetType = "ON_ACCOUNT", amountPaise = paise))
                    } else {
                        add(AllocationInput(targetType = "BILL", billId = id, amountPaise = paise))
                    }
                }
            }
        }
        _uiState.update { it.copy(allocationSheet = sheet.copy(saving = true, error = null)) }
        viewModelScope.launch {
            val result = billingRepository.recordReceipt(
                payerPartyId = partyId,
                amountPaise = amount,
                instrument = sheet.mode,
                instrumentRef = sheet.reference.ifBlank { null },
                allocations = allocations,
                now = System.currentTimeMillis(),
            )
            when (result) {
                is Result.Success -> _uiState.update { it.copy(allocationSheet = null) }
                is Result.Failure -> _uiState.update { it.copy(allocationSheet = it.allocationSheet?.copy(saving = false, error = ErrorCopy.resolve(result))) }
            }
        }
    }

    private fun rupeesText(paise: Long): String {
        val whole = paise / 100
        val frac = paise % 100
        return if (frac == 0L) whole.toString() else "$whole.${frac.toString().padStart(2, '0')}"
    }

    companion object {
        /** The parking key lives in the same map as the bill ids but is not a bill. */
        const val ON_ACCOUNT_KEY = "on-account"
    }
}
