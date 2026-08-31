package com.example.transportapp.feature.billing.screen

import com.example.transportapp.data.transport.billing.BillConsignmentLine
import com.example.transportapp.data.transport.billing.BillHeader

data class FreightBillUiState(
    val billId: String = "",
    val bill: BillHeader? = null,
    val rows: List<BillConsignmentLine> = emptyList(),
    val outstandingPaise: Long = 0,
    val previewing: Boolean = false,
    val issueError: String? = null,
    val removing: Boolean = false,
) {
    val isIssued: Boolean get() = bill?.state == "ISSUED"
    val isCancelled: Boolean get() = bill?.state == "CANCELLED"
    val stage: Stage
        get() = when {
            isIssued || isCancelled -> Stage.ISSUED
            previewing -> Stage.PREVIEW
            else -> Stage.DRAFT
        }

    enum class Stage { DRAFT, PREVIEW, ISSUED }
}

sealed interface FreightBillEvent {
    data class RemoveRow(val consignmentId: String) : FreightBillEvent
    data object ShowPreview : FreightBillEvent
    data object BackToEdit : FreightBillEvent
    data object Issue : FreightBillEvent
    data object DismissError : FreightBillEvent
}
