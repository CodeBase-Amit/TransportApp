package com.example.transportapp.feature.masters.screen

import com.example.transportapp.domain.transport.masters.PartyDetail

data class MasterEditorUiState(
    val title: String = "Edit party",
    val identityHeading: String = "Identity",
    val addressHeading: String = "Address",
    val taxHeading: String = "Tax Information",
    val defaultsHeading: String = "Defaults",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val street: String = "",
    val station: String = "",
    val pincode: String = "",
    val gstin: String = "",
    val taxStatus: String = "Verified active taxpayer",
    val usualRoute: String = "",
    val rateCard: String = "",
    val typeOptions: List<String> = listOf("Consignor", "Consignee", "Both"),
    val type: String = "Both",
    val paymentOptions: List<String> = listOf("Paid", "To Pay", "TBB"),
    val payment: String = "TBB",
    val deleteLabel: String = "Delete this record",
    val deleteMessage: String = "",
    val saveLabel: String = "Save party",
    val saveTopLabel: String = "Save",
    val isNew: Boolean = false,
    val isLoading: Boolean = true,
    val justSaved: Boolean = false,
    val error: String? = null,
) {
    companion object {
        fun from(detail: PartyDetail?) = if (detail == null) {
            MasterEditorUiState(title = "New party", isNew = true, isLoading = false, deleteMessage = "")
        } else {
            MasterEditorUiState(
                name = detail.name,
                email = detail.email.orEmpty(),
                phone = detail.phone,
                street = detail.street.orEmpty(),
                station = detail.station.orEmpty(),
                pincode = detail.pincode.orEmpty(),
                gstin = detail.gstin.orEmpty(),
                taxStatus = if (detail.gstin.isNullOrBlank()) "" else "Verified active taxpayer",
                usualRoute = detail.usualRoute.orEmpty(),
                rateCard = detail.rateCardLabel.orEmpty(),
                type = when (detail.type) {
                    "CONSIGNOR" -> "Consignor"
                    "CONSIGNEE" -> "Consignee"
                    else -> "Both"
                },
                payment = when (detail.usualPaymentMode) {
                    "PAID" -> "Paid"
                    "TOPAY" -> "To Pay"
                    "TBB" -> "TBB"
                    else -> "TBB"
                },
                deleteMessage = if (detail.biltyCount > 0) {
                    "${detail.biltyCount} bilties use this party, so it can't be deleted. You can mark it inactive instead."
                } else {
                    ""
                },
                isLoading = false,
            )
        }
    }
}

sealed interface MasterEditorEvent {
    data class ChangeName(val value: String) : MasterEditorEvent
    data class ChangeEmail(val value: String) : MasterEditorEvent
    data class ChangePhone(val value: String) : MasterEditorEvent
    data class ChangeStreet(val value: String) : MasterEditorEvent
    data class ChangeStation(val value: String) : MasterEditorEvent
    data class ChangePincode(val value: String) : MasterEditorEvent
    data class ChangeGstin(val value: String) : MasterEditorEvent
    data class ChangeRoute(val value: String) : MasterEditorEvent
    data class ChangeRateCard(val value: String) : MasterEditorEvent
    data class SelectType(val value: String) : MasterEditorEvent
    data class SelectPayment(val value: String) : MasterEditorEvent
    data object Save : MasterEditorEvent
    data object Delete : MasterEditorEvent
}
