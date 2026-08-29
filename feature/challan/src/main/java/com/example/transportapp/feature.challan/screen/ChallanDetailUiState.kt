package com.example.transportapp.feature.challan.screen

import com.example.transportapp.core.ui.sample.ChallanDetailSampleData

data class ChallanDetailUiState(
    val challanNo: String = ChallanDetailSampleData.challanNo,
    val statusLabel: String = ChallanDetailSampleData.statusOpen,
    val routeFrom: String = ChallanDetailSampleData.challanRouteFrom,
    val routeTo: String = ChallanDetailSampleData.challanRouteTo,
    val routeVia: String = ChallanDetailSampleData.challanRouteVia,
    val createdLine: String = ChallanDetailSampleData.createdLine,
    val dispatchedLine: String = ChallanDetailSampleData.dispatchedLine,
    val consignments: Int = ChallanDetailSampleData.consignments,
    val loadKg: String = ChallanDetailSampleData.loadKg,
    val hire: String = ChallanDetailSampleData.hire,
    val balance: String = ChallanDetailSampleData.balance,
    val whatsLoadedTitle: String = ChallanDetailSampleData.whatsLoadedTitle,
    val whatsLoadedAction: String = ChallanDetailSampleData.whatsLoadedAction,
    val showAll: String = ChallanDetailSampleData.showAll,
    val editLoad: String = ChallanDetailSampleData.editLoad,
    val vehicleAndDriverHeading: String = ChallanDetailSampleData.vehicleAndDriverHeading,
    val vehicleNumber: String = ChallanDetailSampleData.vehicleNumber,
    val vehicleOwnership: String = ChallanDetailSampleData.vehicleOwnership,
    val driverInitials: String = ChallanDetailSampleData.driverInitials,
    val driverLine: String = ChallanDetailSampleData.driverLine,
    val challanGroups: List<ChallanDetailSampleData.ChallanStationGroup> = ChallanDetailSampleData.challanGroups,
    val paperCompany: String = ChallanDetailSampleData.paperCompany,
    val paperDocType: String = ChallanDetailSampleData.paperDocType,
    val paperChallanNo: String = ChallanDetailSampleData.paperChallanNo,
    val paperVehicle: String = ChallanDetailSampleData.paperVehicle,
    val paperBiltyLines: List<ChallanDetailSampleData.BiltyLine> = ChallanDetailSampleData.paperBiltyLines,
    val paperSeeFull: String = ChallanDetailSampleData.paperSeeFull,
    val dispatchedNotice: String = ChallanDetailSampleData.dispatchedNotice,
    val isDispatched: Boolean = false
)

sealed interface ChallanDetailEvent {
    data object Dispatch : ChallanDetailEvent
    data object EditLoad : ChallanDetailEvent
    data object Print : ChallanDetailEvent
    data object Share : ChallanDetailEvent
    data object More : ChallanDetailEvent
}
