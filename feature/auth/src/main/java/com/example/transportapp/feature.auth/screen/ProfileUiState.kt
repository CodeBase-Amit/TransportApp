package com.example.transportapp.feature.auth.screen

import com.example.transportapp.core.ui.sample.NotificationSetting
import com.example.transportapp.core.ui.sample.ProfileSampleData

data class ProfileUiState(
    val title: String = ProfileSampleData.TITLE,
    val saveLabel: String = ProfileSampleData.SAVE,
    val name: String = ProfileSampleData.NAME,
    val email: String = ProfileSampleData.EMAIL,
    val initials: String = ProfileSampleData.INITIALS,
    val roleLine: String = ProfileSampleData.ROLE_LINE,
    val howYouAppear: String = ProfileSampleData.HOW_YOU_APPEAR,
    val displayName: String = ProfileSampleData.DISPLAY_NAME,
    val phone: String = ProfileSampleData.PHONE,
    val howAppBehaves: String = ProfileSampleData.HOW_APP_BEHAVES,
    val languageLabel: String = ProfileSampleData.LANGUAGE_LABEL,
    val languageOptions: List<Pair<String, String>> = ProfileSampleData.LANGUAGE_OPTIONS,
    val language: String = ProfileSampleData.DEFAULT_LANGUAGE,
    val defaultBranchLabel: String = ProfileSampleData.DEFAULT_BRANCH_LABEL,
    val defaultBranch: String = ProfileSampleData.DEFAULT_BRANCH,
    val openOnLaunchLabel: String = ProfileSampleData.OPEN_ON_LAUNCH_LABEL,
    val openOnLaunchCaption: String = ProfileSampleData.OPEN_ON_LAUNCH_CAPTION,
    val openOnLaunch: Boolean = false,
    val clearSignal: Int = 0,
    val signatureHeading: String = ProfileSampleData.SIGNATURE_HEADING,
    val clearLabel: String = ProfileSampleData.CLEAR,
    val redrawLabel: String = ProfileSampleData.REDRAW,
    val signatureCaption: String = ProfileSampleData.SIGNATURE_CAPTION,
    val notifyHeading: String = ProfileSampleData.NOTIFY_HEADING,
    val notifications: List<NotificationSetting> = ProfileSampleData.NOTIFICATIONS,
    val signOutLabel: String = ProfileSampleData.SIGN_OUT,
    val signOutCaption: String = ProfileSampleData.SIGN_OUT_CAPTION
)

sealed interface ProfileEvent {
    data object Save : ProfileEvent
    data object Clear : ProfileEvent
    data object Redraw : ProfileEvent
    data class ChangeLanguage(val language: String) : ProfileEvent
    data object ToggleOpenOnLaunch : ProfileEvent
    data class ToggleNotification(val index: Int) : ProfileEvent
    data object SignOut : ProfileEvent
}
