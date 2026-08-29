package com.example.transportapp.feature.billing.screen

import com.example.transportapp.core.ui.sample.StatementSampleData

data class StatementUiState(
    val party: String = StatementSampleData.party,
    val partySubtitle: String = StatementSampleData.partySubtitle,
    val period: String = StatementSampleData.period,
    val branchScope: String = StatementSampleData.branchScope,
    val opening: String = StatementSampleData.opening,
    val closing: String = StatementSampleData.closing,
    val ofWhich: String = StatementSampleData.ofWhich,
    val ledgerRows: List<StatementSampleData.LedgerRow> = StatementSampleData.ledgerRows,
    val sendStatement: String = StatementSampleData.sendStatement,
    val changePeriod: String = StatementSampleData.changePeriod
)

sealed interface StatementEvent {
    data object Download : StatementEvent
    data object ChangePeriod : StatementEvent
    data object SendPdf : StatementEvent
}
