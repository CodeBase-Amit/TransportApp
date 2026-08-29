package com.example.transportapp.feature.settings.screen

import com.example.transportapp.core.ui.sample.SettingsGroup
import com.example.transportapp.core.ui.sample.SettingsHubSampleData

data class SettingsHubUiState(
    val title: String = SettingsHubSampleData.TITLE,
    val identityInitials: String = SettingsHubSampleData.IDENTITY_INITIALS,
    val identityName: String = SettingsHubSampleData.IDENTITY_NAME,
    val identityEmail: String = SettingsHubSampleData.IDENTITY_EMAIL,
    val identityRole: String = SettingsHubSampleData.IDENTITY_ROLE,
    val signOutLabel: String = SettingsHubSampleData.SIGN_OUT_LABEL,
    val signOutNote: String = SettingsHubSampleData.SIGN_OUT_NOTE,
    val groups: List<SettingsGroup> = SettingsHubSampleData.groups
)

sealed interface SettingsHubEvent {
    data object SignOut : SettingsHubEvent
    data class RowClick(val label: String) : SettingsHubEvent
}
