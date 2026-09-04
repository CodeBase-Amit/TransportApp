package com.example.transportapp.feature.challan.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.core.ui.navigateTab
import com.example.transportapp.feature.challan.screen.ChallanBuilderScreen
import com.example.transportapp.feature.challan.screen.ChallanDetailScreen
import com.example.transportapp.feature.challan.screen.VehicleBoardScreen

fun NavGraphBuilder.challanNavGraph(navController: NavController) {
    composable(Routes.CHALLAN_BUILDER) {
        ChallanBuilderScreen(
            onBack = { navController.popBackStack() },
            onCreate = {},
            onOpenTrip = { challanNo ->
                if (challanNo.isNotEmpty()) navController.navigate(Routes.challanDetail(challanNo))
            }
        )
    }
    composable(Routes.CHALLAN_DETAIL) {
        ChallanDetailScreen(
            onBack = { navController.popBackStack() },
            onCloseTrip = { navController.popBackStack() },
            onEditLoad = { navController.navigate(Routes.CHALLAN_BUILDER) }
        )
    }
    composable(Routes.VEHICLE_BOARD) {
        VehicleBoardScreen(
            onNewChallan = { navController.navigate(Routes.CHALLAN_BUILDER) },
            onHome = { navController.navigateTab(Routes.DASHBOARD) },
            onRegister = { navController.navigateTab(Routes.REGISTER) },
            onReports = { navController.navigate(Routes.REPORTS_HUB) },
            onMasters = { navController.navigate(Routes.MASTERS_HUB) },
            onExports = { navController.navigate(Routes.EXPORT_CENTRE) },
            onSettings = { navController.navigate(Routes.SETTINGS_HUB) },
            onAccountData = { navController.navigate(Routes.ACCOUNT_DATA) }
        )
    }
}
