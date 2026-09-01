package com.example.transportapp.feature.settings.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * T24 — settings hub (Phase 3 S16): the identity block reads the live session; the group
 * rows carry real counts from the same seed data every other screen shows.
 */
@HiltViewModel
class SettingsHubViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsHubUiState())
    val uiState: StateFlow<SettingsHubUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val s = sessionRepository.session.first()
            val branches = settingsRepository.branches().first()
            val members = settingsRepository.members().first()
            val series = settingsRepository.series().first()
            _uiState.update { state ->
                state.copy(
                    identityInitials = s.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""),
                    identityName = s.name,
                    identityEmail = s.email,
                    identityRole = s.role.lowercase().replace('_', ' ')
                        .replaceFirstChar { it.uppercase() } + " · " + s.branchName,
                    groups = listOf(
                        SettingsGroup("YOUR COMPANY", listOf(
                            SettingsRow("building", "Company profile", s.companyName),
                            SettingsRow("location", "Branches", "${branches.size} branches"),
                            SettingsRow("person", "Members and roles", "${members.size} members"),
                            SettingsRow("numbers", "Numbering series", "${series.size} series"),
                        )),
                        SettingsGroup("DOCUMENTS", listOf(
                            SettingsRow("description", "Templates", "Built-in · v1"),
                            SettingsRow("request", "Template requests", "No requests raised"),
                        )),
                        SettingsGroup("ABOUT", listOf(
                            SettingsRow("info", "Version", "Phase 3 · S16"),
                        )),
                    ),
                )
            }
        }
    }
fun onEvent(event: SettingsHubEvent) {
        when (event) {
            SettingsHubEvent.SignOut -> _uiState.update { it }
            is SettingsHubEvent.RowClick -> _uiState.update { it }
        }
    }
}
