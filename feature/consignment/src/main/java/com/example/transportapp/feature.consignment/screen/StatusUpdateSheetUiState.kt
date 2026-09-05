package com.example.transportapp.feature.consignment.screen

import androidx.compose.runtime.Stable
import com.example.transportapp.domain.transport.ConsignmentStatus

/**
 * T9 — Status update bottom sheet. The next event is a chip, not a dropdown, and only the
 * §7.1-legal continuations appear (Design T9: "Booked and Loaded are absent and must not
 * be drawn greyed").
 */
data class SheetOption(
    val target: ConsignmentStatus,
    val label: String,
    val detail: String,
    val holdPath: Boolean = false,
)

enum class HoldReason(val label: String, val code: String) {
    SHORTAGE("Shortage", "SHORTAGE"),
    DAMAGE("Damage", "DAMAGE"),
    DETAINED("Detained", "DETAINED"),
    OTHER("Other", "OTHER")
}

@Stable
data class StatusUpdateSheetUiState(
    val biltyNo: String = "",
    val contextLine: String = "",
    val options: List<SheetOption> = emptyList(),
    val selected: SheetOption? = null,
    val holdReason: HoldReason = HoldReason.SHORTAGE,
    val remark: String = "",
    val location: String = "",
    /** S15 POD capture: the delivery branch needs a consignee name and a signature. */
    val consigneeName: String = "",
    val hasSignature: Boolean = false,
    val signatureClearSignal: Int = 0,
    /** S19: a picked POD photo is attached and rides the POD row (§7.4). */
    val photoAttached: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val isHold: Boolean get() = selected?.holdPath == true
    val isDelivery: Boolean get() = selected?.target == ConsignmentStatus.DELIVERED
}

sealed interface StatusUpdateSheetEvent {
    data class SelectOption(val option: SheetOption) : StatusUpdateSheetEvent
    data class SelectHoldReason(val reason: HoldReason) : StatusUpdateSheetEvent
    data class ChangeRemark(val value: String) : StatusUpdateSheetEvent
    data class ChangeLocation(val value: String) : StatusUpdateSheetEvent
    data class ChangeConsigneeName(val value: String) : StatusUpdateSheetEvent
    data class SetSignature(val hasInk: Boolean) : StatusUpdateSheetEvent
    data object ClearSignature : StatusUpdateSheetEvent
    /** S19: the Photo Picker answered — the uri is imported + compressed off the UI thread. */
    data class PhotoPicked(val uri: android.net.Uri) : StatusUpdateSheetEvent
    data object RemovePhoto : StatusUpdateSheetEvent
    /** The sheet exports the signed PNG first, then hands the file ref to the save (S15). */
    data class SaveWithSignature(val fileRef: String) : StatusUpdateSheetEvent
    data object UseMyLocation : StatusUpdateSheetEvent
    data object Save : StatusUpdateSheetEvent
}
