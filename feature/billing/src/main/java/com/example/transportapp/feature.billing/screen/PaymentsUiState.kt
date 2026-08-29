package com.example.transportapp.feature.billing.screen

import com.example.transportapp.core.ui.sample.PaymentsSampleData

data class PaymentsUiState(
    val title: String = PaymentsSampleData.title,
    val subtitle: String = PaymentsSampleData.subtitle,
    val toPayTab: String = PaymentsSampleData.toPayTab,
    val billReceiptsTab: String = PaymentsSampleData.billReceiptsTab,
    val toCollect: String = PaymentsSampleData.toCollect,
    val atIndore: String = PaymentsSampleData.atIndore,
    val toPayRows: List<PaymentsSampleData.ToPayRow> = PaymentsSampleData.toPayRows,
    val receivedThisMonth: String = PaymentsSampleData.receivedThisMonth,
    val receipts: String = PaymentsSampleData.receipts,
    val receiptModeLine: String = PaymentsSampleData.receiptModeLine,
    val receiptRows: List<Triple<String, String, String>> = PaymentsSampleData.receiptRows,
    val tab: PaymentsTab = PaymentsTab.TOPAY
)

enum class PaymentsTab { TOPAY, BILL_RECEIPTS }

sealed interface PaymentsEvent {
    data class SelectTab(val tab: PaymentsTab) : PaymentsEvent
}
