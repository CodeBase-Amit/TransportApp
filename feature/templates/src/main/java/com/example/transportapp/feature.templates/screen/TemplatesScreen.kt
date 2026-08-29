package com.example.transportapp.feature.templates.screen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.FilterChip
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PaperColors
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.ui.sample.TemplateRow
import com.example.transportapp.core.ui.sample.VersionHistory

@Composable
fun TemplatesScreen(
    onBack: () -> Unit,
    viewModel: TemplatesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    TemplatesContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@Composable
fun TemplatesContent(
    state: TemplatesUiState,
    onEvent: (TemplatesEvent) -> Unit,
    onBack: () -> Unit
) {
    val filters = listOf(
        "All" to "All ${state.templates.size}",
        "Bilty" to "Bilty ${state.templates.count { it.type == "Bilty" }}",
        "Invoice" to "Invoice ${state.templates.count { it.type == "Invoice" }}",
        "Manifest" to "Manifest ${state.templates.count { it.type == "Manifest" }}"
    )
    val visible = if (state.selectedFilter == "All") state.templates else state.templates.filter { it.type == state.selectedFilter }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = state.title, onNavigationClick = onBack)
        Text(state.subtitle, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Dimens.screenPadding))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp)) {
            filters.forEach { (filter, label) ->
                FilterChip(label, selected = state.selectedFilter == filter, onClick = { onEvent(TemplatesEvent.Filter(filter)) })
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            visible.forEach { template ->
                TemplateCard(template)
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                AppTextButton(state.requestTemplate, onClick = { onEvent(TemplatesEvent.RequestTemplate) })
            }

            Text(state.versionHistoryHeading, style = TransportTypeScale.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(20.dp)) {
                state.versionHistory.forEach { version ->
                    VersionHistoryRow(version)
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(template: TemplateRow) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Paper thumbnail
            Box(
                modifier = Modifier.size(width = 96.dp, height = 136.dp).background(PaperColors.paperWhite, RoundedCornerShape(2.dp)).border(1.dp, PaperColors.paperRule, RoundedCornerShape(2.dp)).padding(8.dp)
            ) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(10.dp).background(PaperColors.paperRule.copy(alpha = 0.25f)))
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(PaperColors.paperRule.copy(alpha = 0.25f)))
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(24.dp).background(PaperColors.paperRule.copy(alpha = 0.25f)))
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(20.dp).background(PaperColors.paperRule.copy(alpha = 0.25f)))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(template.name, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (template.isDefault) {
                        Spacer(Modifier.width(4.dp))
                        Box(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(percent = 100)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("DEFAULT", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    if (template.archived) {
                        Spacer(Modifier.width(4.dp))
                        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(percent = 100)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("ARCHIVED", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text("${template.type} · ${template.copies} · ${template.paper}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(percent = 100)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(template.version, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    template.tags.forEach { tag ->
                        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(percent = 100)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(tag, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                if (template.description.isNotEmpty()) {
                    Text(template.description, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
                if (template.neverPrinted) {
                    Text("Never printed. Preview it before making it the default.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                } else if (!template.archived && template.status.isNotEmpty()) {
                    Text("In use · ${template.status}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
            }
            IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun VersionHistoryRow(version: VersionHistory) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(percent = 100)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(version.version, style = TransportTypeScale.dataSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(8.dp))
            Text(version.date, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(version.author, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(version.change, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun TemplatesPreview() {
    TransportAppTheme {
        TemplatesContent(state = TemplatesUiState(), onEvent = {}, onBack = {})
    }
}
