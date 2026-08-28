package com.example.transportapp.feature.consignment.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.feature.consignment.screen.CaseFileScreen
import com.example.transportapp.feature.consignment.screen.RegisterScreen
import com.example.transportapp.feature.consignment.screen.StatusUpdateSheet

fun NavGraphBuilder.consignmentNavGraph(navController: NavController) {
    composable(Routes.REGISTER) {
        RegisterScreen(
            onBack = { navController.popBackStack() },
            onDocketClick = { biltyNo -> navController.navigate(Routes.caseFile(biltyNo)) },
            onNewBilty = { navController.navigate(Routes.BOOKING_FORM) },
            onHome = { navController.navigate(Routes.DASHBOARD) },
            onVehicles = { navController.navigate(Routes.VEHICLE_BOARD) }
        )
    }
    composable(
        route = Routes.CASE_FILE,
        arguments = listOf(navArgument("biltyNo") { type = NavType.StringType })
    ) { entry ->
        val biltyNo = entry.arguments?.getString("biltyNo") ?: "IND/2627/04188"
        CaseFileScreen(
            biltyNo = biltyNo,
            onBack = { navController.popBackStack() },
            onUpdateStatus = { navController.navigate(Routes.statusSheet(biltyNo)) }
        )
    }
    composable(Routes.STATUS_SHEET) {
        StatusUpdateSheet(
            biltyNo = "IND/2627/04188",
            onDismiss = { navController.popBackStack() },
            onSave = { navController.popBackStack() }
        )
    }
}