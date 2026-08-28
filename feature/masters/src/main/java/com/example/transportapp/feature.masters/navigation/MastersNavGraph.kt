package com.example.transportapp.feature.masters.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.feature.masters.screen.MasterEditorScreen
import com.example.transportapp.feature.masters.screen.MasterListScreen
import com.example.transportapp.feature.masters.screen.MastersHubScreen
import com.example.transportapp.feature.masters.screen.RateCardEditorScreen

fun NavGraphBuilder.mastersNavGraph(navController: NavController) {
    composable(Routes.MASTERS_HUB) {
        MastersHubScreen(
            onBack = { navController.popBackStack() },
            onMasterClick = { type -> navController.navigate(Routes.masterList(type)) }
        )
    }
    composable(
        route = Routes.MASTER_LIST,
        arguments = listOf(navArgument("type") { type = NavType.StringType })
    ) { entry ->
        val type = entry.arguments?.getString("type") ?: "parties"
        MasterListScreen(
            masterType = type,
            onBack = { navController.popBackStack() },
            onRowClick = { navController.navigate(Routes.masterEditor(type, "1")) }
        )
    }
    composable(
        route = Routes.MASTER_EDITOR,
        arguments = listOf(
            navArgument("type") { type = NavType.StringType },
            navArgument("id") { type = NavType.StringType }
        )
    ) {
        MasterEditorScreen(masterType = "party", onBack = { navController.popBackStack() })
    }
    composable(Routes.RATE_CARD_EDITOR) {
        RateCardEditorScreen(onBack = { navController.popBackStack() })
    }
}