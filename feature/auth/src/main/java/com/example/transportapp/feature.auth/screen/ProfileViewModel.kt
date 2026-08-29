package com.example.transportapp.feature.auth.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.Save, ProfileEvent.SignOut -> Unit
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
