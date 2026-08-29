package com.example.transportapp.core.ui.sample

data class BranchRow(
    val name: String,
    val isHeadOffice: Boolean,
    val address: String,
    val members: Int,
    val openBiltes: Int,
    val toPay: String,
    val series: List<String>,
    val noMembersLine: String? = null,
    val inviteText: String? = null
)

/**
 * T26 Branches demo data. UiState defaults all come from here so the screen
 * stays stateless and the Content composable never touches sample data.
 */
object BranchesSampleData {

    const val TITLE = "Branches"
    const val SUBTITLE = "A branch has its own address, its own number series and its own staff."
    const val ADD_BRANCH = "Add branch"
    const val HEAD_OFFICE_CHIP = "(HO)"

    val rows: List<BranchRow> = listOf(
        BranchRow(
            name = "Indore",
            isHeadOffice = true,
            address = "14, Transport Nagar, AB Road, Indore",
            members = 12,
            openBiltes = 45,
            toPay = "₹12,450",
            series = listOf("IND/2627/*")
        ),
        BranchRow(
            name = "Bhiwandi",
            isHeadOffice = false,
            address = "Godown No 5, Rahnal Village, Bhiwandi",
            members = 0,
            openBiltes = 102,
            toPay = "₹45,200",
            series = listOf("BHW/2627/*"),
            noMembersLine = "No one is assigned here",
            inviteText = "Invite someone"
        ),
        BranchRow(
            name = "Pune",
            isHeadOffice = false,
            address = "Plot 42, Transport Hub, Wakad, Pune",
            members = 4,
            openBiltes = 18,
            toPay = "₹8,900",
            series = listOf("PUN/2627/*")
        )
    )
}
