package com.example.transportapp.feature.auth.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.transport.company.CompanyRepository
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.domain.transport.org.MembershipScope
import com.example.transportapp.domain.transport.org.MembershipStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * T2 — reads real org rows from Room (Phase2.md S2). Selection persists the active
 * company/branch so T4's header follows; invitations accept/decline write MEMBERSHIP_E
 * plus an outbox row.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CompanyPickerViewModel @Inject constructor(
    private val companyRepository: CompanyRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanyPickerUiState())
    val uiState: StateFlow<CompanyPickerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.session
                .map { it.email }
                .distinctUntilChanged()
                .flatMapLatest { email ->
                    combine(
                        companyRepository.observeCompanies(),
                        companyRepository.observeAllBranches(),
                        companyRepository.observeMemberCounts(),
                        companyRepository.observeMembershipsForUser(email),
                    ) { companies, allBranches, memberCounts, memberships ->
                        buildRows(companies.map { it.localId to it }, allBranches, memberCounts, memberships)
                    }
                }
                .collect { rows -> _uiState.update { it.copy(companies = rows.companies, invitations = rows.invitations, isLoading = false) } }
        }
    }

    private data class Rows(val companies: List<CompanyRow>, val invitations: List<Invitation>)

    private fun buildRows(
        companies: List<Pair<String, com.example.transportapp.domain.transport.org.CompanySummary>>,
        allBranches: List<com.example.transportapp.domain.transport.org.BranchSummary>,
        memberCounts: Map<String, Int>,
        memberships: List<com.example.transportapp.domain.transport.org.MembershipSummary>,
    ): Rows {
        val activeMemberships = memberships.filter { it.status == MembershipStatus.ACTIVE }
        val companyRows = activeMemberships.map { membership ->
            val company = companies.firstOrNull { it.first == membership.companyId }?.second
            val branches = allBranches.filter { it.companyId == membership.companyId }
            val memberCount = memberCounts[membership.companyId] ?: 1
            val roleLine = when {
                branches.size > 1 && memberCount > 1 ->
                    "${membership.role.label} · ${branches.size} branches · $memberCount members"
                branches.size > 1 -> "${membership.role.label} · ${branches.size} branches"
                membership.branchScope == MembershipScope.ALL ->
                    "${membership.role.label} · ${branches.firstOrNull()?.name ?: "All branches"}"
                else -> {
                    val branchName = branches.firstOrNull { it.localId == membership.branchScope }?.name
                        ?: branches.firstOrNull()?.name.orEmpty()
                    "${membership.role.label} · $branchName only"
                }
            }
            CompanyRow(
                membershipLocalId = membership.localId,
                initials = initialsOf(company?.name ?: membership.companyName),
                name = company?.name ?: membership.companyName,
                roleLine = roleLine,
                branches = branches.map { it.name },
                series = company?.displayBiltySeries,
            )
        }
        val invitations = memberships
            .filter { it.status == MembershipStatus.INVITED }
            .map { membership ->
                Invitation(
                    membershipLocalId = membership.localId,
                    companyName = membership.companyName,
                    invitedBy = membership.invitedBy.orEmpty(),
                    role = membership.role.label,
                    expiresIn = membership.displayExpires.orEmpty(),
                )
            }
        return Rows(companyRows, invitations)
    }

    private fun initialsOf(name: String): String =
        name.split(" ", limit = 3).filter { it.isNotBlank() }.map { it.first().uppercaseChar() }
            .take(2).joinToString("").ifEmpty { "?" }

    fun onEvent(event: CompanyPickerEvent) {
        when (event) {
            is CompanyPickerEvent.SelectCompany -> _uiState.update {
                val branch = it.companies.getOrNull(event.index)?.branches?.firstOrNull() ?: it.selectedBranch
                it.copy(selectedIndex = event.index, selectedBranch = branch)
            }
            is CompanyPickerEvent.SelectBranch -> _uiState.update { it.copy(selectedBranch = event.branch) }
            is CompanyPickerEvent.OpenCompany -> {
                val row = _uiState.value.companies.getOrNull(event.index) ?: return
                viewModelScope.launch {
                    companyRepository.selectCompanyAndBranch(
                        membershipLocalId = row.membershipLocalId,
                        branchLocalId = branchLocalIdFor(row, _uiState.value.selectedBranch),
                    )
                }
            }
            is CompanyPickerEvent.AcceptInvitation -> {
                val invite = _uiState.value.invitations.getOrNull(event.index) ?: return
                viewModelScope.launch { companyRepository.setInvitationAccepted(invite.membershipLocalId) }
            }
            is CompanyPickerEvent.DeclineInvitation -> {
                val invite = _uiState.value.invitations.getOrNull(event.index) ?: return
                viewModelScope.launch { companyRepository.setInvitationDeclined(invite.membershipLocalId) }
            }
            CompanyPickerEvent.RegisterCompany -> Unit // navigation callback
            CompanyPickerEvent.SignOut -> viewModelScope.launch { sessionRepository.signOut() }
        }
    }

    private suspend fun branchLocalIdFor(row: CompanyRow, branchName: String): String? {
        val company = companyRepository.observeCompanies().first()
            .firstOrNull { it.name == row.name } ?: return null
        return companyRepository.getBranchesForCompany(company.localId)
            .firstOrNull { it.name == branchName }?.localId
    }
}
