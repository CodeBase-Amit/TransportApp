package com.example.transportapp.core.ui.sample

object FreightBillSampleData {

    data class RemovalRow(val bilty: String, val route: String, val date: String, val amount: String)

    val party = SampleData.BILL_PARTY
    val gstin = SampleData.BILL_GSTIN
    val partyLine = "Indore · GSTIN ${SampleData.BILL_GSTIN}"
    val period = SampleData.BILL_PERIOD
    val consignments = SampleData.BILL_CONSIGNMENTS
    val freight = SampleData.BILL_FREIGHT
    val gst = SampleData.BILL_GST
    val total = SampleData.BILL_TOTAL
    val due = SampleData.BILL_DUE

    val whatsOnTitle = "What's on it · 23"
    val removeSome = "Remove some"
    val showAll = "Show all 23"

    val removalRows = listOf(
        RemovalRow("IND-9281", "Bhopal to Indore", "12 Jul", "18,240.00"),
        RemovalRow("IND-9310", "Bhopal to Ujjain", "14 Jul", "12,500.00"),
        RemovalRow("IND-9344", "Bhopal to Ratlam", "18 Jul", "24,180.00"),
        RemovalRow("IND-9402", "Bhopal to Indore", "22 Jul", "16,400.00"),
        RemovalRow("IND-9451", "Bhopal to Dewas", "28 Jul", "15,100.00")
    )

    val previewAndIssue = "Preview and issue"
    val draftBar = "DRAFT · NOT ISSUED · NO NUMBER YET"
    val issueNotice = "Issuing assigns FB/IND/2627/00311 and locks the 23 consignments. Needs a connection."
    val backToEdit = "Back to edit"
    val issueThisBill = "Issue this bill"
    val notesOnBill = "Optional, printed under the totals"

    val issuedNo = SampleData.FREIGHT_BILL_NO
    val issuedLine = "issued 25 Aug 2026 by Mahesh Patidar · due 30 Sep 2026"
    val outstandingLine = "90,741.00 outstanding · nothing received yet"
    val recordReceipt = "Record a receipt"
    val paperCompany = "SHIVSHAKTI ROADLINES"
    val paperDocType = "FREIGHT BILL"
    val paperInvoiceLine = "Rupees ninety thousand seven hundred forty one only"
}
