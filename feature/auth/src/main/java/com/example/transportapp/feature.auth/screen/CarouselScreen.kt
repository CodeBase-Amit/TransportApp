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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import kotlinx.coroutines.launch

/**
 * T32 — First-run carousel. Three panels shown once on first install, before sign-in.
 */
@Composable
fun CarouselScreen(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Skip button
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            if (pagerState.currentPage < 2) {
                TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.TopEnd)) {
                    Text("Skip", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            CarouselPanel(page)
        }
        // Bottom area
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == pagerState.currentPage) 24.dp else 8.dp, 8.dp)
                            .background(
                                if (i == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape
                            )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // Button
            if (pagerState.currentPage < 2) {
                AppPrimaryButton(
                    "Next",
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                AppPrimaryButton("Get started", onClick = onGetStarted, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                AppTextButton("I've used this before", onClick = onSkip)
            }
        }
    }
}

@Composable
private fun CarouselPanel(page: Int) {
    val (icon, title, body) = when (page) {
        0 -> Triple("📄", "One form, four copies", "Fill the booking form once. Consignor, consignee, driver and office copies print together, exactly like your GR book.")
        1 -> Triple("📍", "Every bilty in one place", "Booked, loaded, in transit, delivered. Search by bilty number, party or vehicle and see where the goods actually are.")
        else -> Triple("📡", "Works without signal", "Book bilties in the yard or on the highway. Everything saves on the phone and syncs the moment you get a network.")
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier.size(280.dp).height(280.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, style = TransportTypeScale.displaySmall)
        }
        Spacer(Modifier.height(40.dp))
        Text(title, style = TransportTypeScale.headlineMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = TransportTypeScale.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}