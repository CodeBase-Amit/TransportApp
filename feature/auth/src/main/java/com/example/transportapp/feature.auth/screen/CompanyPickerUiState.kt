package com.example.transportapp.feature.auth.screen

/**
 * T2 — Company and branch picker UI models. Migrated to real data in Phase2 S2: companies and
 * invitations flow from CompanyRepository; these data classes are the UiState's own view models.
 */
data class CompanyRow(
    val membershipLocalId: String,
    val initials: String,
    val name: String,
    val roleLine: String,
    val branches: List<String>,
    val activeBranch: String? = null,
    val series: String? = null,
    val isSelected: Boolean = false,
)

data class Invitation(
    val membershipLocalId: String,
    val companyName: String,
    val invitedBy: String,
    val role: String,
    val expiresIn: String,
)

data class CompanyPickerUiState(
    val title: String = "Your companies",
    val companiesHeading: String = "Companies you work at",
    val invitationsHeading: String = "Invitations",
    val companies: List<CompanyRow> = emptyList(),
    val invitations: List<Invitation> = emptyList(),
    val selectedIndex: Int = 0,
    val selectedBranch: String = "Indore",
    val workSection: String = "WORKING AT",
    val biltySeriesLabel: String = "Bilty series ",
    val openPrefix: String = "Open ",
    val acceptLabel: String = "Accept",
    val declineLabel: String = "Decline",
    val registerLabel: String = "Register a new company",
    val isLoading: Boolean = false,
    val error: String? = null,
    /** S27: set once signOut() commits; the screen navigates to Splash only after it. */
    val signedOut: Boolean = false,
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
