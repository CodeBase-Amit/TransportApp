package com.example.transportapp.feature.billing.screen

data class StatementLedgerRow(
    val date: String,
    val docNo: String,
    val desc: String,
    val debit: String,
    val credit: String,
    val balance: String,
)

data class StatementUiState(
    val party: String = "",
    val partySubtitle: String = "",
    val period: String = "",
    val opening: String = "0.00 Dr",
    val ledgerRows: List<StatementLedgerRow> = emptyList(),
    val closing: String = "0.00 Dr",
    val ofWhich: String = "",
    val loading: Boolean = true,
    val error: String? = null,
)

sealed interface StatementEvent {
    data object SendPdf : StatementEvent
}
