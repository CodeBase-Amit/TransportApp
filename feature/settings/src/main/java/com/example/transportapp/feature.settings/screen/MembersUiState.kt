package com.example.transportapp.feature.settings.screen

import com.example.transportapp.core.ui.sample.MemberRow
import com.example.transportapp.core.ui.sample.MembersSampleData
import com.example.transportapp.core.ui.sample.RoleMatrixRow

data class MembersUiState(
    val title: String = MembersSampleData.TITLE,
    val activeTab: String = MembersSampleData.ACTIVE_TAB,
    val invitedTab: String = MembersSampleData.INVITED_TAB,
    val inviteAction: String = MembersSampleData.INVITE_ACTION,
    val roleMatrixNote: String = MembersSampleData.ROLE_MATRIX_NOTE,
    val roleColumns: List<String> = MembersSampleData.roleColumns,
    val roleMatrix: List<RoleMatrixRow> = MembersSampleData.roleMatrix,
    val members: List<MemberRow> = MembersSampleData.members,
    val showRoleMatrix: Boolean = false
)

sealed interface MembersEvent {
    data object Invite : MembersEvent
    data object ToggleRoleMatrix : MembersEvent
    data object Resend : MembersEvent
}
