package com.example.transportapp.feature.settings.screen

import androidx.compose.runtime.Stable
/** One T24 row (Phase 3 S16): identity and counts come from the live session/org data. */
data class SettingsRow(
    val icon: String,
    val label: String,
    val value: String? = null,
    val locked: Boolean = false,
    val gate: String? = null,
    val syncIcon: Boolean = false
)

@Stable
data class SettingsGroup(
    val heading: String,
    val rows: List<SettingsRow>
)

@Stable
data class SettingsHubUiState(
    val title: String = "Settings",
    val identityInitials: String = "…",
    val identityName: String = "",
    val identityEmail: String = "",
    val identityRole: String = "",
    val signOutLabel: String = "Sign out of TransportApp",
    val signOutNote: String = "Data on this device will be kept",
    val groups: List<SettingsGroup> = emptyList()
)

sealed interface SettingsHubEvent {
    data object SignOut : SettingsHubEvent
}
