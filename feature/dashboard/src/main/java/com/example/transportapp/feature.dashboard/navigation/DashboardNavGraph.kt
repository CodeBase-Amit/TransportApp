package com.example.transportapp.feature.dashboard.navigation

import android.content.pm.ApplicationInfo
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.feature.dashboard.screen.DashboardScreen

fun NavGraphBuilder.dashboardNavGraph(navController: NavController) {
    composable(Routes.DASHBOARD) {
        // The dev screen-map shortcut is a debug affordance only (the app module decides
        // debuggability; a feature module cannot read the :app build type). The route itself
        // still exists in release — it is just not linked from any screen.
        val isDebuggable = navController.context.applicationInfo.flags and
            ApplicationInfo.FLAG_DEBUGGABLE != 0
        DashboardScreen(
            onNewBilty = { navController.navigate(Routes.BOOKING_FORM) },
            onRegister = { navController.navigate(Routes.REGISTER) },
            onVehicles = { navController.navigate(Routes.VEHICLE_BOARD) },
            onOpenScreenMap = { navController.navigate(Routes.SCREEN_INDEX) },
            showScreenMap = isDebuggable
        )
    }
}