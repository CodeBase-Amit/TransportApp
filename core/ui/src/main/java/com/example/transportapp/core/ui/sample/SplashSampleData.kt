package com.example.transportapp.core.ui.sample

/**
 * T0 Splash demo data. UiState defaults all come from here so the
 * screen stays stateless and the Content composable never touches sample data.
 */
object SplashSampleData {

    const val COMPANY = "Shivshakti Roadlines"
    const val SUBTITLE = "Indore · Bilty and transport register"

    val RESOLUTION_STEPS = listOf(
        "Signing you in",
        "Checking your branch",
        "Loading your bilty format",
        "Syncing 3 changes"
    )

    const val LAST_STEP_INDEX = 3

    const val FORCED_UPDATE_TITLE = "Update TransportApp to continue"
    const val FORCED_UPDATE_BODY = "Your bilty format uses a template this version can't print correctly. Updating takes a moment and your unsynced bilties are safe."
    const val FORCED_UPDATE_ACTION = "Update now"
    const val FORCED_UPDATE_NOTE = "What happens to my saved bilties?"

    const val FAILED_TITLE = "Can't reach the server"
    const val FAILED_BODY = "You can keep booking bilties offline. 3 changes are waiting to sync."
    const val FAILED_ACTION = "Continue offline"
    const val FAILED_RETRY = "Try again"
}
