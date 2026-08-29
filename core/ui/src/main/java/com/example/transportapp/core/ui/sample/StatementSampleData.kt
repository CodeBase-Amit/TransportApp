package com.example.transportapp.core.ui.sample

object StatementSampleData {

    data class LedgerRow(val date: String, val docNo: String, val desc: String, val debit: String, val credit: String, val balance: String)

    val party = SampleData.BILL_PARTY
    val partySubtitle = "Indore · GSTIN 23AACDS8812K1Z4"
    val period = "1 Apr 2026 – 25 Aug 2026"
    val branchScope = "All branches"

    val opening = SampleData.OPENING_BALANCE
    val closing = SampleData.CLOSING_BALANCE
    val over90 = SampleData.OVER90_AGEING
    val ofWhich = "of which ${SampleData.OVER90_AGEING}"

    val ledgerRows = listOf(
        LedgerRow("12 Jul", "FB/IND/2627/00298", "freight bill", "22,400.00", "—", "3,64,500.00"),
        LedgerRow("20 Jul", "RCPT/IND/2627/00482", "NEFT", "—", "45,000.00", "3,19,500.00"),
        LedgerRow("02 Aug", "FB/IND/2627/00305", "freight bill", "18,500.00", "—", "3,38,000.00"),
        LedgerRow("10 Aug", "RCPT/IND/2627/00501", "Cash", "—", "12,000.00", "3,26,000.00"),
        LedgerRow("20 Aug", "RCPT/IND/2627/00518", "NEFT", "—", "50,000.00", "2,76,000.00"),
        LedgerRow("25 Aug", "FB/IND/2627/00311", "freight bill", "90,741.00", "—", "3,66,741.00"),
        LedgerRow("25 Aug", "RCPT/IND/2627/00530", "NEFT", "—", "40,300.00", "3,26,441.00")
    )

    val sendStatement = "Send statement as PDF"
    val changePeriod = "Change period"
}
