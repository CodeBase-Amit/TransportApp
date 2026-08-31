package com.example.transportapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.transportapp.core.common.SeedIds
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.transportapp.core.designsystem.component.TransportTopAppBar
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.ui.Routes

/**
 * Dev / verification screen index. Lists every screen (T0–T33) so each is reachable
 * by a single tap from the running app, and each screen's back button returns here.
 * Reachable from the Dashboard's person icon.
 */
@Composable
fun ScreenIndexScreen(onBack: () -> Unit, navController: NavController) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TransportTopAppBar(title = "Screen map", onNavigationClick = onBack)
        Text(
            "All 34 screens from Design.md. Tap any row to open it; use its back arrow to return here.",
            style = TransportTypeScale.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.screenPadding, vertical = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            items(screenGroups) { group ->
                Text(
                    group.name.uppercase(),
                    style = TransportTypeScale.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                group.screens.forEach { screen ->
                    IndexRow(screen, navController)
                }
            }
        }
    }
}

@Composable
private fun IndexRow(screen: ScreenEntry, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(screen.route) }
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(screen.id, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(screen.name, style = TransportTypeScale.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (screen.note != null) {
                Text(screen.note, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(6.dp))
}

private data class ScreenEntry(val id: String, val name: String, val route: String, val note: String? = null)
private data class ScreenGroup(val name: String, val screens: List<ScreenEntry>)

private val screenGroups = listOf(
    ScreenGroup("First-run & onboarding", listOf(
        ScreenEntry("T0", "Splash / session resolver", Routes.SPLASH, "auto-resolves"),
        ScreenEntry("T32", "First-run carousel", Routes.CAROUSEL, "3 panels"),
        ScreenEntry("T1", "Sign in", Routes.SIGN_IN),
        ScreenEntry("T2", "Company & branch picker", Routes.COMPANY_PICKER),
        ScreenEntry("T3", "Company setup wizard", Routes.SETUP_WIZARD, "4 steps"),
        ScreenEntry("T33", "Your profile", Routes.PROFILE, "signature pad")
    )),
    ScreenGroup("Home & booking", listOf(
        ScreenEntry("T4", "Dashboard", Routes.DASHBOARD, "the app's home"),
        ScreenEntry("T5", "New booking form", Routes.BOOKING_FORM, "the single form"),
        ScreenEntry("T6", "Bilty preview", Routes.biltyPreview("IND/2627/04188"), "4-copy stack")
    )),
    ScreenGroup("Consignment", listOf(
        ScreenEntry("T7", "Consignment register", Routes.REGISTER, "docket rows"),
        ScreenEntry("T8", "Consignment case file", Routes.caseFile("IND/2627/04188")),
        ScreenEntry("T9", "Status update sheet", Routes.statusSheet("IND/2627/04188"))
    )),
    ScreenGroup("Challan & vehicles", listOf(
        ScreenEntry("T10", "Challan builder", Routes.CHALLAN_BUILDER, "load meter"),
        ScreenEntry("T11", "Challan detail", Routes.challanDetail("CHL/IND/2627/00742")),
        ScreenEntry("T12", "Vehicle & trip board", Routes.VEHICLE_BOARD)
    )),
    ScreenGroup("Money", listOf(
        ScreenEntry("T13", "Unbilled pool", Routes.UNBILLED_POOL),
        ScreenEntry("T14", "Freight bill detail", Routes.freightBill(SeedIds.BILL_00311), "seeded issued bill"),
        ScreenEntry("T15", "Payments & receipts", Routes.PAYMENTS),
        ScreenEntry("T16", "Party statement", Routes.statement(SeedIds.PARTY_DEEPAK_STEEL))
    )),
    ScreenGroup("Masters", listOf(
        ScreenEntry("T17", "Masters hub", Routes.MASTERS_HUB, "9 counts"),
        ScreenEntry("T18", "Master list", Routes.masterList("parties"), "A–Z + merge"),
        ScreenEntry("T19", "Master editor", Routes.masterEditor("party", "1")),
        ScreenEntry("T20", "Rate card editor", Routes.rateCardEditor("deepak"))
    )),
    ScreenGroup("Reports", listOf(
        ScreenEntry("T21", "Reports hub", Routes.REPORTS_HUB, "4 question groups"),
        ScreenEntry("T22", "Report viewer", Routes.reportViewer("freight"), "frozen column"),
        ScreenEntry("T23", "Export centre", Routes.EXPORT_CENTRE, "12 sheets")
    )),
    ScreenGroup("Settings & admin", listOf(
        ScreenEntry("T24", "Settings hub", Routes.SETTINGS_HUB),
        ScreenEntry("T25", "Company profile", Routes.COMPANY_PROFILE, "letterhead"),
        ScreenEntry("T26", "Branches", Routes.BRANCHES),
        ScreenEntry("T27", "Members & roles", Routes.MEMBERS),
        ScreenEntry("T28", "Numbering series", Routes.NUMBERING),
        ScreenEntry("T29", "Templates", Routes.TEMPLATES),
        ScreenEntry("T30", "Template requests", Routes.TEMPLATE_REQUESTS),
        ScreenEntry("T31", "Account & data", Routes.ACCOUNT_DATA)
    ))
)