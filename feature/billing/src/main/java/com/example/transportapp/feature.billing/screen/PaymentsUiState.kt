package com.example.transportapp.feature.billing.screen

import androidx.compose.runtime.Stable
import com.example.transportapp.data.transport.billing.OutstandingBill
import com.example.transportapp.data.transport.billing.ReceiptLine
import com.example.transportapp.data.transport.billing.TopayLine

/** The collect sheet over one To Pay consignment (Design T15). */
data class CollectSheetState(
    val line: TopayLine,
    val amountText: String,
    val mode: String = "CASH",
    val reference: String = "",
    val isManager: Boolean = false,
    val waiverReason: String = "",
    val waiving: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
)

/** The record-a-receipt sheet with its explicit allocation (§12.2). */
@Stable
data class AllocationSheetState(
    val parties: List<Pair<String, String>> = emptyList(), // id to name
    val partyId: String? = null,
    val partyName: String = "",
    val amountText: String = "",
    val mode: String = "NEFT",
    val reference: String = "",
    val bills: List<OutstandingBill> = emptyList(),
    val applied: Map<String, String> = emptyMap(), // bill id to rupee text
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
) {
    /** Paise applied across the bills, as typed. */
    val appliedPaise: Long
        get() = applied.values.sumOf { parseRupees(it) }
    val amountPaise: Long get() = parseRupees(amountText)
    val unappliedPaise: Long get() = amountPaise - appliedPaise
    val canSave: Boolean get() = amountPaise > 0 && unappliedPaise == 0L && partyId != null && !saving
}

@Stable
data class PaymentsUiState(
    val tab: PaymentsTab = PaymentsTab.TOPAY,
    val toPayRows: List<TopayLine> = emptyList(),
    val receipts: List<ReceiptLine> = emptyList(),
    val receivedThisMonthPaise: Long = 0,
    val receiptsCount: Int = 0,
    val collectSheet: CollectSheetState? = null,
    val allocationSheet: AllocationSheetState? = null,
) {
    val toCollectPaise: Long get() = toPayRows.sumOf { it.amountPaise }
}

enum class PaymentsTab { TOPAY, BILL_RECEIPTS }

sealed interface PaymentsEvent {
    data class SelectTab(val tab: PaymentsTab) : PaymentsEvent
    data class OpenCollect(val line: TopayLine) : PaymentsEvent
    data object DismissCollect : PaymentsEvent
    data class SetCollectAmount(val text: String) : PaymentsEvent
    data class SetCollectMode(val mode: String) : PaymentsEvent
    data class SetCollectReference(val text: String) : PaymentsEvent
    data class SetWaiverReason(val text: String) : PaymentsEvent
    data object RecordWaiver : PaymentsEvent
    data object SaveCollect : PaymentsEvent
    data object OpenAllocation : PaymentsEvent
    data object DismissAllocation : PaymentsEvent
    data class SelectParty(val partyId: String) : PaymentsEvent
    data class SetAmount(val text: String) : PaymentsEvent
    data class SetMode(val mode: String) : PaymentsEvent
    data class SetReference(val text: String) : PaymentsEvent
    data class SetApplied(val billId: String, val text: String) : PaymentsEvent
    data object ParkTheRest : PaymentsEvent
    data object SaveAllocation : PaymentsEvent
}

internal fun parseRupees(text: String): Long {
    val clean = text.filter { it.isDigit() || it == '.' }
    if (clean.isEmpty()) return 0
    val whole = clean.substringBefore('.').toLongOrNull() ?: 0
    val frac = clean.substringAfter('.', "").take(2)
    val paise = when (frac.length) {
        0 -> 0L
        1 -> frac.toLong() * 10
        else -> frac.toLong()
    }
    return whole * 100 + paise
}
