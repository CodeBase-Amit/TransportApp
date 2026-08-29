package com.example.transportapp.core.ui.sample

data class NotificationSetting(
    val label: String,
    val on: Boolean
)

/**
 * T33 Your profile demo data. UiState defaults all come from here so the
 * screen stays stateless and the Content composable never touches sample data.
 */
object ProfileSampleData {

    const val TITLE = "Your profile"
    const val SAVE = "Save"

    const val NAME = "Mahesh Patidar"
    const val EMAIL = "mahesh.patidar@gmail.com"
    const val INITIALS = "MP"
    const val ROLE_LINE = "Owner · Shivshakti Roadlines"

    const val HOW_YOU_APPEAR = "How you appear"
    const val DISPLAY_NAME = "Mahesh Patidar"
    const val PHONE = "+91 94250 61183"

    const val HOW_APP_BEHAVES = "How the app behaves"
    const val LANGUAGE_LABEL = "Language"
    val LANGUAGE_OPTIONS = listOf("English" to "English", "Hindi" to "हिन्दी")
    const val DEFAULT_LANGUAGE = "English"
    const val DEFAULT_BRANCH_LABEL = "Default branch"
    const val DEFAULT_BRANCH = "Indore"
    const val OPEN_ON_LAUNCH_LABEL = "Open the booking form on launch"
    const val OPEN_ON_LAUNCH_CAPTION = "For clerks who only book. Skips the dashboard."

    const val SIGNATURE_HEADING = "Delivery signature"
    const val CLEAR = "Clear"
    const val REDRAW = "Redraw"
    const val SIGNATURE_CAPTION = "Printed in the receiver's box on the POD copy. Draw it once."

    const val NOTIFY_HEADING = "Notify me about"
    val NOTIFICATIONS = listOf(
        NotificationSetting("A consignment I booked is held", true),
        NotificationSetting("A vehicle is late", true),
        NotificationSetting("A freight bill I raised is paid", false)
    )

    const val SIGN_OUT = "Sign out of this phone"
    const val SIGN_OUT_CAPTION = "3 changes haven't synced yet. Sign out will wait for them."
}
