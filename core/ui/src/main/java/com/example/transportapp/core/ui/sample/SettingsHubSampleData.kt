package com.example.transportapp.core.ui.sample

data class SettingsGroup(val heading: String, val rows: List<SettingsRow>)
data class SettingsRow(
    val icon: String,
    val label: String,
    val value: String? = null,
    val locked: Boolean = false,
    val gate: String? = null,
    val syncIcon: Boolean = false
)

/**
 * T24 Settings hub demo data. UiState defaults all come from here so the
 * screen stays stateless and the Content composable never touches sample data.
 */
object SettingsHubSampleData {

    const val TITLE = "Settings"

    const val IDENTITY_INITIALS = "MP"
    const val IDENTITY_NAME = "Mahesh Patidar"
    const val IDENTITY_EMAIL = "mahesh@shivshaktiroadlines.in"
    const val IDENTITY_ROLE = "Owner"

    const val SIGN_OUT_LABEL = "Sign out of this phone"
    const val SIGN_OUT_NOTE = "Your 3 unsynced changes will be sent first."

    val groups: List<SettingsGroup> = listOf(
        SettingsGroup("THE COMPANY", listOf(
            SettingsRow("business", "Company profile", "Shivshakti Roadlines"),
            SettingsRow("account_balance", "Branches", "4"),
            SettingsRow("group", "Members", "4 active, 1 invited"),
            SettingsRow("numbers", "Document Series", "6")
        )),
        SettingsGroup("DOCUMENTS", listOf(
            SettingsRow("description", "Templates", "5 installed"),
            SettingsRow("photo_camera", "Template requests", "1 quoted"),
            SettingsRow("print", "Print settings", "(Thermal 3\")"),
            SettingsRow("article", "Terms and conditions", null)
        )),
        SettingsGroup("THIS PHONE", listOf(
            SettingsRow("language", "Language", "English (IN)"),
            SettingsRow("text_fields", "Text size", "System"),
            SettingsRow("dark_mode", "Theme", "System Default"),
            SettingsRow("print", "Printer", "Not set up"),
            SettingsRow("sync", "Data Sync", "Offline")
        )),
        SettingsGroup("ACCOUNT", listOf(
            SettingsRow("folder", "Account and data", null),
            SettingsRow("help", "Help and how-to", null),
            SettingsRow("info", "About", "v1.0.4 (118)")
        ))
    )
}
