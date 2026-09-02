package com.example.transportapp.core.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

/**
 * S17 — bottom-nav tab switching (TransportApp.md §6.2): tabs never stack on each other,
 * each tab's state survives switching, and re-tapping the active tab does nothing.
 * [popUpTo] with saveState keeps every tab's back stack; [restoreState] brings it back.
 */
fun NavController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(0) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Maps a drawer selection to its route — plain navigate (push) for hub screens, which
 * the user backs out of; [navigateTab] for the three tab roots.
 */
fun NavController.navigateDrawerDestination(destination: DrawerDestination) {
    val route = when (destination) {
        DrawerDestination.HOME -> Routes.DASHBOARD
        DrawerDestination.REGISTER -> Routes.REGISTER
        DrawerDestination.VEHICLES -> Routes.VEHICLE_BOARD
        DrawerDestination.REPORTS -> Routes.REPORTS_HUB
        DrawerDestination.MASTERS -> Routes.MASTERS_HUB
        DrawerDestination.EXPORTS -> Routes.EXPORT_CENTRE
        DrawerDestination.SETTINGS -> Routes.SETTINGS_HUB
        DrawerDestination.ACCOUNT_DATA -> Routes.ACCOUNT_DATA
    }
    if (drawerIsTab(destination)) navigateTab(route) else navigate(route)
}
