package com.example.transportapp.feature.auth.screen

enum class SplashPhase { RESOLVING, FORCED_UPDATE, RESOLVE_FAILED }

data class SplashUiState(
    val phase: SplashPhase = SplashPhase.RESOLVING,
    val stepName: String = "Checking session",
    val stepIndex: Int = 0,
    val company: String = "",
    val subtitle: String = "Runs the whole document-and-money trail from a phone.",
    val forcedUpdateTitle: String = "Update required",
    val forcedUpdateBody: String = "This version can no longer reach the service. Update to continue.",
    val forcedUpdateAction: String = "Update now",
    val forcedUpdateNote: String = "You can keep working offline until then.",
    val failedTitle: String = "Couldn't resolve your session",
    val failedBody: String = "The local data didn't open. Nothing is lost — try again.",
    val failedAction: String = "Continue offline",
    val failedRetry: String = "Retry"
)

sealed interface SplashEvent {
    data object ContinueOffline : SplashEvent
    data object Retry : SplashEvent
    data object UpdateNow : SplashEvent
}
