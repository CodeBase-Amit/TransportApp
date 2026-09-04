package com.example.transportapp.feature.auth.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.transport.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * T33 — Your profile (Phase2.md S2). Identity comes from the mocked session
 * (SessionRepository); sign-out clears the local mirror only (§17.4).
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.session.collect { session ->
                _uiState.update {
                    it.copy(
                        name = session.name,
                        email = session.email,
                        roleLine = roleLine(session),
                        displayName = session.name,
                        defaultBranch = session.branchName.ifEmpty { it.defaultBranch },
                    )
                }
            }
        }
    }

    private fun roleLine(session: com.example.transportapp.data.transport.session.UserSession): String {
        val role = runCatching { com.example.transportapp.domain.transport.Role.valueOf(session.role) }
            .getOrDefault(com.example.transportapp.domain.transport.Role.DELIVERY_CLERK)
        return "${role.label} · ${session.companyName}"
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            // S21: Save writes the display name through the session seam (membership
            // name update rides the outbox when the drain lands).
            ProfileEvent.Save -> viewModelScope.launch {
                val name = _uiState.value.displayName.trim()
                if (name.isNotEmpty()) {
                    sessionRepository.updateDisplayName(name)
                    _uiState.update { it.copy(saveNotice = "Saved") }
                }
            }
            ProfileEvent.SignOut -> viewModelScope.launch { sessionRepository.signOut() }
            ProfileEvent.Clear, ProfileEvent.Redraw -> _uiState.update { it.copy(clearSignal = it.clearSignal + 1) }
            is ProfileEvent.ChangeLanguage -> _uiState.update { it.copy(language = event.language) }
            ProfileEvent.ToggleOpenOnLaunch -> _uiState.update { it.copy(openOnLaunch = !it.openOnLaunch) }
            is ProfileEvent.ToggleNotification -> _uiState.update { it ->
                val list = it.notifications.toMutableList()
                list[event.index] = list[event.index].copy(on = !list[event.index].on)
                it.copy(notifications = list)
            }
        }
    }
}
