package com.example.transportapp.feature.dashboard.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.feature.dashboard.screen.DashboardScreen

fun NavGraphBuilder.dashboardNavGraph(navController: NavController) {
    composable(Routes.DASHBOARD) {
        DashboardScreen(
            onNewBilty = { navController.navigate(Routes.BOOKING_FORM) },
            onRegister = { navController.navigate(Routes.REGISTER) },
            onVehicles = { navController.navigate(Routes.VEHICLE_BOARD) },
            onOpenScreenMap = { navController.navigate(Routes.SCREEN_INDEX) }
        )
    }
}