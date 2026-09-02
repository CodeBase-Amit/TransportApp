package com.example.transportapp.feature.challan.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.common.formatIndianGrouping
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.trip.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One row of the §11.4 board — the screen's card shape, kept. */
data class VehicleRow(
    val number: String,
    val ownership: String,
    val isLate: Boolean,
    val lateLine: String? = null,
    val stops: List<String> = emptyList(),
    val currentStop: Int = 0,
    val driver: String,
    val load: String,
    val challan: String? = null,
    val idleDays: Int? = null,
    val lastTrip: String? = null,
)

data class VehicleBoardUiState(
    val title: String = "Vehicles",
    val filterChips: List<String> = listOf("Running", "Idle", "Own", "Attached", "Late"),
    val selectedFilter: String = "Running",
    val summaryRunning: String = "0",
    val summaryIdle: String = "0",
    val summaryLate: String = "0",
    val vehicles: List<VehicleRow> = emptyList(),
    val loadIt: String = "Load it",
    val newChallan: String = "New challan",
    val companyInitials: String = "SR",
    val companyName: String = "",
    val branchName: String = "",
)

sealed interface VehicleBoardEvent {
    data class SelectFilter(val filter: String) : VehicleBoardEvent
}

/**
 * T12 (Phase2.md S7): every vehicle with its current trip — On trip / Available, days out,
 * the late flag. Positions update when a clerk records an event, never live GPS (§11.4).
 */
@HiltViewModel
class VehicleBoardViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val tripRepository: TripRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleBoardUiState())
    val uiState: StateFlow<VehicleBoardUiState> = _uiState.asStateFlow()

    private val selectedFilter = MutableStateFlow("Running")

    init {
        viewModelScope.launch {
            val session = sessionRepository.session.first()
            combine(tripRepository.observeBoard(session.companyId), selectedFilter) { rows, filter ->
                val running = rows.filter { it.onTrip }
                val available = rows.filter { !it.onTrip }
                val late = running.filter { it.isLate }
                val shown = when (filter) {
                    "Running" -> running
                    "Idle" -> available
                    "Own" -> rows.filter { it.ownership == "OWN" }
                    "Attached" -> rows.filter { it.ownership != "OWN" }
                    "Late" -> late
                    else -> rows
                }
                val cards = shown.map { row ->
                    VehicleRow(
                        number = row.number,
                        ownership = when (row.ownership) {
                            "OWN" -> "Own"
                            "ATTACHED" -> "Attached"
                            else -> "Market"
                        },
                        isLate = row.isLate,
                        lateLine = if (row.isLate) "running past its expected arrival" else null,
                        stops = if (row.onTrip) listOf(session.branchName, row.destStation ?: "") else emptyList(),
                        currentStop = if (row.onTrip && row.tripState == com.example.transportapp.domain.transport.TripState.DISPATCHED) 1 else 0,
                        driver = row.driverName ?: "",
                        load = "${formatIndianGrouping(row.loadKg ?: 0)} kg",
                        challan = row.challanNo,
                        idleDays = row.idleDays?.toInt(),
                        lastTrip = row.idleDays?.let { "recently" },
                    )
                }
                Triple(
                    VehicleBoardUiState(
                        summaryRunning = running.size.toString(),
                        summaryIdle = available.size.toString(),
                        summaryLate = late.size.toString(),
                        vehicles = cards,
                        selectedFilter = filter,
                        companyInitials = session.companyName.split(" ").mapNotNull { w -> w.firstOrNull() }.take(2).joinToString("").ifEmpty { "SR" },
                        companyName = session.companyName,
                        branchName = session.branchName,
                    ),
                    running.size,
                    late.size,
                )
            }.collect { (state, _, _) ->
                _uiState.update { state }
            }
        }
    }

    fun onEvent(event: VehicleBoardEvent) {
        when (event) {
            is VehicleBoardEvent.SelectFilter -> selectedFilter.value = event.filter
        }
    }
}
