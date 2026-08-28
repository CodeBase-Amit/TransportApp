package com.example.transportapp.feature.consignment.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.DocketRow
import com.example.transportapp.core.designsystem.component.NavDestination
import com.example.transportapp.core.designsystem.component.SearchField
import com.example.transportapp.core.designsystem.component.SummaryStrip
import com.example.transportapp.core.designsystem.component.TransportBottomNavBar
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.ui.sample.SampleData

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onDocketClick: (String) -> Unit,
    onNewBilty: () -> Unit,
    onHome: () -> Unit,
    onVehicles: () -> Unit
) {
    val rows = SampleData.registerRows

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TransportTopAppBar(title = "Register", onNavigationClick = onBack, trailingIcons = {
                IconButton(onClick = {}) { Icon(Icons.Rounded.Tune, contentDescription = "Filter", tint = MaterialTheme.colorScheme.onSurface) }
                IconButton(onClick = {}) { Icon(Icons.Rounded.FileDownload, contentDescription = "Export", tint = MaterialTheme.colorScheme.onSurface) }
            })

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.screenPadding)) {
                SearchField(value = "", onValueChange = {}, placeholder = "Bilty number, party, vehicle or private mark")
            }

            SummaryStrip(
                "MATCHING" to "61",
                "PACKAGES" to "812",
                "FREIGHT" to "2,41,880.00",
                modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(rows) { row ->
                    DocketRow(
                        docNumber = row.docNumber,
                        amount = row.amount,
                        fromStation = row.from,
                        toStation = row.to,
                        consignee = row.consignee,
                        status = row.status,
                        paymentMode = row.paymentMode,
                        packagesCaption = row.caption,
                        exceptionCaption = row.exception,
                        onClick = { onDocketClick(row.docNumber) }
                    )
                }
            }

            // Bottom navigation — Home / Register / Vehicles
            TransportBottomNavBar(
                destinations = listOf(
                    NavDestination("Home", Icons.Outlined.Home, Icons.Rounded.Home),
                    NavDestination("Register", Icons.Outlined.ListAlt, Icons.Outlined.ListAlt),
                    NavDestination("Vehicles", Icons.Outlined.LocalShipping, Icons.Rounded.LocalShipping)
                ),
                activeIndex = 1,
                onSelect = { index ->
                    when (index) {
                        0 -> onHome()
                        2 -> onVehicles()
                    }
                }
            )
        }

        // Extended FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 96.dp)
        ) {
            AppPrimaryButton(
                text = "New bilty",
                onClick = onNewBilty,
                leadingIcon = Icons.Rounded.Add
            )
        }
    }
}