package com.example.transportapp.feature.consignment.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.example.transportapp.core.common.Money
import com.example.transportapp.core.common.formatIndianGrouping
import com.example.transportapp.core.ui.sample.RegisterListItem
import com.example.transportapp.core.ui.sample.RegisterRow
import com.example.transportapp.data.transport.consignment.RegisterDocket
import com.example.transportapp.data.transport.consignment.RegisterFilter
import com.example.transportapp.data.transport.consignment.RegisterRepository
import com.example.transportapp.data.transport.consignment.RegisterSummary
import com.example.transportapp.data.transport.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Design T7's chip row: three families that never touch, each an independent toggle. */
data class RegisterChip(val label: String, val kind: ChipKind, val selected: Boolean)

enum class ChipKind { IN_TRANSIT, THIS_MONTH, ALL_BRANCHES, TO_PAY, UNBILLED, HELD, DELIVERED }

data class RegisterUiState(
    val searchQuery: String = "",
    val chips: List<RegisterChip> = defaultChips(),
    val summary: RegisterSummary? = null,
    val isEmptyRegister: Boolean = false,
    val isLoading: Boolean = true,
    val companyInitials: String = "SR",
    val companyName: String = "",
    val branchName: String = "",
) {
    companion object {
        fun defaultChips(selected: Set<ChipKind> = emptySet()) =
            listOf(
                RegisterChip("In transit", ChipKind.IN_TRANSIT, ChipKind.IN_TRANSIT in selected),
                RegisterChip("This month", ChipKind.THIS_MONTH, ChipKind.THIS_MONTH in selected),
                RegisterChip("All branches", ChipKind.ALL_BRANCHES, ChipKind.ALL_BRANCHES in selected),
                RegisterChip("To Pay", ChipKind.TO_PAY, ChipKind.TO_PAY in selected),
                RegisterChip("Unbilled", ChipKind.UNBILLED, ChipKind.UNBILLED in selected),
                RegisterChip("Held", ChipKind.HELD, ChipKind.HELD in selected),
                RegisterChip("Delivered", ChipKind.DELIVERED, ChipKind.DELIVERED in selected),
            )
    }
}

sealed interface RegisterEvent {
    data class ChangeSearchQuery(val query: String) : RegisterEvent
    data class ToggleChip(val kind: ChipKind) : RegisterEvent
    data object ClearFilters : RegisterEvent
}

