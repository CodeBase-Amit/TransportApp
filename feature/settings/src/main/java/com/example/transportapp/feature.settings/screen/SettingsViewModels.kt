package com.example.transportapp.feature.settings.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.common.Money
import com.example.transportapp.data.transport.account.SettingsRepository
import com.example.transportapp.data.transport.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * T26/T27/T28 (Phase2.md S10): branches, members and numbering series read the live org
 * tables; the branch cards aggregate their own counts.
 */
@HiltViewModel
class BranchesViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BranchesUiState())
    val uiState: StateFlow<BranchesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionRepository.session.first()
            _uiState.update { it.copy(canManage = session.role == "OWNER" || session.role == "MANAGER") }
            settingsRepository.branches().collect { branches ->
                _uiState.update { it.copy(branches = branches.map { b -> BranchRow(b.name, b.isHeadOffice, b.address ?: "—", 0, 0, "0.00", emptyList()) }) }
            }
        }
    }

    fun onEvent(event: BranchesEvent) {
        when (event) {
            // S21: the real add-branch dialog (was a no-op).
            BranchesEvent.AddBranch -> _uiState.update { if (it.canManage) it.copy(showAddBranch = true, error = null) else it }
            BranchesEvent.DismissAddBranch -> _uiState.update { it.copy(showAddBranch = false, branchName = "", branchCode = "", branchAddress = "", error = null) }
            is BranchesEvent.ChangeBranchName -> _uiState.update { it.copy(branchName = event.value) }
            is BranchesEvent.ChangeBranchCode -> _uiState.update { it.copy(branchCode = event.value.uppercase()) }
            is BranchesEvent.ChangeBranchAddress -> _uiState.update { it.copy(branchAddress = event.value) }
            BranchesEvent.SaveBranch -> viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, error = null) }
                val session = sessionRepository.session.first()
                val result = settingsRepository.addBranch(session.companyId, _uiState.value.branchName, _uiState.value.branchCode, _uiState.value.branchAddress)
                when (result) {
                    is com.example.transportapp.core.common.Result.Success ->
                        _uiState.update { it.copy(isSaving = false, showAddBranch = false, branchName = "", branchCode = "", branchAddress = "") }
                    is com.example.transportapp.core.common.Result.Failure ->
                        _uiState.update { it.copy(isSaving = false, error = result.message ?: result.code.name) }
                }
            }
        }
    }
}

@HiltViewModel
class MembersViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    // S19: the invite dialog's fields.
    private val _inviteEmail = MutableStateFlow("")
    val inviteEmail: StateFlow<String> = _inviteEmail.asStateFlow()
    private val _inviteRole = MutableStateFlow("BOOKING_CLERK")
    val inviteRole: StateFlow<String> = _inviteRole.asStateFlow()

    private val _uiState = MutableStateFlow(MembersUiState())
    val uiState: StateFlow<MembersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionRepository.session.first()
            _uiState.update { it.copy(isOwner = session.role == "OWNER") }
            val selfEmail = session.email
            settingsRepository.members().collect { members ->
                _uiState.update { state ->
                    val rows = members.map { it.toRow(selfEmail) }
                    state.copy(
                        members = rows,
                        activeTab = "Active · ${rows.count { !it.invited }}",
                        invitedTab = "Invited · ${rows.count { it.invited }}",
                    )
                }
            }
        }
    }

    fun onEvent(event: MembersEvent) {
        when (event) {
            // S19: the invite button opens the dialog (was a dead no-op).
            MembersEvent.Invite -> _uiState.update { if (it.isOwner) it.copy(showInvite = true, inviteError = null) else it }
            MembersEvent.DismissInvite -> _uiState.update { it.copy(showInvite = false, inviteError = null) }.also { _inviteEmail.value = "" }
            is MembersEvent.ChangeInviteEmail -> _inviteEmail.value = event.value
            is MembersEvent.ChangeInviteRole -> _inviteRole.value = event.role
            MembersEvent.SendInvite -> viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, inviteError = null) }
                val session = sessionRepository.session.first()
                val role = _inviteRole.value
                val result = settingsRepository.inviteMember(session.companyId, _inviteEmail.value.trim(), role, session.name)
                when (result) {
                    is com.example.transportapp.core.common.Result.Success ->
                        _uiState.update { it.copy(isSaving = false, showInvite = false) }.also { _inviteEmail.value = "" }
                    is com.example.transportapp.core.common.Result.Failure ->
                        _uiState.update { it.copy(isSaving = false, inviteError = result.message ?: result.code.name) }
                }
            }
            MembersEvent.ToggleRoleMatrix -> _uiState.update { it.copy(showRoleMatrix = !it.showRoleMatrix) }
            // S21: the invitation row's X — tombstone + outbox, then the live query drops the row.
            is MembersEvent.CancelInvite -> viewModelScope.launch {
                val session = sessionRepository.session.first()
                val target = settingsRepository.members().first().firstOrNull { it.email == event.memberEmail && it.status == "INVITED" }
                if (target != null) {
                    settingsRepository.cancelInvitationByMail(session.companyId, event.memberEmail)
                }
            }
        }
    }
}

