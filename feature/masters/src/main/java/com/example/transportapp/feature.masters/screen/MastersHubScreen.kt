package com.example.transportapp.feature.masters.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.core.ui.sample.SampleData

/**
 * T17 — Masters hub. Nine kinds of reference data with live counts.
 */
@Composable
fun MastersHubScreen(
    onBack: () -> Unit,
    onMasterClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        TransportTopAppBar(title = "Masters", onNavigationClick = onBack)
        Text(
            "Reference data the booking form fills itself from. The better this is, the less anyone types.",
            style = TransportTypeScale.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp)
        )

        SampleData.masterGroups.forEach { group ->
            Spacer(Modifier.height(8.dp))
            GroupHeading(group.heading, modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.screenPadding)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp)
            ) {
                group.rows.forEach { (label, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = TransportTypeScale.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text(count, style = TransportTypeScale.dataMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Duplicate banner
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenPadding)
                .background(transportColors().haulAmberContainer, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Merge, contentDescription = null, tint = transportColors().onHaulAmber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("7 parties look like duplicates of another party.", style = TransportTypeScale.bodyMedium, color = transportColors().onHaulAmber, modifier = Modifier.weight(1f))
            AppTextButton("Review them", onClick = {}, color = transportColors().onHaulAmber)
        }
    }
}