package com.example.transportapp.feature.billing.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.feature.billing.screen.FreightBillScreen
import com.example.transportapp.feature.billing.screen.PaymentsScreen
import com.example.transportapp.feature.billing.screen.StatementScreen
import com.example.transportapp.feature.billing.screen.UnbilledPoolScreen

fun NavGraphBuilder.billingNavGraph(navController: NavController) {
    composable(Routes.UNBILLED_POOL) {
        UnbilledPoolScreen(
            onBack = { navController.popBackStack() },
            onBuildBill = { navController.navigate(Routes.FREIGHT_BILL) }
        )
    }
    composable(Routes.FREIGHT_BILL) {
        FreightBillScreen(onBack = { navController.popBackStack() })
    }
    composable(Routes.PAYMENTS) {
        PaymentsScreen(onBack = { navController.popBackStack() })
    }
    composable(
        route = Routes.STATEMENT,
        arguments = listOf(navArgument("partyId") { type = NavType.StringType })
    ) { entry ->
        val partyId = entry.arguments?.getString("partyId") ?: ""
        StatementScreen(partyId = partyId, onBack = { navController.popBackStack() })
    }
}