@HiltViewModel
class NumberingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NumberingUiState())
    val uiState: StateFlow<NumberingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionRepository.session.first()
            _uiState.update { it.copy(isOwner = session.role == "OWNER") }
            settingsRepository.series().collect { series ->
                _uiState.update { state ->
                    state.copy(
                        series = series.map { s ->
                            val used = s.lastIssued > 0
                            SeriesRow(
                                localId = s.localId,
                                label = s.docType.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() } + " · " + s.branch,
                                nextNumber = s.prefix + (s.lastIssued + 1).toString().padStart(s.digits, '0'),
                                prefix = s.prefix,
                                fy = s.fyPart,
                                lastUsed = s.lastIssued.toString(),
                                caption = if (used) "issued through ${s.lastIssued}" else "no numbers issued yet",
                                neverUsed = !used,
                            )
                        },
                    )
                }
            }
        }
    }

    fun onEvent(event: NumberingEvent) {
        when (event) {
            is NumberingEvent.StartCounterEdit -> {
                val row = _uiState.value.series.firstOrNull { it.localId == event.seriesLocalId } ?: return
                _uiState.update {
                    it.copy(counterEdit = CounterEditUi(
                        seriesLocalId = row.localId,
                        label = row.label,
                        nextNumber = row.nextNumber,
                        digits = row.nextNumber.substringAfterLast('/').length,
                        typed = "",
                    ))
                }
            }
            is NumberingEvent.ChangeCounter -> _uiState.update { state ->
                state.copy(counterEdit = state.counterEdit?.copy(typed = event.value.filter { ch -> ch.isDigit() }))
            }
            NumberingEvent.ConfirmCounter -> viewModelScope.launch {
                val edit = _uiState.value.counterEdit ?: return@launch
                val newCounter = edit.typed.toLongOrNull() ?: return@launch
                _uiState.update { it.copy(isSaving = true, error = null) }
                val companyId = sessionRepository.session.first().companyId
                // S27: resolve by the series' local id — the old path parsed the branch out
                // of the display label (a branch_id) and every confirm failed.
                val result = settingsRepository.changeSeriesCounterById(companyId, edit.seriesLocalId, newCounter)
                when (result) {
                    is com.example.transportapp.core.common.Result.Success ->
                        _uiState.update { it.copy(isSaving = false, counterEdit = null) }
                    is com.example.transportapp.core.common.Result.Failure ->
                        _uiState.update { it.copy(isSaving = false, error = result.message ?: result.code.name) }
                }
            }
            NumberingEvent.DismissCounterEdit -> _uiState.update { it.copy(counterEdit = null) }
        }
    }
}
