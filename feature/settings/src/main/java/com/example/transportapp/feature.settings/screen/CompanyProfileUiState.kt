package com.example.transportapp.feature.settings.screen

import androidx.compose.runtime.Stable
/**
 * T25 — company profile (S19): static labels inline (the sample singleton is gone, §5);
 * the editable fields default from the ViewModel's COMPANY_E read, and a half-edited
 * draft survives process death via the ViewModel's SavedStateHandle.
 */
@Stable
data class CompanyProfileUiState(
    val title: String = "Company profile",
    val previewHeading: String = "How it prints",
    val identityHeading: String = "Identity",
    val addressHeading: String = "Address",
    val taxHeading: String = "Tax information",
    val contactHeading: String = "Contact",
    val logoHeading: String = "Logo and signature",
    val footerHeading: String = "Terms footer",
    val legalName: String = "",
    val tradeName: String = "",
    val constitution: String = "Proprietorship",
    val constitutionOptions: List<String> = listOf("Proprietorship", "Partnership", "Pvt Ltd", "LLP"),
    val address: String = "",
    val city: String = "",
    val pincode: String = "",
    val state: String = "",
    val gstin: String = "",
    val pan: String = "",
    val transporterId: String = "",
    val phone: String = "",
    val altPhone: String = "",
    val email: String = "",
    val website: String = "",
    val footerClause: String = "Goods booked at owner's risk. Subject to Indore jurisdiction.",
    val templateNote: String = "Printed by your installed bilty template — change the layout in Templates.",
    val saveLabel: String = "Save",
    val saveAndUpdate: String = "Save and update outbox",
    val deleteTitle: String = "Delete company",
    val deleteBody: String = "Only a sole Owner can delete the company. Issued documents are retained for the statutory period.",
    val logoNote: String = "PNG or JPG, square. It prints 90×90 pt on every document.",
    val saved: Boolean = false,
    val logoRef: String? = null,
    val error: String? = null,
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
    // S22 - the logo picker answered (D60)
    data class LogoPicked(val uri: android.net.Uri) : CompanyProfileEvent
    data class RequestDelete(val value: String) : CompanyProfileEvent
}
