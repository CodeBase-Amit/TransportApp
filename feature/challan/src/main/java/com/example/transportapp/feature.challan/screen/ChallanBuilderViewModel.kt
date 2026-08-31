package com.example.transportapp.feature.challan.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.common.Money
import com.example.transportapp.core.common.formatIndianGrouping
import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.PaymentMode
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.trip.CreateTripDraft
import com.example.transportapp.data.transport.trip.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * T10 (Phase2.md S7): the loadable pool = Booked here + At hub here, never on a live trip;
 * the load meter fills toward the vehicle's capacity; Create stamps the challan (§9) and
 * marks every leg Loaded (§11.2).
 */
@HiltViewModel
class ChallanBuilderViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val tripRepository: TripRepository,
    private val numberingRepository: com.example.transportapp.data.transport.numbering.NumberingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChallanBuilderUiState())
    val uiState: StateFlow<ChallanBuilderUiState> = _uiState.asStateFlow()

    private var vehicleId: String? = null
    private var driverId: String? = null
    private var originBranchId: String = ""

    init {
        viewModelScope.launch {
            val session = sessionRepository.session.first()
            originBranchId = session.branchId
            val reserved = numberingRepository.peekNext(session.companyId, session.branchId, "CHALLAN")
            val vehicles = tripRepository.availableVehicles(session.companyId)
            val drivers = tripRepository.drivers(session.companyId)
            vehicleId = vehicles.firstOrNull()?.localId
            driverId = drivers.firstOrNull()?.localId
            tripRepository.observeLoadablePool(session.companyId, session.branchId).collect { pool ->
                _uiState.update { state ->
                    val rows = pool.map { c ->
                        LoadableRow(
                            localId = c.localId,
                            docNumber = c.displayNo,
                            consignee = c.consigneeName,
                            packages = "${c.packages} pkg",
                            weight = formatIndianGrouping(c.weightKg) + " kg",
                            amount = Money(c.totalPaise).formatted(),
                            paymentMode = c.paymentMode?.let { runCatching { PaymentMode.valueOf(it) }.getOrNull() }
                                ?: PaymentMode.TOPAY,
                            status = c.status,
                            isOnwardLeg = c.status == com.example.transportapp.domain.transport.ConsignmentStatus.AT_HUB,
                            onwardNote = if (c.status == com.example.transportapp.domain.transport.ConsignmentStatus.AT_HUB) "Onward leg · load again on this challan" else null,
                            weightKg = c.weightKg,
                            totalPaise = c.totalPaise,
                            toStationId = c.toStationId,
                            toStation = c.toStation,
                        )
                    }
                    val selected = state.selectedBilties.filter { no -> rows.any { it.docNumber == no } }.toSet()
                    val vehicle = vehicles.firstOrNull { it.localId == vehicleId }
                    val driver = drivers.firstOrNull { it.localId == driverId }
                    val selectedRows = rows.filter { it.docNumber in selected }
                    val dest = selectedRows.firstOrNull()?.toStation ?: ""
                    val via = selectedRows.map { it.toStation }.filter { it != dest }.distinct()
                    val weight = selectedRows.sumOf { it.weightKg }.toInt()
                    val capacity = vehicle?.capacityKg ?: 0
                    state.copy(
                        reservedNumber = reserved?.display ?: "",
                        readyToLoad = "READY TO LOAD · ${rows.size} AT ${session.branchName.uppercase()}",
                        loadable = rows,
                        selectedBilties = selected,
                        vehicleNumber = vehicle?.number ?: "",
                        vehicleOwnership = vehicle?.let { "${it.ownershipLabel} · ${formatIndianGrouping(it.capacityKg.toLong())} kg" } ?: "",
                        driverName = driver?.name ?: "",
                        driverLicenceLine = driver?.licence?.let { "Licence $it" } ?: "",
                        routeTo = dest,
                        routeVia = via.joinToString(" · "),
                        capacityKg = capacity,
                        selectedWeightKg = weight,
                        overByKg = (weight - capacity).coerceAtLeast(0),
                        overloaded = weight > capacity && capacity > 0,
                        freightTotal = Money(selectedRows.sumOf { it.totalPaise }).formatted(),
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onEvent(event: ChallanBuilderEvent) {
        when (event) {
            is ChallanBuilderEvent.ToggleSelectAll -> _uiState.update {
                val all = if (it.selectedBilties.size == it.loadable.size) emptySet() else it.loadable.map { c -> c.docNumber }.toSet()
                it.copy(selectedBilties = all).recomputed()
            }
            is ChallanBuilderEvent.ToggleConsignment -> _uiState.update {
                val today = it.selectedBilties
                it.copy(selectedBilties = if (event.docNumber in today) today - event.docNumber else today + event.docNumber).recomputed()
            }
            is ChallanBuilderEvent.SelectFilter -> _uiState.update { it.copy(selectedFilter = event.filter) }
            is ChallanBuilderEvent.Create -> create()
        }
    }

    /** The meter, the freight total and the route line always follow the selection. */
    private fun ChallanBuilderUiState.recomputed(): ChallanBuilderUiState {
        val selectedRows = loadable.filter { it.docNumber in selectedBilties }
        val dest = selectedRows.firstOrNull()?.toStation ?: ""
        val via = selectedRows.map { it.toStation }.filter { it != dest }.distinct()
        val weight = selectedRows.sumOf { it.weightKg }.toInt()
        val capacity = capacityKg
        return copy(
            selectedWeightKg = weight,
            overByKg = (weight - capacity).coerceAtLeast(0),
            overloaded = weight > capacity && capacity > 0,
            freightTotal = Money(selectedRows.sumOf { it.totalPaise }).formatted(),
            routeTo = dest,
            routeVia = via.joinToString(" · "),
        )
    }

    /** Creates the trip and immediately issues it — the challan number is stamped (§9), the legs Loaded (§11.2). */
    private fun create() {
        val state = _uiState.value
        if (state.isLoading || state.createdChallanNo != null) return
        val selected = state.loadable.filter { it.docNumber in state.selectedBilties }
        if (selected.isEmpty()) {
            _uiState.update { it.copy(error = "Pick at least one consignment to load") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val session = sessionRepository.session.first()
            val destId = selected.firstOrNull()?.toStationId
            if (destId == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            val draft = CreateTripDraft(
                vehicleId = vehicleId ?: return@launch,
                driverId = driverId ?: return@launch,
                originBranchId = session.branchId,
                destStationId = destId,
                viaStationIds = selected.map { it.toStationId }.filter { it != destId }.distinct(),
                consignmentIds = selected.map { it.localId },
                hirePaise = 1_850_000,
                advancePaise = 1_200_000,
                // The demo session is the Owner: the §11.2 overload approval is granted inline.
                managerOverride = state.overloaded,
            )
            val created = tripRepository.createTrip(draft, System.currentTimeMillis())
            val trip = created.getOrNull()
            if (trip == null) {
                val failure = created as? com.example.transportapp.core.common.Result.Failure
                _uiState.update { it.copy(isLoading = false, error = failure?.message ?: failure?.code?.name) }
                return@launch
            }
            val issued = tripRepository.issue(trip.tripLocalId, System.currentTimeMillis())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = (issued as? com.example.transportapp.core.common.Result.Failure)?.message,
                    createdChallanNo = issued.getOrNull()?.challanNo,
                )
            }
        }
    }

    fun consumeCreatedChallanNo() {
        _uiState.update { it.copy(createdChallanNo = null) }
    }
}
