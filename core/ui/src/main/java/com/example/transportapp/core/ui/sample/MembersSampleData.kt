package com.example.transportapp.core.ui.sample

data class MemberRow(
    val name: String,
    val email: String,
    val scope: String,
    val role: String,
    val isSelf: Boolean = false,
    val invited: Boolean = false,
    val invitedBy: String? = null,
    val invitedRole: String? = null,
    val invitedDate: String? = null
)

data class RoleMatrixRow(val capability: String, val marks: List<Boolean>)

/**
 * T27 Members demo data. UiState defaults all come from here so the screen
 * stays stateless and the Content composable never touches sample data.
 */
object MembersSampleData {

    const val TITLE = "Members"
    const val ACTIVE_TAB = "Active · 4"
    const val INVITED_TAB = "Invited · 1"
    const val INVITE_ACTION = "Invite someone"
    const val ROLE_MATRIX_NOTE = "Roles define access levels across the platform. Assign them carefully."

    val roleColumns = listOf("Capability", "Own", "Mgr", "Acc", "Book", "Del")

    val roleMatrix: List<RoleMatrixRow> = listOf(
        RoleMatrixRow("Book Bilty", listOf(true, true, false, true, false)),
        RoleMatrixRow("Edit Settings", listOf(true, false, false, false, false)),
        RoleMatrixRow("View Dockets", listOf(true, true, true, true, true)),
        RoleMatrixRow("Update Status", listOf(true, true, false, true, true))
    )

    val members: List<MemberRow> = listOf(
        MemberRow("Mahesh Patidar", "mahesh@shivshaktiroadlines.in", "All branches", "Owner", isSelf = true),
        MemberRow("Sunita Jain", "sunita@shivshaktiroadlines.in", "All branches", "Manager"),
        MemberRow("Ramesh Yadav", "ramesh.y@shivshaktiroadlines.in", "Indore only", "Booking Clerk"),
        MemberRow("Iqbal Shaikh", "iqbal.s@shivshaktiroadlines.in", "Nagpur only", "Delivery"),
        MemberRow(
            "accounts.bhiwandi@gmail.com", "accounts.bhiwandi@gmail.com", "Bhiwandi only", "Accountant",
            invited = true, invitedBy = "Mahesh Patidar", invitedDate = "22 Aug"
        )
    )
}
