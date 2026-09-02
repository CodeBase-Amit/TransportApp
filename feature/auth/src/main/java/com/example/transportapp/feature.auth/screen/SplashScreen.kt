package com.example.transportapp.feature.auth.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.RouteLine
import com.example.transportapp.core.designsystem.component.RouteLineStep
import com.example.transportapp.core.designsystem.component.StepState
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

/**
 * T0 — Splash and session resolver.
 * 4-step route line: session, company+branch, templates, sync.
 */
@Composable
fun SplashScreen(
    onResolved: (SplashDestination) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.phase, state.stepIndex, state.destination) {
        if (state.phase == SplashPhase.RESOLVING && state.stepIndex == 3) {
            onResolved(state.destination)
        }
    }

    SplashContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun SplashContent(
    state: SplashUiState,
    onEvent: (SplashEvent) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        when (state.phase) {
            SplashPhase.RESOLVING -> ResolvingFrame(state)
            SplashPhase.FORCED_UPDATE -> ForcedUpdateFrame(state)
            SplashPhase.RESOLVE_FAILED -> ResolveFailedFrame(state, onContinueOffline = { onEvent(SplashEvent.ContinueOffline) })
        }
    }
}

@Composable
private fun ResolvingFrame(state: SplashUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppMark()
        Spacer(Modifier.height(20.dp))
        Text(state.company, style = TransportTypeScale.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(state.subtitle, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(40.dp))
        // 4-tick route line, no labels — just the current step name below
        val steps = List(4) { i ->
            RouteLineStep("", when {
                i < state.stepIndex -> StepState.DONE
                i == state.stepIndex -> StepState.CURRENT
                else -> StepState.UPCOMING
            })
        }
        RouteLine(
            steps = steps,
            showLabels = false,
            showTruck = true,
            modifier = Modifier.width(200.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(state.stepName, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ForcedUpdateFrame(state: SplashUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppMark()
        Spacer(Modifier.height(24.dp))
        Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(16.dp))
        Text(state.forcedUpdateTitle, style = TransportTypeScale.headlineSmall, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            state.forcedUpdateBody,
            style = TransportTypeScale.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        AppPrimaryButton(state.forcedUpdateAction, onClick = {}, modifier = Modifier.fillMaxSize())
        AppTextButton(state.forcedUpdateNote, onClick = {})
    }
}

@Composable
private fun ResolveFailedFrame(state: SplashUiState, onContinueOffline: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppMark()
        Spacer(Modifier.height(24.dp))
        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(16.dp))
        Text(state.failedTitle, style = TransportTypeScale.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(state.failedBody, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        AppPrimaryButton(state.failedAction, onClick = onContinueOffline, modifier = Modifier.fillMaxSize())
        AppTextButton(state.failedRetry, onClick = onContinueOffline)
    }
}

@Composable
private fun AppMark() {
    Box(
        modifier = Modifier.size(88.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(44.dp))
    }
}
