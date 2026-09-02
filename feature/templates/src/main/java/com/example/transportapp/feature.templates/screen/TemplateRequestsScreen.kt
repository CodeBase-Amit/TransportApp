package com.example.transportapp.feature.templates.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppOutlinedButton
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.RouteLine
import com.example.transportapp.core.designsystem.component.RouteLineStep
import com.example.transportapp.core.designsystem.component.StepState
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors

@Composable
fun TemplateRequestsScreen(
    onBack: () -> Unit,
    viewModel: TemplateRequestsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    TemplateRequestsContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@Composable
fun TemplateRequestsContent(
    state: TemplateRequestsUiState,
    onEvent: (TemplateRequestsEvent) -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TransportTopAppBar(title = state.title, onNavigationClick = onBack)

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(state.openHeading, style = TransportTypeScale.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.openRequests.forEach { request ->
                        OpenRequestCard(request, state, onEvent)
                    }
                }
                item {
                    Text(state.pastHeading, style = TransportTypeScale.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp)) {
                        state.pastRequests.forEach { req ->
                            PastRequestRow(req)
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            AppPrimaryButton(state.newRequest, onClick = { onEvent(TemplateRequestsEvent.OpenCapture) }, leadingIcon = Icons.Rounded.Add)
        }
    }

    if (state.showCapture) {
        CaptureSheet(state, onEvent)
    }
}

@Composable
private fun OpenRequestCard(request: TemplateRequest, state: TemplateRequestsUiState, onEvent: (TemplateRequestsEvent) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(request.id, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Box(modifier = Modifier.background(transportColors().haulAmberContainer, RoundedCornerShape(percent = 100)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text(request.status, style = TransportTypeScale.labelMedium, color = transportColors().onHaulAmber)
            }
        }
        Text(request.description, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(12.dp))
        val steps = state.stepLabels.mapIndexed { i, label ->
            RouteLineStep(
                label,
                when {
                    i < request.step -> StepState.DONE
                    i == request.step -> StepState.CURRENT
                    else -> StepState.UPCOMING
                }
            )
        }
        RouteLine(steps = steps, modifier = Modifier.fillMaxWidth())
        Text("Sent ${request.sentDate} · quoted ${request.quotedDate}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().background(transportColors().haulAmberContainer, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${request.amountNote.orEmpty()} ${request.quotedAmount.orEmpty()}", style = TransportTypeScale.dataMedium, color = transportColors().onHaulAmber)
            Spacer(Modifier.width(8.dp))
            Text("one-time · 3 working days", style = TransportTypeScale.bodySmall, color = transportColors().onHaulAmber, modifier = Modifier.weight(1f))
            AppPrimaryButton(state.approvePay, onClick = { onEvent(TemplateRequestsEvent.ApprovePay) }, height = 40.dp)
        }
        Text(state.seePreview, style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun PastRequestRow(req: PastRequest) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(req.id, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(req.description, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(req.date, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(req.amount, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.background(if (req.status == "INSTALLED") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(percent = 100)).padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text(req.status, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CaptureSheet(state: TemplateRequestsUiState, onEvent: (TemplateRequestsEvent) -> Unit) {
    Dialog(onDismissRequest = { onEvent(TemplateRequestsEvent.CloseCapture) }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.captureTitle, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(state.captureStep, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { onEvent(TemplateRequestsEvent.CloseCapture) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(state.captureBody, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f), RoundedCornerShape(percent = 100))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(state.captureWarning, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                AppOutlinedButton(state.captureRetake, onClick = { onEvent(TemplateRequestsEvent.Retake) }, modifier = Modifier.weight(1f))
                AppOutlinedButton(state.captureAddPhoto, onClick = { onEvent(TemplateRequestsEvent.AddPhoto) }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            AppPrimaryButton(state.captureSend, onClick = { onEvent(TemplateRequestsEvent.SendForChecking) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TemplateRequestsPreview() {
    TransportAppTheme {
        TemplateRequestsContent(state = TemplateRequestsUiState(), onEvent = {}, onBack = {})
    }
}
