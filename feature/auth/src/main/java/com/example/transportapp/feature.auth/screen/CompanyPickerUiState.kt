package com.example.transportapp.feature.auth.screen

import com.example.transportapp.core.ui.sample.CompanyPickerSampleData
import com.example.transportapp.core.ui.sample.CompanyRow
import com.example.transportapp.core.ui.sample.Invitation

data class CompanyPickerUiState(
    val title: String = CompanyPickerSampleData.TITLE,
    val companiesHeading: String = CompanyPickerSampleData.COMPANIES_HEADING,
    val invitationsHeading: String = CompanyPickerSampleData.INVITATIONS_HEADING,
    val companies: List<CompanyRow> = CompanyPickerSampleData.COMPANIES,
    val invitations: List<Invitation> = CompanyPickerSampleData.INVITATIONS,
    val selectedIndex: Int = 0,
    val selectedBranch: String = "Indore",
    val workSection: String = CompanyPickerSampleData.BRANCH_SECTION,
    val biltySeriesLabel: String = CompanyPickerSampleData.BILTY_SERIES_LABEL,
    val openPrefix: String = CompanyPickerSampleData.OPEN_PREFIX,
    val acceptLabel: String = CompanyPickerSampleData.ACCEPT,
    val declineLabel: String = CompanyPickerSampleData.DECLINE,
    val registerLabel: String = CompanyPickerSampleData.REGISTER
)

sealed interface CompanyPickerEvent {
    data class SelectCompany(val index: Int) : CompanyPickerEvent
    data class SelectBranch(val branch: String) : CompanyPickerEvent
    data class OpenCompany(val index: Int) : CompanyPickerEvent
    data class AcceptInvitation(val index: Int) : CompanyPickerEvent
    data class DeclineInvitation(val index: Int) : CompanyPickerEvent
    data object RegisterCompany : CompanyPickerEvent
    data object SignOut : CompanyPickerEvent
}
