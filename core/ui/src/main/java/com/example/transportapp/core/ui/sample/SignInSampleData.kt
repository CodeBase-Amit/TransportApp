package com.example.transportapp.core.ui.sample

data class Reassurance(
    val title: String,
    val body: String
)

/**
 * T1 Sign in demo data. UiState defaults all come from here so the screen
 * stays stateless and the Content composable never touches sample data.
 */
object SignInSampleData {

    const val TITLE = "Book a bilty in under a minute"
    const val BODY = "Four printed copies, a live register, and every challan and freight bill built from the same form."
    const val INITIALS = "SR"

    const val GOOGLE_LABEL = "Continue with Google"
    const val GOOGLE_LOADING = "Signing in…"

    const val TERMS_INTRO = "By continuing you agree to our "
    const val CONJUNCTION = " and "
    const val TERMS = "Terms"
    const val PRIVACY = "Privacy Policy"

    val REASSURANCES = listOf(
        Reassurance("Works offline", "Bilties save on the phone and sync when there's signal"),
        Reassurance("Only your staff see your data", "Each company's register is separate and private"),
        Reassurance("Prints on your own letterhead", "Real A4 output, sharp on paper and as PDF")
    )
}
