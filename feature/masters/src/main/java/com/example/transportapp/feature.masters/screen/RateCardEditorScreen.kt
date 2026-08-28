package com.example.transportapp.feature.masters.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.AppTextButton
import com.example.transportapp.core.designsystem.component.ContentCard
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PlexMonoFamily
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.ui.sample.SampleData

/**
 * T20 — Rate card editor. Resolution order + rate table + charges.
 */
@Composable
fun RateCardEditorScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = "Rate card", onNavigationClick = onBack, trailingIcons = {})
        Text(
            "Deepak Steel Traders · Rate card 2026-27 · 12 rates",
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.screenPadding)
        )

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing)
        ) {
            // Resolution order
            GroupHeading("How a rate is chosen")
            ContentCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "1 · Party + route + goods",
                        "2 · Party + route",
                        "3 · Route + goods",
                        "4 · Route",
                        "5 · Company default"
                    ).forEachIndexed { i, line ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(24.dp)
                                    .background(if (i < 4) MaterialTheme.colorScheme.outlineVariant else Color.Transparent)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(line, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Text(
                    "The booking form takes the first one that exists. Add a narrower rate to override a wider one.",
                    style = TransportTypeScale.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            // Rate table
            GroupHeading("Rates · 12", trailing = { AppTextButton("Add rate", onClick = {}) })
            ContentCard(modifier = Modifier.fillMaxWidth()) {
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.horizontalScroll(scrollState)) {
                    Row(Modifier.width(600.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(vertical = 8.dp)) {
                        listOf("Route", "Goods", "Basis", "Rate", "Min").forEach {
                            Text(it, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(100.dp))
                        }
                    }
                    SampleData.rateRows.forEach { rate ->
                        Row(Modifier.width(600.dp).padding(vertical = 8.dp)) {
                            Text(rate.route, style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(100.dp))
                            Text(rate.goods, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(80.dp))
                            Text(rate.basis, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(80.dp))
                            Text(rate.rate, style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(60.dp))
                            Text(rate.min, style = TransportTypeScale.dataSmall, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(80.dp))
                        }
                        rate.note?.let { note ->
                            Text(note, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 100.dp))
                        }
                    }
                }
            }

            // Charges
            GroupHeading("Charges that apply automatically")
            ContentCard {
                listOf(
                    "Hamali · 8.00 per package" to true,
                    "Door delivery · 150.00 fixed" to true,
                    "Statistical charge · 20.00 fixed" to true,
                    "Loading · 0.00" to false,
                    "Demurrage · 50.00 per day after 3 days" to false
                ).forEach { (label, on) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = TransportTypeScale.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text(if (on) "ON" else "OFF", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("Switched-on charges appear on the booking form already filled. The clerk can remove one, but never has to add it.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }

        // Sticky bar
        AppPrimaryButton("Save rate card", onClick = {}, modifier = Modifier.fillMaxWidth().padding(16.dp))
    }
}