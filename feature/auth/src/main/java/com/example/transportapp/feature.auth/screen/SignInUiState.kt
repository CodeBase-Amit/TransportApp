package com.example.transportapp.feature.auth.screen

import androidx.compose.runtime.Stable
data class Reassurance(
    val title: String,
    val body: String
)

/**
 * T1 sign-in copy (S18): static UI copy inline — the sample singleton is gone (§5).
 */
@Stable
data class SignInUiState(
    val title: String = "Book a bilty in under a minute",
    val body: String = "Four printed copies, a live register, and every challan and freight bill built from the same form.",
    val initials: String = "SR",
    val reassurances: List<Reassurance> = listOf(
        Reassurance("Works offline", "Bilties save on the phone and sync when there's signal"),
        Reassurance("Only your staff see your data", "Each company's register is separate and private"),
        Reassurance("Prints on your own letterhead", "Real A4 output, sharp on paper and as PDF")
    ),
    val googleLabel: String = "Continue with Google",
    val googleLoadingLabel: String = "Signing in…",
    val termsIntro: String = "By continuing you agree to our ",
    val conjunction: String = " and ",
    val termsLabel: String = "Terms",
    val privacyLabel: String = "Privacy Policy",
    val loading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SignInEvent {
    data object ContinueWithGoogle : SignInEvent
    data object Terms : SignInEvent
    data object Privacy : SignInEvent
}
