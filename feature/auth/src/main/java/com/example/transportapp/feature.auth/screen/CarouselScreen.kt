package com.example.transportapp.feature.auth.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.PaymentStamp
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.ui.sample.CarouselPanel
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
    val state by viewModel.uiState.collectAsState()
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
            // Page dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(state.panels.size) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == state.currentPage) 24.dp else 8.dp, 8.dp)
                            .background(
                                if (i == state.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape
                            )
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
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val stamp = panel.stamp
        if (stamp != null) {
            Box(
                modifier = Modifier.size(280.dp).height(280.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)),
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
                modifier = Modifier.size(280.dp).height(280.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(panel.emoji ?: "", style = TransportTypeScale.displaySmall)
            }
        }
        Spacer(Modifier.height(40.dp))
        if (panel.title.isNotEmpty()) {
            Text(panel.title, style = TransportTypeScale.headlineMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(panel.body, style = TransportTypeScale.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
