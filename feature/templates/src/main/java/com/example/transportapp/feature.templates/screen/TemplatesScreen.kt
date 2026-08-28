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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.FilterChip
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PaperColors
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.ui.sample.SampleData

@Composable
fun TemplatesScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = "Templates", onNavigationClick = onBack)
        Text("A template decides what a printed document looks like. Documents already issued keep the version they were printed with.", style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Dimens.screenPadding))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp)) {
            FilterChip("All 5", selected = true, onClick = {})
            FilterChip("Bilty 2", selected = false, onClick = {})
            FilterChip("Challan 1", selected = false, onClick = {})
            FilterChip("Bill 1", selected = false, onClick = {})
            FilterChip("Receipt 1", selected = false, onClick = {})
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SampleData.templates.forEach { template ->
                TemplateCard(template)
            }
        }
    }
}

@Composable
private fun TemplateCard(template: SampleData.TemplateRow) {
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
                Text("In use · ${template.inUse}", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (template.neverPrinted) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)).padding(8.dp)
            ) {
                Text("Never printed. Preview it before making it the default.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}