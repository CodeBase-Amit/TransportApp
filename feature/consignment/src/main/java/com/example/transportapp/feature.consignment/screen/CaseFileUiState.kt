package com.example.transportapp.feature.consignment.screen

import com.example.transportapp.core.ui.sample.CaseEvent
import com.example.transportapp.core.ui.sample.CaseFileMoneyLine
import com.example.transportapp.core.ui.sample.CaseFileSampleData
import com.example.transportapp.core.ui.sample.CaseFileStat
import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.PaymentMode

data class CaseFileUiState(
    val stats: List<CaseFileStat> = CaseFileSampleData.stats,
    val events: List<CaseEvent> = CaseFileSampleData.events,
    val moneyRows: List<CaseFileMoneyLine> = CaseFileSampleData.moneyRows,
    val fromStation: String = CaseFileSampleData.FROM_STATION,
    val toStation: String = CaseFileSampleData.TO_STATION,
    val distance: String = CaseFileSampleData.DISTANCE,
    val bookedText: String = CaseFileSampleData.BOOKED_TEXT,
    val toPayCallout: String = CaseFileSampleData.TOPAY_CALLOUT,
    val paymentMode: PaymentMode = PaymentMode.TOPAY,
    val status: ConsignmentStatus = ConsignmentStatus.IN_TRANSIT
)
