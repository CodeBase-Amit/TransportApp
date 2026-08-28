package com.example.transportapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.feature.auth.navigation.authNavGraph
import com.example.transportapp.feature.billing.navigation.billingNavGraph
import com.example.transportapp.feature.booking.navigation.bookingNavGraph
import com.example.transportapp.feature.challan.navigation.challanNavGraph
import com.example.transportapp.feature.consignment.navigation.consignmentNavGraph
import com.example.transportapp.feature.dashboard.navigation.dashboardNavGraph
import com.example.transportapp.feature.masters.navigation.mastersNavGraph
import com.example.transportapp.feature.reports.navigation.reportsNavGraph
import com.example.transportapp.feature.settings.navigation.settingsNavGraph
import com.example.transportapp.feature.templates.navigation.templatesNavGraph

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.SCREEN_INDEX
    ) {
        // Dev / verification screen map — reachable from the Dashboard
        composable(Routes.SCREEN_INDEX) {
            ScreenIndexScreen(
                onBack = { navController.popBackStack() },
                navController = navController
            )
        }
        // Auth & onboarding
        authNavGraph(navController)
        // Main
        dashboardNavGraph(navController)
        bookingNavGraph(navController)
        consignmentNavGraph(navController)
        challanNavGraph(navController)
        // Money
        billingNavGraph(navController)
        // Masters
        mastersNavGraph(navController)
        // Reports
        reportsNavGraph(navController)
        // Settings & admin
        settingsNavGraph(navController)
        templatesNavGraph(navController)
    }
}
