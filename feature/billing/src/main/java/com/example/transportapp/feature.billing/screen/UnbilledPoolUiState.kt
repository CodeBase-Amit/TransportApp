package com.example.transportapp.feature.billing.screen

import com.example.transportapp.data.transport.billing.BillConsignmentLine
import com.example.transportapp.data.transport.billing.UnbilledPartyGroup

/** One T13 party card, with its selection and expansion as the user left it. */
data class UnbilledPartyState(
    val group: UnbilledPartyGroup,
    val selected: Boolean = false,
    val expanded: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val rows: List<BillConsignmentLine> = emptyList(),
)

data class UnbilledPoolUiState(
    val parties: List<UnbilledPartyState> = emptyList(),
    val thisQuarter: Boolean = true,
    val allBranches: Boolean = true,
    val minAgeDays: Int? = null,
    val building: Boolean = false,
    val error: String? = null,
) {
    val summaryParties: Int get() = parties.size
    val summaryConsignments: Int get() = parties.sumOf { it.group.consignments }
    val summaryFreightPaise: Long get() = parties.sumOf { it.group.freightPaise }

    /** What the sticky bar totals: the explicit per-consignment selection. */
    val selectedIds: Set<String> get() = parties.flatMap { p -> p.selectedIds }.toSet()
    val selectedConsignments: Int get() = selectedIds.size
    val selectedPaise: Long get() = parties.sumOf { p -> p.rows.filter { it.localId in p.selectedIds }.sumOf { it.totalPaise } + if (p.selected && p.selectedIds.isEmpty()) p.group.totalPaise else 0L }
    val selectedPartyCount: Int get() = parties.count { it.selected || it.selectedIds.isNotEmpty() }
    val canBuild: Boolean get() = selectedPartyCount > 0 && !building
}

sealed interface UnbilledPoolEvent {
    data class ToggleParty(val partyId: String) : UnbilledPoolEvent
    data class ToggleConsignment(val partyId: String, val consignmentId: String) : UnbilledPoolEvent
    data class ToggleExpand(val partyId: String) : UnbilledPoolEvent
    data class ToggleQuarter(val on: Boolean) : UnbilledPoolEvent
    data class ToggleAllBranches(val on: Boolean) : UnbilledPoolEvent
    data class SetAgeFilter(val minAgeDays: Int?) : UnbilledPoolEvent
    data object BuildBill : UnbilledPoolEvent
    data object DismissError : UnbilledPoolEvent
}
