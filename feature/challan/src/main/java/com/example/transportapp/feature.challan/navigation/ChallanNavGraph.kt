package com.example.transportapp.feature.challan.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.feature.challan.screen.ChallanBuilderScreen
import com.example.transportapp.feature.challan.screen.ChallanDetailScreen
import com.example.transportapp.feature.challan.screen.VehicleBoardScreen

fun NavGraphBuilder.challanNavGraph(navController: NavController) {
    composable(Routes.CHALLAN_BUILDER) {
        ChallanBuilderScreen(
            onBack = { navController.popBackStack() },
            onCreate = { navController.navigate(Routes.CHALLAN_DETAIL) },
            onOpenTrip = {}
        )
    }
    composable(Routes.CHALLAN_DETAIL) {
        ChallanDetailScreen(
            onBack = { navController.popBackStack() },
            onDispatch = {},
            onCloseTrip = { navController.popBackStack() }
        )
    }
    composable(Routes.VEHICLE_BOARD) {
        VehicleBoardScreen(
            onBack = { navController.popBackStack() },
            onNewChallan = { navController.navigate(Routes.CHALLAN_BUILDER) }
        )
    }
}
