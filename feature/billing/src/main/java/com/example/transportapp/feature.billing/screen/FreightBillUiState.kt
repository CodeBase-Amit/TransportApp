package com.example.transportapp.feature.billing.screen

import com.example.transportapp.core.ui.sample.FreightBillSampleData

data class FreightBillUiState(
    val party: String = FreightBillSampleData.party,
    val gstin: String = FreightBillSampleData.gstin,
    val partyLine: String = FreightBillSampleData.partyLine,
    val period: String = FreightBillSampleData.period,
    val consignments: Int = FreightBillSampleData.consignments,
    val freight: String = FreightBillSampleData.freight,
    val gst: String = FreightBillSampleData.gst,
    val total: String = FreightBillSampleData.total,
    val due: String = FreightBillSampleData.due,
    val whatsOnTitle: String = FreightBillSampleData.whatsOnTitle,
    val removeSome: String = FreightBillSampleData.removeSome,
    val showAll: String = FreightBillSampleData.showAll,
    val removalRows: List<FreightBillSampleData.RemovalRow> = FreightBillSampleData.removalRows,
    val previewAndIssue: String = FreightBillSampleData.previewAndIssue,
    val draftBar: String = FreightBillSampleData.draftBar,
    val issueNotice: String = FreightBillSampleData.issueNotice,
    val backToEdit: String = FreightBillSampleData.backToEdit,
    val issueThisBill: String = FreightBillSampleData.issueThisBill,
    val notesOnBill: String = FreightBillSampleData.notesOnBill,
    val issuedNo: String = FreightBillSampleData.issuedNo,
    val issuedLine: String = FreightBillSampleData.issuedLine,
    val outstandingLine: String = FreightBillSampleData.outstandingLine,
    val recordReceipt: String = FreightBillSampleData.recordReceipt,
    val paperCompany: String = FreightBillSampleData.paperCompany,
    val paperDocType: String = FreightBillSampleData.paperDocType,
    val paperInvoiceLine: String = FreightBillSampleData.paperInvoiceLine,
    val state: BillState = BillState.DRAFT
)

enum class BillState { DRAFT, PREVIEW, ISSUED }

sealed interface FreightBillEvent {
    data class ChangeState(val state: BillState) : FreightBillEvent
}
