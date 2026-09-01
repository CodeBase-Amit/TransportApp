package com.example.transportapp.feature.billing.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.ui.ErrorCopy
import com.example.transportapp.data.transport.billing.BillingRepository
import com.example.transportapp.data.transport.billing.PoolFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * T13 — the unbilled pool (§12.1). Billing is company-level, so the pool defaults to all
 * branches (D43). The party checkbox selects the whole group (its rows are fetched then, so
 * the sticky bar can total them); individual consignments refine the selection.
 * "Build the bill" creates one draft bill per selected party — a bill has exactly one
 * billed-to party (§12.1) — and opens the first.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UnbilledPoolViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnbilledPoolUiState())
    val uiState: StateFlow<UnbilledPoolUiState> = _uiState.asStateFlow()

    private val filter = MutableStateFlow(PoolFilter())

    init {
        filter.flatMapLatest { billingRepository.observeUnbilledPool(it) }
            .onEach { pool ->
                _uiState.update { state ->
                    state.copy(
                        parties = pool.map { group ->
                            val previous = state.parties.firstOrNull { it.group.partyId == group.partyId }
                            UnbilledPartyState(
                                group = group,
                                selected = previous?.selected ?: false,
                                expanded = previous?.expanded ?: false,
                                selectedIds = previous?.selectedIds.orEmpty(),
                                rows = previous?.rows.orEmpty(),
                            )
                        },
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: UnbilledPoolEvent) {
        when (event) {
            is UnbilledPoolEvent.ToggleParty -> toggleParty(event.partyId)
            is UnbilledPoolEvent.ToggleExpand -> toggleExpand(event.partyId)
            is UnbilledPoolEvent.ToggleConsignment -> _uiState.update { state ->
                state.copy(parties = state.parties.map {
                    if (it.group.partyId == event.partyId) {
                        val ids = it.selectedIds
                        it.copy(
                            selectedIds = if (event.consignmentId in ids) ids - event.consignmentId else ids + event.consignmentId,
                            selected = false,
                        )
                    } else it
                })
            }
            is UnbilledPoolEvent.ToggleQuarter -> filter.value = filter.value.copy(thisQuarterOnly = event.on)
            is UnbilledPoolEvent.ToggleAllBranches -> filter.value = filter.value.copy(allBranches = event.on)
            is UnbilledPoolEvent.SetAgeFilter -> filter.value = filter.value.copy(minAgeDays = event.minAgeDays)
            UnbilledPoolEvent.BuildBill -> buildBill()
            UnbilledPoolEvent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun toggleParty(partyId: String) {
        val party = _uiState.value.parties.firstOrNull { it.group.partyId == partyId } ?: return
        val turningOn = !party.selected
        _uiState.update { state ->
            state.copy(parties = state.parties.map { if (it.group.partyId == partyId) it.copy(selected = turningOn) else it })
        }
        if (turningOn && party.rows.isEmpty()) {
            // Fetch the group's rows so the selection (and the sticky bar's total) is explicit.
            viewModelScope.launch {
                val rows = billingRepository.observeUnbilledRows(partyId, filter.value).first()
                _uiState.update { state ->
                    state.copy(parties = state.parties.map {
                        if (it.group.partyId == partyId) it.copy(rows = rows, selectedIds = rows.map { r -> r.localId }.toSet()) else it
                    })
                }
            }
        } else if (turningOn) {
            _uiState.update { state ->
                state.copy(parties = state.parties.map {
                    if (it.group.partyId == partyId) it.copy(selectedIds = it.rows.map { r -> r.localId }.toSet()) else it
                })
            }
        } else {
            _uiState.update { state ->
                state.copy(parties = state.parties.map {
                    if (it.group.partyId == partyId) it.copy(selectedIds = emptySet()) else it
                })
            }
        }
    }

    private fun toggleExpand(partyId: String) {
        val wasExpanded = _uiState.value.parties.firstOrNull { it.group.partyId == partyId }?.expanded ?: false
        _uiState.update { state ->
            state.copy(parties = state.parties.map { if (it.group.partyId == partyId) it.copy(expanded = !it.expanded) else it })
        }
        if (!wasExpanded) {
            billingRepository.observeUnbilledRows(partyId, filter.value)
                .onEach { rows ->
                    _uiState.update { state ->
                        state.copy(parties = state.parties.map { if (it.group.partyId == partyId) it.copy(rows = rows) else it })
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    /** One draft bill per selected party — a bill is billed to exactly one party (§12.1). */
    private fun buildBill() {
        val targets = _uiState.value.parties.filter { it.selected && it.selectedIds.isNotEmpty() }
        if (targets.isEmpty()) return
        _uiState.update { it.copy(building = true) }
        viewModelScope.launch {
            var firstBillId: String? = null
            var failure: String? = null
            for (party in targets) {
                val result = billingRepository.buildDraftBill(
                    partyId = party.group.partyId,
                    consignmentIds = party.selectedIds.toList(),
                    dueAt = null,
                    notes = null,
                    now = System.currentTimeMillis(),
                )
                    firstBillId = firstBillId ?: result.getOrNull()?.localId
                val maybeFailure = (result as? com.example.transportapp.core.common.Result.Failure)?.let { ErrorCopy.resolve(it) }
                if (failure == null) failure = maybeFailure
            }
            _uiState.update { it.copy(building = false, error = failure) }
            firstBillId?.let { onBillBuilt(it) }
        }
    }

    /** Set by the screen: navigates to T14 for the first created draft. */
    var onBillBuilt: (String) -> Unit = {}
}
