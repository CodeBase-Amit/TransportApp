package com.example.transportapp.feature.auth.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.PaymentStamp
import com.example.transportapp.core.designsystem.theme.AppShapes
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.HaulMotion
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import kotlinx.coroutines.flow.collect

/**
 * T32 — First-run carousel. Three panels shown once on first install, before sign-in.
 */
@Composable
fun CarouselScreen(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
    viewModel: CarouselViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CarouselContent(state = state, onEvent = viewModel::onEvent, onGetStarted = onGetStarted, onSkip = onSkip)
}

@Composable
fun CarouselContent(
    state: CarouselUiState,
    onEvent: (CarouselEvent) -> Unit,
    onGetStarted: () -> Unit,
    onSkip: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { state.panels.size })

    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.animateScrollToPage(state.currentPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page -> onEvent(CarouselEvent.SelectPage(page)) }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Skip button
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            if (state.currentPage < state.panels.lastIndex) {
                TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.TopEnd)) {
                    Text(state.skipLabel, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            CarouselPanel(state.panels[page])
        }
        // Bottom area
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page dots — active dot animates to an elongated pill in primary
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(state.panels.size) { i ->
                    val active = i == state.currentPage
                    val width by animateFloatAsState(
                        targetValue = if (active) 24f else 8f,
                        animationSpec = HaulMotion.snappy,
                        label = "dotWidth"
                    )
                    val fill by animateColorAsState(
                        targetValue = if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        animationSpec = HaulMotion.short(),
                        label = "dotFill"
                    )
                    Box(
                        modifier = Modifier
                            .width(width.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(percent = 100))
                            .background(fill)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // Button
            if (state.currentPage < state.panels.lastIndex) {
                AppPrimaryButton(
                    state.nextLabel,
                    onClick = { onEvent(CarouselEvent.Next) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                AppPrimaryButton(state.getStartedLabel, onClick = onGetStarted, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                AppTextButton(state.usedBeforeLabel, onClick = onSkip)
            }
        }
    }
}

@Composable
private fun CarouselPanel(panel: CarouselPanel) {
    // Entrance animation on panel reveal
    var show by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { show = 1f }
    val alpha by animateFloatAsState(show, HaulMotion.enterFloat(), label = "panelAlpha")
    val slideY by animateFloatAsState(show, HaulMotion.enterFloat(), label = "panelSlide")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(top = 48.dp)
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = (1f - slideY) * 24f
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val stamp = panel.stamp
        if (stamp != null) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .height(280.dp)
                    .clip(AppShapes.contentCard)
                    .background(MaterialTheme.colorScheme.primaryContainer, AppShapes.contentCard)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, AppShapes.contentCard),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PaymentStamp(mode = stamp.mode)
                    Spacer(Modifier.height(12.dp))
                    Text(stamp.caption, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .height(280.dp)
                    .clip(AppShapes.contentCard)
                    .background(MaterialTheme.colorScheme.primaryContainer, AppShapes.contentCard)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, AppShapes.contentCard),
                contentAlignment = Alignment.Center
            ) {
                Text(panel.emoji ?: "", style = TransportTypeScale.displaySmall)
            }
        }
        Spacer(Modifier.height(40.dp))
        if (panel.title.isNotEmpty()) {
            Text(
                panel.title,
                style = TransportTypeScale.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                panel.body,
                style = TransportTypeScale.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
