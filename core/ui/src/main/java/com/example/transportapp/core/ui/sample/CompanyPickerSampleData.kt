package com.example.transportapp.core.ui.sample

data class CompanyRow(
    val initials: String,
    val name: String,
    val roleLine: String,
    val branches: List<String>,
    val activeBranch: String? = null,
    val series: String = "IND/2627 · next number 04189",
    val isSelected: Boolean = false
)

data class Invitation(
    val companyName: String,
    val invitedBy: String,
    val role: String,
    val expiresIn: String
)

/**
 * T2 Company picker demo data. UiState defaults all come from here so the
 * screen stays stateless and the Content composable never touches sample data.
 */
object CompanyPickerSampleData {

    const val TITLE = "Your companies"
    const val COMPANIES_HEADING = "Companies you work at"
    const val INVITATIONS_HEADING = "Invitations"

    const val BRANCH_SECTION = "WORKING AT"
    const val BILTY_SERIES_LABEL = "Bilty series "

    const val OPEN_PREFIX = "Open "
    const val ACCEPT = "Accept"
    const val DECLINE = "Decline"
    const val REGISTER = "Register a new company"

    val COMPANIES = listOf(
        CompanyRow("SR", "Shivshakti Roadlines", "Owner · 3 branches · 4 members", listOf("Indore", "Nagpur", "Bhiwandi"), "Indore", isSelected = true),
        CompanyRow("BC", "Bharat Cargo Carriers", "Booking Clerk · Nagpur only", emptyList())
    )

    val INVITATIONS = listOf(
        Invitation("Malwa Goods Transport", "sunita.jain@gmail.com", "Accountant", "5 days")
    )
}
