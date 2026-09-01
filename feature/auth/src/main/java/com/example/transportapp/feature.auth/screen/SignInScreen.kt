package com.example.transportapp.feature.auth.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.ErrorBanner
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

/**
 * T1 — Sign in. The only screen a signed-out user sees after the carousel.
 */
@Composable
fun SignInScreen(
    onSignedIn: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    SignInContent(state = state, onEvent = viewModel::onEvent, onSignedIn = onSignedIn, onTerms = onTerms, onPrivacy = onPrivacy)
}

@Composable
fun SignInContent(
    state: SignInUiState,
    onEvent: (SignInEvent) -> Unit,
    onSignedIn: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(16.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Box(
            modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(state.initials, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(Modifier.height(24.dp))
        Text(state.title, style = TransportTypeScale.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(
            state.body,
            style = TransportTypeScale.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(40.dp))

        // Three reassurances
        state.reassurances.forEachIndexed { index, reassurance ->
            val icon = when (index % 3) {
                0 -> Icons.Rounded.CloudOff
                1 -> Icons.Rounded.Lock
                else -> Icons.Rounded.Print
            }
            ReassuranceRow(icon, reassurance.title, reassurance.body)
            if (index < state.reassurances.lastIndex) {
                Spacer(Modifier.height(16.dp))
            }
        }

        Spacer(Modifier.weight(1f))

        if (state.errorMessage != null) {
            ErrorBanner(message = state.errorMessage, modifier = Modifier.padding(bottom = 16.dp))
        }

        // Google sign-in button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { onEvent(SignInEvent.ContinueWithGoogle); onSignedIn() }
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(percent = 100))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Google G mark placeholder
                Box(
                    modifier = Modifier.size(20.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(percent = 100))
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (state.loading) state.googleLoadingLabel else state.googleLabel,
                    style = TransportTypeScale.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(state.termsIntro, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppTextButton(state.termsLabel, onClick = onTerms)
            Text(state.conjunction, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppTextButton(state.privacyLabel, onClick = onPrivacy)
        }
    }
}

@Composable
private fun ReassuranceRow(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
