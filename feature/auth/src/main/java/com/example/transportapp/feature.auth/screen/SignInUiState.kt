package com.example.transportapp.feature.auth.screen

import com.example.transportapp.core.ui.sample.Reassurance
import com.example.transportapp.core.ui.sample.SignInSampleData

data class SignInUiState(
    val title: String = SignInSampleData.TITLE,
    val body: String = SignInSampleData.BODY,
    val initials: String = SignInSampleData.INITIALS,
    val reassurances: List<Reassurance> = SignInSampleData.REASSURANCES,
    val googleLabel: String = SignInSampleData.GOOGLE_LABEL,
    val googleLoadingLabel: String = SignInSampleData.GOOGLE_LOADING,
    val termsIntro: String = SignInSampleData.TERMS_INTRO,
    val conjunction: String = SignInSampleData.CONJUNCTION,
    val termsLabel: String = SignInSampleData.TERMS,
    val privacyLabel: String = SignInSampleData.PRIVACY,
    val loading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SignInEvent {
    data object ContinueWithGoogle : SignInEvent
    data object Terms : SignInEvent
    data object Privacy : SignInEvent
}
