package com.example.transportapp.feature.challan.screen

import com.example.transportapp.core.ui.sample.ChallanBuilderSampleData

data class ChallanBuilderUiState(
    val reservedNumber: String = ChallanBuilderSampleData.reservedNumber,
    val filterChips: List<String> = ChallanBuilderSampleData.filterChips,
    val selectedFilter: String = ChallanBuilderSampleData.filterChips.first(),
    val readyToLoad: String = ChallanBuilderSampleData.readyToLoad,
    val loadable: List<ChallanBuilderSampleData.LoadableConsignment> = ChallanBuilderSampleData.loadable,
    val selectedBilties: Set<String> = setOf("IND/2627/04188", "IND/2627/04191", "IND/2627/04192"),
    val vehicleNumber: String = ChallanBuilderSampleData.vehicleNumber,
    val vehicleOwnership: String = ChallanBuilderSampleData.vehicleOwnership,
    val driverName: String = ChallanBuilderSampleData.driverName,
    val driverLicenceLine: String = ChallanBuilderSampleData.driverLicenceLine,
    val routeTo: String = ChallanBuilderSampleData.routeTo,
    val routeVia: String = ChallanBuilderSampleData.routeVia,
    val lorryHire: String = ChallanBuilderSampleData.lorryHire,
    val advancePaid: String = ChallanBuilderSampleData.advancePaid,
    val balance: String = ChallanBuilderSampleData.balance,
    val capacityKg: Int = ChallanBuilderSampleData.capacityKg,
    val freightTotal: String = ChallanBuilderSampleData.freightTotal,
    val selectAll: String = ChallanBuilderSampleData.selectAll,
    val createChallan: String = ChallanBuilderSampleData.createChallan,
    val consignmentsSuffix: String = ChallanBuilderSampleData.consignmentsSuffix
)

sealed interface ChallanBuilderEvent {
    data object ToggleSelectAll : ChallanBuilderEvent
    data class ToggleConsignment(val bilty: String) : ChallanBuilderEvent
    data class SelectFilter(val filter: String) : ChallanBuilderEvent
}
