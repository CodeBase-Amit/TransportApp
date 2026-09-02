package com.example.transportapp.feature.auth.screen

data class NotificationSetting(
    val label: String,
    val on: Boolean
)

/**
 * T33 profile (S18): static labels inline; the identity block (name/email/role) is
 * populated from the live session by the ViewModel — the sample singleton is gone (§5).
 */
data class ProfileUiState(
    val title: String = "Your profile",
    val saveLabel: String = "Save",
    val name: String = "",
    val email: String = "",
    val initials: String = "…",
    val roleLine: String = "",
    val howYouAppear: String = "How you appear",
    val displayName: String = "",
    val phone: String = "",
    val howAppBehaves: String = "How the app behaves",
    val languageLabel: String = "Language",
    val languageOptions: List<Pair<String, String>> = listOf("English" to "English", "Hindi" to "हिन्दी"),
    val language: String = "English",
    val defaultBranchLabel: String = "Default branch",
    val defaultBranch: String = "",
    val openOnLaunchLabel: String = "Open the booking form on launch",
    val openOnLaunchCaption: String = "For clerks who only book. Skips the dashboard.",
    val openOnLaunch: Boolean = false,
    val clearSignal: Int = 0,
    val signatureHeading: String = "Delivery signature",
    val clearLabel: String = "Clear",
    val redrawLabel: String = "Redraw",
    val signatureCaption: String = "Printed in the receiver's box on the POD copy. Draw it once.",
    val notifyHeading: String = "Notify me about",
    val notifications: List<NotificationSetting> = listOf(
        NotificationSetting("A consignment I booked is held", true),
        NotificationSetting("A vehicle is late", true),
        NotificationSetting("A freight bill I raised is paid", false)
    ),
    val signOutLabel: String = "Sign out of this phone",
    val signOutCaption: String = "Data on this device will be kept"
) {
    /** Shown when the ViewModel hasn't spoken yet; never in a migrated screen. */
    val isLoadingIdentity: Boolean get() = email.isEmpty()
}

sealed interface ProfileEvent {
    data object Save : ProfileEvent
    data object Clear : ProfileEvent
    data object Redraw : ProfileEvent
    data class ChangeLanguage(val language: String) : ProfileEvent
    data object ToggleOpenOnLaunch : ProfileEvent
    data class ToggleNotification(val index: Int) : ProfileEvent
    data object SignOut : ProfileEvent
}
