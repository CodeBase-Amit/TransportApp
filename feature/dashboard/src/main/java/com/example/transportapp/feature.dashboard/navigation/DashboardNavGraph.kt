package com.example.transportapp.feature.dashboard.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.core.ui.navigateTab
import com.example.transportapp.feature.dashboard.screen.DashboardScreen

fun NavGraphBuilder.dashboardNavGraph(navController: NavController) {
    composable(Routes.DASHBOARD) {
        DashboardScreen(
            onNewBilty = { navController.navigate(Routes.BOOKING_FORM) },
            onRegister = { navController.navigateTab(Routes.REGISTER) },
            onVehicles = { navController.navigateTab(Routes.VEHICLE_BOARD) },
            onReports = { navController.navigate(Routes.REPORTS_HUB) },
            onMasters = { navController.navigate(Routes.MASTERS_HUB) },
            onExports = { navController.navigate(Routes.EXPORT_CENTRE) },
            onSettings = { navController.navigate(Routes.SETTINGS_HUB) },
            onAccountData = { navController.navigate(Routes.ACCOUNT_DATA) },
            onUnbilled = { navController.navigate(Routes.UNBILLED_POOL) },
            onException = { biltyNo -> navController.navigate(Routes.caseFile(biltyNo)) },
        )
    }
}