/**
 * T7's live register (Phase2.md S6): Paging 3 (D6) over the real consignments with the
 * filter chips, a debounced LIKE search (D7), the summary-strip aggregates reflecting the
 * same filter, and day section headers inserted between pages.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val registerRepository: RegisterRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val filter = MutableStateFlow(RegisterFilter())
    private var searchJob: Job? = null

    /** Separator insertion needs a common supertype: domain row or day header. */
    private sealed interface PageItem {
        data class Docket(val docket: RegisterDocket) : PageItem
        data class Header(val label: String) : PageItem
    }

    val items: Flow<PagingData<RegisterListItem>> =
        filter.flatMapLatest { f ->
            viewModelScope.let {
                sessionRepository.session.first().let { session ->
                    registerRepository.pagingRegister(session.companyId, session.branchId, f)
                }
            }
        }.cachedIn(viewModelScope)
            .map { paging ->
                paging
                    .map { PageItem.Docket(it) }
                    // Day section headers are inserted over the domain rows (which carry
                    // bookedAt), then the rows map onto the screen structs.
                    .insertSeparators { before: PageItem?, after: PageItem? ->
                        val afterDocket = after as? PageItem.Docket ?: return@insertSeparators null
                        val beforeDocket = before as? PageItem.Docket
                        when {
                            beforeDocket == null -> PageItem.Header(headerLabel(afterDocket.docket.bookedAt))
                            dayOf(beforeDocket.docket.bookedAt) != dayOf(afterDocket.docket.bookedAt) ->
                                PageItem.Header(headerLabel(afterDocket.docket.bookedAt))
                            else -> null
                        }
                    }
                    .map { item ->
                        when (item) {
                            is PageItem.Header -> RegisterListItem.Header(item.label)
                            is PageItem.Docket -> RegisterListItem.Row(
                                RegisterRow(
                                    docNumber = item.docket.displayNo,
                                    consignee = item.docket.consigneeName,
                                    amount = Money(item.docket.amountPaise).formatted(),
                                    from = item.docket.fromStation,
                                    to = item.docket.toStation,
                                    status = item.docket.status,
                                    paymentMode = item.docket.paymentMode,
                                    caption = "${item.docket.packages} pkg · ${formatIndianGrouping(item.docket.weightKg)} kg",
                                    exception = item.docket.heldRemark,
                                    syncPending = item.docket.syncPending,
                                ),
                            )
                        }
                    }
            }

    init {
        viewModelScope.launch {
            filter.collect { f ->
                val session = sessionRepository.session.first()
                _uiState.update {
                    it.copy(
                        chips = RegisterUiState.defaultChips(selectedChips(f)),
                        isLoading = true,
                        companyInitials = session.companyName.split(" ").mapNotNull { w -> w.firstOrNull() }.take(2).joinToString("").ifEmpty { "SR" },
                        companyName = session.companyName,
                        branchName = session.branchName,
                    )
                }
                val summary = registerRepository.summary(session.companyId, session.branchId, f)
                _uiState.update { it.copy(summary = summary, isLoading = false) }
            }
        }
    }

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.ChangeSearchQuery -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                searchJob?.cancel()
                searchJob = viewModelScope.launch {
                    delay(SEARCH_DEBOUNCE_MS)
                    filter.update { it.copy(search = event.query.trim().takeIf { q -> q.isNotEmpty() }) }
                }
            }
            is RegisterEvent.ToggleChip -> filter.update { f ->
                when (event.kind) {
                    ChipKind.IN_TRANSIT, ChipKind.HELD, ChipKind.DELIVERED -> {
                        // The three journey chips are mutually exclusive.
                        val next = if (f.status == statusOf(event.kind)) null else statusOf(event.kind)
                        f.copy(status = next)
                    }
                    ChipKind.TO_PAY -> f.copy(paymentMode = if (f.paymentMode == com.example.transportapp.domain.transport.PaymentMode.TOPAY) null else com.example.transportapp.domain.transport.PaymentMode.TOPAY)
                    ChipKind.UNBILLED -> f.copy(unbilledOnly = !f.unbilledOnly)
                    ChipKind.ALL_BRANCHES -> f.copy(allBranches = !f.allBranches)
                    ChipKind.THIS_MONTH -> f.copy(sinceAt = if (f.sinceAt == null) monthStart() else null)
                }
            }
            RegisterEvent.ClearFilters -> {
                _uiState.update { it.copy(searchQuery = "") }
                filter.value = RegisterFilter()
            }
        }
    }

    private fun selectedChips(f: RegisterFilter): Set<ChipKind> = buildSet {
        if (f.status == com.example.transportapp.domain.transport.ConsignmentStatus.IN_TRANSIT) add(ChipKind.IN_TRANSIT)
        if (f.status == com.example.transportapp.domain.transport.ConsignmentStatus.HELD) add(ChipKind.HELD)
        if (f.status == com.example.transportapp.domain.transport.ConsignmentStatus.DELIVERED) add(ChipKind.DELIVERED)
        if (f.paymentMode != null) add(ChipKind.TO_PAY)
        if (f.unbilledOnly) add(ChipKind.UNBILLED)
        if (f.allBranches) add(ChipKind.ALL_BRANCHES)
        if (f.sinceAt != null) add(ChipKind.THIS_MONTH)
    }

    private fun statusOf(kind: ChipKind) = when (kind) {
        ChipKind.IN_TRANSIT -> com.example.transportapp.domain.transport.ConsignmentStatus.IN_TRANSIT
        ChipKind.HELD -> com.example.transportapp.domain.transport.ConsignmentStatus.HELD
        ChipKind.DELIVERED -> com.example.transportapp.domain.transport.ConsignmentStatus.DELIVERED
        else -> null
    }

    private fun monthStart(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun dayOf(bookedAt: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date(bookedAt))

    private fun headerLabel(bookedAt: Long): String {
        val stamp = SimpleDateFormat("d MMM", Locale.ENGLISH).format(Date(bookedAt)).uppercase(Locale.ENGLISH)
        return when (dayOf(bookedAt)) {
            dayOf(System.currentTimeMillis()) -> "TODAY · $stamp"
            dayOf(System.currentTimeMillis() - 86_400_000L) -> "YESTERDAY · $stamp"
            else -> stamp
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 150L
    }
}
