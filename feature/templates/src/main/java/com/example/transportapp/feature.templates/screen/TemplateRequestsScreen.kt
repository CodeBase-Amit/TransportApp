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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.RouteLine
import com.example.transportapp.core.designsystem.component.RouteLineStep
import com.example.transportapp.core.designsystem.component.StepState
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.core.ui.sample.SampleData

@Composable
fun TemplateRequestsScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
        TransportTopAppBar(title = "Template requests", onNavigationClick = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("OPEN REQUESTS", style = TransportTypeScale.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("TR-2026-0037", style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.background(transportColors().haulAmberContainer, RoundedCornerShape(percent = 100)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("QUOTED", style = TransportTypeScale.labelMedium, color = transportColors().onHaulAmber)
                        }
                    }
                    Text("Bilty · 4 copies · from your own book", style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(12.dp))
                    // 5-tick route line
                    val steps = listOf(
                        RouteLineStep("Sent", StepState.DONE),
                        RouteLineStep("Checked", StepState.DONE),
                        RouteLineStep("Quoted", StepState.CURRENT),
                        RouteLineStep("Building", StepState.UPCOMING),
                        RouteLineStep("Installed", StepState.UPCOMING)
                    )
                    RouteLine(steps = steps, modifier = Modifier.fillMaxWidth())
                    Text("Sent 20 Aug · quoted 22 Aug", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().background(transportColors().haulAmberContainer, RoundedCornerShape(12.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("2,500.00", style = TransportTypeScale.dataMedium, color = transportColors().onHaulAmber)
                        Spacer(Modifier.width(8.dp))
                        Text("one-time · 3 working days", style = TransportTypeScale.bodySmall, color = transportColors().onHaulAmber, modifier = Modifier.weight(1f))
                        AppPrimaryButton("Approve and pay", onClick = {}, height = 40.dp)
                    }
                    Text("See what they'll build", style = TransportTypeScale.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
                }
            }
            item {
                Text("PAST REQUESTS", style = TransportTypeScale.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp)) {
                    SampleData.pastRequests.forEach { req ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(req.id, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(req.description, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(req.date, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box(modifier = Modifier.background(if (req.status == "INSTALLED") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(percent = 100)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text(req.status, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            AppPrimaryButton("New request", onClick = {}, leadingIcon = Icons.Rounded.Add)
        }
    }
}