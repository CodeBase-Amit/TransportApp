package com.example.transportapp.feature.masters.screen

import com.example.transportapp.core.ui.sample.MasterEditorSampleData

data class MasterEditorUiState(
    val title: String = MasterEditorSampleData.TITLE,
    val identityHeading: String = MasterEditorSampleData.IDENTITY_HEADING,
    val addressHeading: String = MasterEditorSampleData.ADDRESS_HEADING,
    val taxHeading: String = MasterEditorSampleData.TAX_HEADING,
    val defaultsHeading: String = MasterEditorSampleData.DEFAULTS_HEADING,
    val name: String = MasterEditorSampleData.NAME,
    val email: String = MasterEditorSampleData.EMAIL,
    val phone: String = MasterEditorSampleData.PHONE,
    val street: String = MasterEditorSampleData.STREET_ADDRESS,
    val station: String = MasterEditorSampleData.STATION,
    val pincode: String = MasterEditorSampleData.PINCODE,
    val gstin: String = MasterEditorSampleData.GSTIN,
    val taxStatus: String = MasterEditorSampleData.TAX_STATUS,
    val usualRoute: String = MasterEditorSampleData.USUAL_ROUTE,
    val rateCard: String = MasterEditorSampleData.RATE_CARD,
    val typeOptions: List<String> = MasterEditorSampleData.typeOptions,
    val type: String = MasterEditorSampleData.DEFAULT_TYPE,
    val paymentOptions: List<String> = MasterEditorSampleData.paymentOptions,
    val payment: String = MasterEditorSampleData.DEFAULT_PAYMENT,
    val deleteLabel: String = MasterEditorSampleData.DELETE_LABEL,
    val deleteMessage: String = MasterEditorSampleData.DELETE_MESSAGE,
    val saveLabel: String = MasterEditorSampleData.SAVE_LABEL,
    val saveTopLabel: String = MasterEditorSampleData.SAVE_TOP_LABEL
)

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
