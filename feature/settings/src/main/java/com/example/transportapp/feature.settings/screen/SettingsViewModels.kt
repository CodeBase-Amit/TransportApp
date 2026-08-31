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
            settingsRepository.branches().collect { branches ->
                _uiState.update { it.copy(branches = branches.map { b -> BranchRow(b.name, b.isHeadOffice, b.address ?: "—", 0, 0, "0.00", emptyList()) }) }
            }
        }
    }

    fun onEvent(event: BranchesEvent) {
        when (event) {
            BranchesEvent.AddBranch -> _uiState.update { it }
            BranchesEvent.BranchMore -> _uiState.update { it }
        }
    }
}

@HiltViewModel
class MembersViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MembersUiState())
    val uiState: StateFlow<MembersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val selfEmail = sessionRepository.session.first().email
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
            MembersEvent.Invite -> _uiState.update { it }
            MembersEvent.ToggleRoleMatrix -> _uiState.update { it.copy(showRoleMatrix = !it.showRoleMatrix) }
            MembersEvent.Resend -> _uiState.update { it }
        }
    }
}

@HiltViewModel
class NumberingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NumberingUiState())
    val uiState: StateFlow<NumberingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.series().collect { series ->
                _uiState.update { state ->
                    state.copy(
                        series = series.map { s ->
                            val used = s.lastIssued > 0
                            SeriesRow(
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
            NumberingEvent.Edit -> _uiState.update { it }
            NumberingEvent.SeriesMore -> _uiState.update { it }
        }
    }
}
