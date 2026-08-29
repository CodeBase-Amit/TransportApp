package com.example.transportapp.feature.settings.screen

import com.example.transportapp.core.ui.sample.CompanyProfileSampleData

data class CompanyProfileUiState(
    val title: String = CompanyProfileSampleData.TITLE,
    val previewHeading: String = CompanyProfileSampleData.PREVIEW_HEADING,
    val identityHeading: String = CompanyProfileSampleData.IDENTITY_HEADING,
    val addressHeading: String = CompanyProfileSampleData.ADDRESS_HEADING,
    val taxHeading: String = CompanyProfileSampleData.TAX_HEADING,
    val contactHeading: String = CompanyProfileSampleData.CONTACT_HEADING,
    val logoHeading: String = CompanyProfileSampleData.LOGO_HEADING,
    val footerHeading: String = CompanyProfileSampleData.FOOTER_HEADING,
    val legalName: String = CompanyProfileSampleData.LEGAL_NAME,
    val tradeName: String = CompanyProfileSampleData.TRADE_NAME,
    val constitution: String = CompanyProfileSampleData.CONSTITUTION,
    val constitutionOptions: List<String> = CompanyProfileSampleData.CONSTITUTIONS,
    val address: String = CompanyProfileSampleData.ADDRESS,
    val city: String = CompanyProfileSampleData.CITY,
    val pincode: String = CompanyProfileSampleData.PINCODE,
    val state: String = CompanyProfileSampleData.STATE,
    val gstin: String = CompanyProfileSampleData.GSTIN,
    val pan: String = CompanyProfileSampleData.PAN,
    val transporterId: String = CompanyProfileSampleData.TRANSPORTER_ID,
    val phone: String = CompanyProfileSampleData.PHONE,
    val altPhone: String = CompanyProfileSampleData.ALT_PHONE,
    val email: String = CompanyProfileSampleData.EMAIL,
    val website: String = CompanyProfileSampleData.WEBSITE,
    val footerClause: String = CompanyProfileSampleData.FOOTER_CLAUSE,
    val templateNote: String = CompanyProfileSampleData.TEMPLATE_NOTE,
    val saveLabel: String = CompanyProfileSampleData.SAVE,
    val saveAndUpdate: String = CompanyProfileSampleData.SAVE_AND_UPDATE,
    val deleteTitle: String = CompanyProfileSampleData.DELETE_TITLE,
    val deleteBody: String = CompanyProfileSampleData.DELETE_BODY,
    val logoNote: String = CompanyProfileSampleData.LOGO_NOTE
)

sealed interface CompanyProfileEvent {
    data class ChangeLegalName(val value: String) : CompanyProfileEvent
    data class ChangeTradeName(val value: String) : CompanyProfileEvent
    data class ChangeConstitution(val value: String) : CompanyProfileEvent
    data class ChangeAddress(val value: String) : CompanyProfileEvent
    data class ChangeCity(val value: String) : CompanyProfileEvent
    data class ChangePincode(val value: String) : CompanyProfileEvent
    data class ChangeState(val value: String) : CompanyProfileEvent
    data class ChangeGstin(val value: String) : CompanyProfileEvent
    data class ChangePan(val value: String) : CompanyProfileEvent
    data class ChangeTransporterId(val value: String) : CompanyProfileEvent
    data class ChangePhone(val value: String) : CompanyProfileEvent
    data class ChangeAltPhone(val value: String) : CompanyProfileEvent
    data class ChangeEmail(val value: String) : CompanyProfileEvent
    data class ChangeWebsite(val value: String) : CompanyProfileEvent
    data class ChangeFooter(val value: String) : CompanyProfileEvent
    data object Save : CompanyProfileEvent
    data class RequestDelete(val value: String) : CompanyProfileEvent
}
