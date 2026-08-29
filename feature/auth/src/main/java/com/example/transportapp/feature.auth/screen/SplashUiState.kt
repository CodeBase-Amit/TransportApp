package com.example.transportapp.feature.auth.screen

import com.example.transportapp.core.ui.sample.SplashSampleData

enum class SplashPhase { RESOLVING, FORCED_UPDATE, RESOLVE_FAILED }

data class SplashUiState(
    val phase: SplashPhase = SplashPhase.RESOLVING,
    val stepName: String = SplashSampleData.RESOLUTION_STEPS.first(),
    val stepIndex: Int = 0,
    val company: String = SplashSampleData.COMPANY,
    val subtitle: String = SplashSampleData.SUBTITLE,
    val forcedUpdateTitle: String = SplashSampleData.FORCED_UPDATE_TITLE,
    val forcedUpdateBody: String = SplashSampleData.FORCED_UPDATE_BODY,
    val forcedUpdateAction: String = SplashSampleData.FORCED_UPDATE_ACTION,
    val forcedUpdateNote: String = SplashSampleData.FORCED_UPDATE_NOTE,
    val failedTitle: String = SplashSampleData.FAILED_TITLE,
    val failedBody: String = SplashSampleData.FAILED_BODY,
    val failedAction: String = SplashSampleData.FAILED_ACTION,
    val failedRetry: String = SplashSampleData.FAILED_RETRY
)

sealed interface SplashEvent {
    data object ContinueOffline : SplashEvent
    data object Retry : SplashEvent
    data object UpdateNow : SplashEvent
}
