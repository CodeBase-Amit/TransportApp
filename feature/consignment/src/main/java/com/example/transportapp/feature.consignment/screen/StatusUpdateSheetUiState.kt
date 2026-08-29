package com.example.transportapp.feature.consignment.screen

import com.example.transportapp.core.ui.sample.StatusUpdateSheetSampleData

/**
 * T9 — Status update bottom sheet. The next event is a chip, not a dropdown.
 */
enum class StatusEventOption(val label: String, val detail: String, val holdPath: Boolean = false) {
    DEPARTED("Departed Dhule — back in transit", "The usual next step from At hub"),
    ARRIVED("Arrived at Nashik", "Reached the destination branch"),
    OUT_FOR_DELIVERY("Out for delivery", "Door delivery loaded out"),
    DELIVERED("Delivered", "POD captured"),
    HOLD("Hold", "Exception — needs a reason", holdPath = true),
    RETURN("Return to origin", "RTO decision")
}

enum class HoldReason(val label: String) { SHORTAGE("Shortage"), DAMAGE("Damage"), DETAINED("Detained"), OTHER("Other") }

data class StatusUpdateSheetUiState(
    val selectedEvent: StatusEventOption = StatusEventOption.DEPARTED,
    val holdReason: HoldReason = HoldReason.SHORTAGE,
    val remark: String = "",
    val location: String = StatusUpdateSheetSampleData.LOCATION
) {
    val isHold: Boolean get() = selectedEvent == StatusEventOption.HOLD
}

sealed interface StatusUpdateSheetEvent {
    data class SelectEvent(val event: StatusEventOption) : StatusUpdateSheetEvent
    data class SelectHoldReason(val reason: HoldReason) : StatusUpdateSheetEvent
    data class ChangeRemark(val value: String) : StatusUpdateSheetEvent
    data class ChangeLocation(val value: String) : StatusUpdateSheetEvent
    data object UseMyLocation : StatusUpdateSheetEvent
}
