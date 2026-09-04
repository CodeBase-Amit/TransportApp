package com.example.transportapp.feature.challan.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.common.Money
import com.example.transportapp.core.common.formatIndianGrouping
import com.example.transportapp.data.transport.documents.DocumentRepository
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.trip.TripRepository
import com.example.transportapp.core.ui.PrintStatus
import com.example.transportapp.domain.transport.PaymentMode
import com.example.transportapp.domain.transport.TripState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChallanRow(val bilty: String, val consignee: String, val weight: String, val mode: PaymentMode)

data class ChallanStationGroup(val station: String, val count: Int, val rows: List<ChallanRow>)

data class BiltyLine(val bilty: String, val dest: String, val weight: String)

data class ChallanDetailUiState(
    val challanNo: String = "",
    val statusLabel: String = "Open",
    val routeFrom: String = "",
    val routeTo: String = "",
    val routeVia: String = "",
    val createdLine: String = "",
    val dispatchedLine: String = "",
    val consignments: Int = 0,
    val loadKg: String = "",
    val hire: String = "",
    val balance: String = "",
    // S19 — the §11 money position (freight earned vs hire + costs).
    val moneyHeading: String = "THE MONEY",
    val freightLine: String = "",
    val hireLine: String = "",
    val costsLine: String = "",
    val marginLabel: String = "Provisional margin",
    val margin: String = "",
    val costs: List<TripCostLine> = emptyList(),
    val addCostLabel: String = "Add a cost",
    val costOpen: Boolean = false,
    val isOwnerOrManager: Boolean = false,
    val whatsLoadedTitle: String = "WHAT'S LOADED",
    val showAll: String = "",
    val editLoad: String = "Edit load",
    val vehicleAndDriverHeading: String = "VEHICLE AND DRIVER",
    val vehicleNumber: String = "",
    val vehicleOwnership: String = "",
    val driverInitials: String = "",
    val driverLine: String = "",
    val challanGroups: List<ChallanStationGroup> = emptyList(),
    val paperCompany: String = "SHIVSHAKTI ROADLINES",
    val paperDocType: String = "LOADING CHALLAN",
    val paperChallanNo: String = "",
    val paperVehicle: String = "",
    val paperBiltyLines: List<BiltyLine> = emptyList(),
    val paperSeeFull: String = "See full challan",
    val dispatchedNotice: String = "",
    val isDispatched: Boolean = false,
    val isClosed: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/** One recorded trip cost row (S19 §11 block). */
data class TripCostLine(val head: String, val amount: String, val remark: String)

/** The add-cost dialog state (S19): head, rupee amount, remark (required per §11.4). */
data class CostDraft(
    val head: String = "Diesel",
    val amount: String = "",
    val remark: String = "",
) {
    val valid: Boolean get() = amount.toLongOrNull() != null && amount.toLongOrNull()!! > 0 && remark.isNotBlank()
}

sealed interface ChallanDetailEvent {
    data object Dispatch : ChallanDetailEvent
    data object CloseTrip : ChallanDetailEvent
    data object EditLoad : ChallanDetailEvent
    data object Print : ChallanDetailEvent
    data object Share : ChallanDetailEvent
    data object More : ChallanDetailEvent
    // S19 — trip costs (§11)
    data object StartAddCost : ChallanDetailEvent
    data object DismissAddCost : ChallanDetailEvent
    data class ChangeCostHead(val head: String) : ChallanDetailEvent
    data class ChangeCostAmount(val value: String) : ChallanDetailEvent
    data class ChangeCostRemark(val value: String) : ChallanDetailEvent
    data object SaveCost : ChallanDetailEvent
}

/**
 * T11 (Phase2.md S7): the challan as document and then as record — dispatch is one
 * confident action, close settles the trip and moves every consignment per §11.2.
 * S22 (D60): Print/Share render the challan through the fixed-format document path.
 */
@HiltViewModel
class ChallanDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val tripRepository: TripRepository,
    private val documentRepository: DocumentRepository,
) : ViewModel() {

    private val challanNo: String = checkNotNull(savedStateHandle["challanNo"])

    private val _uiState = MutableStateFlow(ChallanDetailUiState(challanNo = challanNo))
    val uiState: StateFlow<ChallanDetailUiState> = _uiState.asStateFlow()

    /** S22: the challan render/print status (same shape the case file uses). */
    private val _printStatus = MutableStateFlow<PrintStatus>(PrintStatus.Idle)
    val printStatus: StateFlow<PrintStatus> = _printStatus.asStateFlow()

    /** S19: the open add-cost dialog draft. */
    private val _costDraft = MutableStateFlow(CostDraft())
    val costDraft: StateFlow<CostDraft> = _costDraft.asStateFlow()

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            val session = sessionRepository.session.first()
            val isOwnerOrManager = session.role == "OWNER" || session.role == "MANAGER"
            val detail = tripRepository.tripDetail(challanNo)
            if (detail == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            _uiState.update { state ->
                state.copy(
                    challanNo = detail.challanNo ?: "",
                    statusLabel = when (detail.state) {
                        TripState.ISSUED, TripState.OPEN -> "Open"
                        TripState.DISPATCHED -> "Dispatched"
                        TripState.CLOSED -> "Closed"
                        TripState.CANCELLED -> "Cancelled"
                    },
                    routeFrom = detail.originBranchName,
                    routeTo = detail.destStation,
                    routeVia = detail.viaStations.takeIf { it.isNotEmpty() }?.let { "via ${it.joinToString(" · ")}" } ?: "",
                    createdLine = detail.createdLine,
                    dispatchedLine = detail.dispatchedAt?.let { dispatched ->
                        "dispatched ${formatDateTime(dispatched)} · expected ${detail.destStation} ${formatDate(dispatched + 2L * 24 * 60 * 60 * 1000)}, 6:00 PM"
                    } ?: "",
                    consignments = detail.consignments,
                    loadKg = "${formatIndianGrouping(detail.loadKg)} kg",
                    hire = Money(detail.hirePaise).formatted(),
                    balance = Money(detail.balancePaise).formatted(),
                    editLoad = if (detail.state == TripState.ISSUED) "Edit load" else "",
                    showAll = if (detail.consignments > 5) "Show all ${detail.consignments}" else "",
                    vehicleNumber = detail.vehicleNumber,
                    vehicleOwnership = when (detail.vehicleOwnership) {
                        "OWN" -> "Own · ${formatIndianGrouping(detail.vehicleCapacityKg.toLong())} kg"
                        "ATTACHED" -> "Attached · ${formatIndianGrouping(detail.vehicleCapacityKg.toLong())} kg"
                        else -> "Market · ${formatIndianGrouping(detail.vehicleCapacityKg.toLong())} kg"
                    },
                    driverInitials = detail.driverName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase(),
                    driverLine = listOfNotNull(detail.driverLicence?.let { "Licence $it" }, detail.driverPhone).joinToString(" · "),
                    challanGroups = detail.legs
                        .groupBy { it.toStation }
                        .map { (station, legs) ->
                            ChallanStationGroup(
                                station = station.uppercase(),
                                count = legs.size,
                                rows = legs.map { leg ->
                                    ChallanRow(
                                        bilty = leg.displayNo,
                                        consignee = leg.consigneeName,
                                        weight = formatIndianGrouping(leg.weightKg) + " kg",
                                        mode = leg.paymentMode?.let { runCatching { PaymentMode.valueOf(it) }.getOrNull() } ?: PaymentMode.TOPAY,
                                    )
                                },
                            )
                        },
                    paperChallanNo = detail.challanNo ?: "",
                    paperVehicle = detail.vehicleNumber,
                    paperBiltyLines = detail.legs.take(2).map { BiltyLine(it.displayNo, it.toStation, formatIndianGrouping(it.weightKg) + " kg") },
                    // S19 — the §11 money position card.
                    freightLine = Money(detail.freightPaise).formatted(),
                    hireLine = Money(detail.hirePaise).formatted(),
                    costsLine = Money(detail.costsPaise).formatted(),
                    margin = Money(detail.marginPaise).formatted(),
                    costs = detail.costs.map { TripCostLine(it.head, it.amount, it.remark) },
                    isOwnerOrManager = isOwnerOrManager,
                    dispatchedNotice = if (detail.state == TripState.DISPATCHED) {
                        "Balance ${Money(detail.balancePaise).formatted()} payable to the driver when the trip closes."
                    } else {
                        ""
                    },
                    isDispatched = detail.state != TripState.ISSUED && detail.state != TripState.OPEN,
                    isClosed = detail.state == TripState.CLOSED,
                    isLoading = false,
                    error = null,
                )
            }
        }
    }

    fun onEvent(event: ChallanDetailEvent) {
        when (event) {
            is ChallanDetailEvent.Dispatch -> act { tripRepository.dispatch(it, System.currentTimeMillis()) }
            is ChallanDetailEvent.CloseTrip -> act { tripRepository.close(it, System.currentTimeMillis()) }
            ChallanDetailEvent.EditLoad, ChallanDetailEvent.More -> Unit
            // S22 (D60): the challan renders through the fixed-format template (§11).
            ChallanDetailEvent.Print -> actRender(print = true)
            ChallanDetailEvent.Share -> actRender(print = false)
            // S19 — trip costs (§11): every cost needs a remark; the save reloads the money card.
            ChallanDetailEvent.StartAddCost -> _costDraft.update { CostDraft() }.also { _uiState.update { s -> s.copy(costOpen = true) } }
            ChallanDetailEvent.DismissAddCost -> _uiState.update { it.copy(costOpen = false) }
            is ChallanDetailEvent.ChangeCostHead -> _costDraft.update { it.copy(head = event.head) }
            is ChallanDetailEvent.ChangeCostAmount -> _costDraft.update { it.copy(amount = event.value.filter { ch -> ch.isDigit() }) }
            is ChallanDetailEvent.ChangeCostRemark -> _costDraft.update { it.copy(remark = event.value) }
            ChallanDetailEvent.SaveCost -> viewModelScope.launch {
                val draft = _costDraft.value
                val trip = tripRepository.tripDetail(challanNo) ?: return@launch
                _uiState.update { it.copy(isLoading = true, error = null) }
                val result = tripRepository.addCost(
                    tripId = trip.tripLocalId,
                    head = draft.head.uppercase(),
                    amountPaise = (draft.amount.toLongOrNull() ?: 0) * 100,
                    paymentMode = "CASH",
                    remark = draft.remark,
                    now = System.currentTimeMillis(),
                )
                when (result) {
                    is com.example.transportapp.core.common.Result.Success -> {
                        _costDraft.value = CostDraft()
                        _uiState.update { it.copy(isLoading = false, costOpen = false) }
                        reload()
                    }
                    is com.example.transportapp.core.common.Result.Failure ->
                        _uiState.update { it.copy(isLoading = false, error = result.message ?: result.code.name) }
                }
            }
        }
    }

    /** S22: render the challan through the byte path, then print or share it. */
    private fun actRender(print: Boolean) {
        if (_printStatus.value is PrintStatus.Rendering) return
        _printStatus.value = PrintStatus.Rendering("Preparing the challan…")
        viewModelScope.launch {
            when (val result = documentRepository.renderChallan(challanNo)) {
                is com.example.transportapp.core.common.Result.Success -> {
                    _printStatus.value = PrintStatus.Idle
                    if (print) documentRepository.print(result.value)
                    else documentRepository.share(result.value, "Challan $challanNo")
                }
                is com.example.transportapp.core.common.Result.Failure ->
                    _printStatus.value = PrintStatus.Error(result.message ?: "The challan could not be rendered")
            }
        }
    }

    private fun act(action: suspend (String) -> com.example.transportapp.core.common.Result<Unit>) {
        viewModelScope.launch {
            val session = sessionRepository.session.first()
            _uiState.update { it.copy(isLoading = true, error = null) }
            val trip = tripRepository.tripDetail(challanNo)
            val tripId = trip?.tripLocalId
            if (tripId == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            val result = action(tripId)
            val failure = result as? com.example.transportapp.core.common.Result.Failure
            _uiState.update { it.copy(isLoading = false, error = failure?.message) }
            if (result.isSuccess()) reload()
        }
    }

    private fun formatDateTime(epoch: Long): String =
        SimpleDateFormat("d MMM, h:mm a", Locale.ENGLISH).format(Date(epoch))

    private fun formatDate(epoch: Long): String =
        SimpleDateFormat("d MMM", Locale.ENGLISH).format(Date(epoch))
}
