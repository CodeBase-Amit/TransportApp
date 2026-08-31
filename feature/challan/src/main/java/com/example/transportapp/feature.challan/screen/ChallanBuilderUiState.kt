package com.example.transportapp.feature.challan.screen

import com.example.transportapp.core.common.Money
import com.example.transportapp.core.common.formatIndianGrouping
import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.PaymentMode

/** One pickable consignment in the §11.2 loadable pool. */
data class LoadableRow(
    val localId: String,
    val docNumber: String,
    val consignee: String,
    val packages: String,
    val weight: String,
    val amount: String,
    val paymentMode: PaymentMode,
    val status: ConsignmentStatus,
    val isOnwardLeg: Boolean = false,
    val onwardNote: String? = null,
    val weightKg: Long = 0,
    val totalPaise: Long = 0,
    val toStationId: String = "",
    val toStation: String = "",
)

data class ChallanBuilderUiState(
    val reservedNumber: String = "",
    val filterChips: List<String> = listOf("Booked here", "Arrived from elsewhere"),
    val selectedFilter: String = "Booked here",
    val readyToLoad: String = "READY TO LOAD",
    val loadable: List<LoadableRow> = emptyList(),
    val selectedBilties: Set<String> = emptySet(),
    val vehicleNumber: String = "",
    val vehicleOwnership: String = "",
    val driverName: String = "",
    val driverLicenceLine: String = "",
    val routeTo: String = "",
    val routeVia: String = "",
    val lorryHire: String = Money(1_850_000).formatted(),
    val advancePaid: String = Money(1_200_000).formatted(),
    val balance: String = Money(650_000).formatted(),
    val capacityKg: Int = 0,
    val selectedWeightKg: Int = 0,
    val overByKg: Int = 0,
    val overloaded: Boolean = false,
    val freightTotal: String = "0.00",
    val selectAll: String = "Select all",
    val createChallan: String = "Create challan",
    val consignmentsSuffix: String = "consignments",
    val isLoading: Boolean = true,
    val error: String? = null,
    /** One-shot: the stamped challan number after create+issue; navigates to T11. */
    val createdChallanNo: String? = null,
)

sealed interface ChallanBuilderEvent {
    data object ToggleSelectAll : ChallanBuilderEvent
    data class ToggleConsignment(val docNumber: String) : ChallanBuilderEvent
    data class SelectFilter(val filter: String) : ChallanBuilderEvent
    data object Create : ChallanBuilderEvent
}
