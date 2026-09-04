package com.example.transportapp.feature.consignment.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.core.ui.navigateTab
import com.example.transportapp.feature.consignment.screen.CaseFileScreen
import com.example.transportapp.feature.consignment.screen.RegisterScreen
import com.example.transportapp.feature.consignment.screen.StatusUpdateSheet

fun NavGraphBuilder.consignmentNavGraph(navController: NavController) {
    composable(Routes.REGISTER) {
        RegisterScreen(
            onDocketClick = { biltyNo -> navController.navigate(Routes.caseFile(biltyNo)) },
            onNewBilty = { navController.navigate(Routes.BOOKING_FORM) },
            onHome = { navController.navigateTab(Routes.DASHBOARD) },
            onVehicles = { navController.navigateTab(Routes.VEHICLE_BOARD) },
            onReports = { navController.navigate(Routes.REPORTS_HUB) },
            onMasters = { navController.navigate(Routes.MASTERS_HUB) },
            onExports = { navController.navigate(Routes.EXPORT_CENTRE) },
            onSettings = { navController.navigate(Routes.SETTINGS_HUB) },
            onAccountData = { navController.navigate(Routes.ACCOUNT_DATA) },
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
            onAddPhoto = {},
            onAmend = { navController.navigate("booking_form?amends=" + android.net.Uri.encode(biltyNo)) },
            onCancel = {},
            onHold = { navController.navigate(Routes.statusSheet(biltyNo)) },
            onRaiseBill = { navController.navigate(Routes.UNBILLED_POOL) },
            onFullHistory = { navController.navigate(Routes.statusSheet(biltyNo)) }
        )
    }
    composable(
        route = Routes.STATUS_SHEET,
        arguments = listOf(navArgument("biltyNo") { type = NavType.StringType })
    ) { entry ->
        val biltyNo = entry.arguments?.getString("biltyNo") ?: "IND/2627/04188"
        StatusUpdateSheet(
            biltyNo = biltyNo,
            onDismiss = { navController.popBackStack() },
            onSave = { navController.popBackStack() }
        )
    }
}