package com.example.transportapp.feature.auth.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.ErrorBanner
import com.example.transportapp.core.designsystem.theme.AppShapes
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.HaulMotion
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
    // Staggered entrance animation — premium first impression
    var show by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        show = 1f
    }
    val alpha by animateFloatAsState(show, HaulMotion.enterFloat(), label = "signInAlpha")
    val slideY by animateFloatAsState(show, HaulMotion.enterFloat(), label = "signInSlide")
    val translationY = (1f - slideY) * 40f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(Dimens.screenPadding)
            .graphicsLayer { this.alpha = alpha; this.translationY = translationY }
    ) {
        Spacer(Modifier.height(48.dp))

        // Brand mark — premium rounded treatment with subtle surface outline
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(AppShapes.contentCard)
                .background(MaterialTheme.colorScheme.primary, AppShapes.contentCard)
                .padding(horizontal = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                state.initials,
                style = TransportTypeScale.displaySmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(Modifier.height(Dimens.sectionSpacing))
        Text(
            state.title,
            style = TransportTypeScale.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Dimens.fieldGap))
        Text(
            state.body,
            style = TransportTypeScale.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(Dimens.sectionSpacing))

        // Reassurance cards — tonal surface with icon badges
        Column(
            modifier = Modifier
                .clip(AppShapes.contentCard)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, AppShapes.contentCard)
                .padding(Dimens.cardPaddingStandard),
            verticalArrangement = Arrangement.spacedBy(Dimens.fieldGap)
        ) {
            state.reassurances.forEachIndexed { index, reassurance ->
                val icon = when (index % 3) {
                    0 -> Icons.Rounded.CloudOff
                    1 -> Icons.Rounded.Lock
                    else -> Icons.Rounded.Print
                }
                ReassuranceRow(icon, reassurance.title, reassurance.body)
            }
        }

        Spacer(Modifier.weight(1f))

        if (state.errorMessage != null) {
            ErrorBanner(
                message = state.errorMessage,
                modifier = Modifier.padding(bottom = Dimens.fieldGap)
            )
        }

        // Google sign-in button — premium outlined style
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable {
                    onEvent(SignInEvent.ContinueWithGoogle)
                    onSignedIn()
                }
                .clip(RoundedCornerShape(percent = 100))
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(percent = 100))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(percent = 100))
                .padding(horizontal = Dimens.screenPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Google G mark placeholder
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(percent = 100))
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (state.loading) state.googleLoadingLabel else state.googleLabel,
                    style = TransportTypeScale.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(Dimens.fieldGap))

        // Terms & privacy row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                state.termsIntro,
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AppTextButton(state.termsLabel, onClick = onTerms)
            Text(
                state.conjunction,
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AppTextButton(state.privacyLabel, onClick = onPrivacy)
        }
    }
}

@Composable
private fun ReassuranceRow(icon: ImageVector, title: String, body: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.fieldGap)
    ) {
        // Icon badge — primary container circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(percent = 100)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(title, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
