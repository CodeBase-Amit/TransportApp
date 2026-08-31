package com.example.transportapp.feature.dashboard.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.common.Money
import com.example.transportapp.data.transport.dashboard.DashboardRepository
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.tracking.StatusRepository
import com.example.transportapp.domain.transport.RoleRank
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

/**
 * T4 — Dashboard (Phase2.md S10): the ten §13 tile queries run in parallel behind
 * [DashboardRepository], role gating hides tiles the member cannot follow (§13: not greyed
 * out, and never a number with a dead tap), and the strip is dismissible per item.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val statusRepository: StatusRepository,
    private val dashboardRepository: DashboardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.session.collect { session ->
                _uiState.update {
                    it.copy(
                        companyName = session.companyName.ifEmpty { it.companyName },
                        branchName = session.branchName.ifEmpty { it.branchName },
                    )
                }
                refresh(session.companyId, session.branchId, session.role)
            }
        }
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.DismissException -> _uiState.update {
                it.copy(dismissedExceptions = it.dismissedExceptions + event.index)
            }
            DashboardEvent.Refresh -> viewModelScope.launch {
                val s = sessionRepository.session.first()
                refresh(s.companyId, s.branchId, s.role)
            }
        }
    }

    private suspend fun refresh(companyId: String, branchId: String, role: String) {
        val now = System.currentTimeMillis()
        val tiles = dashboardRepository.load(now)
        val exceptions = statusRepository.exceptions(companyId, branchId = null, sinceAt = now - 30L * 24 * 60 * 60 * 1000, now = now)

        val visible = { minRole: String -> RoleRank.atLeast(role, minRole) }
        fun m(paise: Long) = Money(paise).formatted()
        val tileList = buildList {
            if (visible("BOOKING_CLERK")) add(DashTile("Running services", tiles.runningServices.toString(), tiles.nearestRunningArrival?.let { "next arrival " + SimpleDateFormat("d MMM, h:mm a", Locale.ENGLISH).format(it) } ?: "none dispatched"))
            if (visible("BOOKING_CLERK")) add(DashTile("In transit", tiles.inTransit.toString(), "${tiles.inTransitPackages} packages"))
            if (visible("BOOKING_CLERK")) add(DashTile("Booked today", tiles.bookedToday.toString(), "${tiles.bookedTodayWeightKg} kg · ${m(tiles.bookedTodayFreightPaise)}"))
            if (visible("DELIVERY_CLERK")) add(DashTile("To Pay to collect", m(tiles.topayAwaitingPaise), "${tiles.topayAwaiting} consignments at branch", money = true, amberBar = true))
            if (visible("ACCOUNTANT")) add(DashTile("Unbilled freight", m(tiles.unbilledPaise), tiles.unbilledOldestDays?.let { "oldest $it days" } ?: "nothing waiting", money = true, amberBar = true))
            if (visible("ACCOUNTANT")) add(DashTile("Receivable", m(tiles.receivablePaise), m(tiles.receivableOver90Paise) + " over 90 days", money = true, amberBar = true))
            if (visible("BOOKING_CLERK")) add(DashTile("Exceptions", tiles.exceptions.sumOf { it.count }.toString(), "last 30 days"))
            if (visible("BOOKING_CLERK")) add(DashTile("Overdue arrivals", (tiles.ageing1to3 + tiles.ageing4to7 + tiles.ageing7plus).toString(), "past expected date"))
            if (visible("MANAGER")) add(DashTile("Vehicles idle", tiles.idleVehicles.size.toString(), "more than 7 days"))
        }

        _uiState.update { state ->
            state.copy(
                asOf = "as of " + SimpleDateFormat("h:mm a", Locale.ENGLISH).format(java.util.Date(now)),
                exceptions = exceptions.map { item ->
                    DashException(
                        title = "${item.biltyNo} held",
                        body = listOfNotNull(item.remark ?: item.reasonCode, item.atText).joinToString(" · "),
                        isLate = item.isLate,
                    )
                },
                tiles = tileList,
                thisMonthFigures = listOf(
                    m(tiles.month.freightPaise) to "Freight",
                    m(tiles.month.hirePaise) to "Hire",
                    m(tiles.month.marginPaise) to "Margin",
                ),
                thisMonthDelta = deltaText(tiles),
            )
        }
    }

    private fun deltaText(tiles: com.example.transportapp.data.transport.dashboard.DashboardData): String {
        val last = tiles.month.lastMonthFreightPaise
        if (last <= 0) return "no freight last month"
        val pct = ((tiles.month.freightPaise - last) * 100 / last).toInt()
        return (if (pct >= 0) "+$pct" else "$pct") + "% vs last month"
    }
}
