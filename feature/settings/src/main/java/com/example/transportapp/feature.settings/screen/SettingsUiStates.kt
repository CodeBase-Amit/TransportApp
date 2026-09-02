package com.example.transportapp.feature.settings.screen

import com.example.transportapp.data.transport.account.MemberRowData

/** T26 branch card. */
data class BranchRow(
    val name: String,
    val isHeadOffice: Boolean,
    val address: String,
    val members: Int,
    val openBiltes: Int,
    val toPay: String,
    val series: List<String>,
    val noMembersLine: String? = null,
    val inviteText: String? = null,
)

data class BranchesUiState(
    val title: String = "Branches",
    val subtitle: String = "Your network. Every bilty books at one of these.",
    val addBranch: String = "Add a branch",
    val headOfficeChip: String = "HEAD OFFICE",
    val branches: List<BranchRow> = emptyList(),
)

sealed interface BranchesEvent {
    data object AddBranch : BranchesEvent
    data object BranchMore : BranchesEvent
}

/** T27 member rows (active + invited share one shape). */
data class MemberRow(
    val name: String,
    val email: String,
    val scope: String,
    val role: String,
    val isSelf: Boolean = false,
    val invited: Boolean = false,
    val invitedBy: String? = null,
    val invitedRole: String? = null,
    val invitedDate: String? = null,
)

data class MembersUiState(
    val title: String = "Members",
    val activeTab: String = "Active",
    val invitedTab: String = "Invited",
    val inviteAction: String = "Invite a member",
    val roleMatrixNote: String = "Owners and managers can do everything. Accountants see money. Clerks see their branch.",
    val roleColumns: List<String> = listOf("", "Book", "Money", "All"),
    val roleMatrix: List<RoleMatrixRowUi> = listOf(
        RoleMatrixRowUi("Book a bilty", listOf(true, false, true)),
        RoleMatrixRowUi("Collect To Pay", listOf(true, true, true)),
        RoleMatrixRowUi("Raise a bill", listOf(false, true, true)),
        RoleMatrixRowUi("See receivables", listOf(false, true, true)),
        RoleMatrixRowUi("Manage members", listOf(false, false, true)),
    ),
    val members: List<MemberRow> = emptyList(),
    val showRoleMatrix: Boolean = false,
    // S19 — the invite dialog (Owner only).
    val isOwner: Boolean = false,
    val showInvite: Boolean = false,
    val inviteError: String? = null,
    val isSaving: Boolean = false,
)

data class RoleMatrixRowUi(val capability: String, val marks: List<Boolean>)

sealed interface MembersEvent {
    data object Invite : MembersEvent
    data object ToggleRoleMatrix : MembersEvent
    data object Resend : MembersEvent
    // S19 — the invite dialog
    data object DismissInvite : MembersEvent
    data class ChangeInviteEmail(val value: String) : MembersEvent
    data class ChangeInviteRole(val role: String) : MembersEvent
    data object SendInvite : MembersEvent
}

/** T28 series card. */
data class SeriesRow(
    val localId: String,
    val label: String,
    val nextNumber: String,
    val prefix: String,
    val fy: String,
    val lastUsed: String,
    val caption: String,
    val neverUsed: Boolean = false,
)

/** The §9 counter-change dialog state: typed confirmation, forward-only (S19). */
data class CounterEditUi(
    val seriesLocalId: String,
    val label: String,
    val nextNumber: String,
    val digits: Int,
    val typed: String = "",
) {
    /** The typed value is the new high-water mark; it must be a full-width number. */
    val valid: Boolean get() = typed.length == digits && typed.toLongOrNull() != null
}

data class NumberingUiState(
    val title: String = "Numbering series",
    val subtitle: String = "Numbers are leased per device offline; the counter never rolls back.",
    val editLabel: String = "Edit",
    val series: List<SeriesRow> = emptyList(),
    /** S19: the open counter-change dialog, Owner only. */
    val isOwner: Boolean = false,
    val counterEdit: CounterEditUi? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
)

sealed interface NumberingEvent {
    data class StartCounterEdit(val seriesLocalId: String) : NumberingEvent
    data class ChangeCounter(val value: String) : NumberingEvent
    data object ConfirmCounter : NumberingEvent
    data object DismissCounterEdit : NumberingEvent
    data object SeriesMore : NumberingEvent
}

fun MemberRowData.toRow(selfEmail: String): MemberRow = MemberRow(
    name = name,
    email = email,
    scope = branchScope,
    role = role,
    isSelf = email == selfEmail,
)
