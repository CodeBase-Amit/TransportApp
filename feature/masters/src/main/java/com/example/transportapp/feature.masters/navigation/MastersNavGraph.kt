package com.example.transportapp.feature.masters.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.feature.masters.screen.MasterEditorScreen
import com.example.transportapp.feature.masters.screen.MasterListScreen
import com.example.transportapp.feature.masters.screen.MastersHubScreen
import com.example.transportapp.feature.masters.screen.RateCardEditorScreen

fun NavGraphBuilder.mastersNavGraph(navController: NavController) {
    composable(Routes.MASTERS_HUB) {
        MastersHubScreen(
            onBack = { navController.popBackStack() },
            onMasterClick = { type ->
                // Rate cards are party-scoped (§3): the hub row opens the demo party's card.
                if (type.equals("Rate cards", ignoreCase = true)) {
                    navController.navigate(Routes.rateCardEditor(SeedIds.PARTY_DEEPAK_STEEL))
                } else {
                    navController.navigate(Routes.masterList(type))
                }
            }
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
            onRowClick = { partyId -> navController.navigate(Routes.masterEditor(type, partyId)) },
            onAddParty = { navController.navigate(Routes.masterEditor(type, "new")) }
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
    composable(
        route = Routes.RATE_CARD_EDITOR,
        arguments = listOf(navArgument("partyId") { type = NavType.StringType })
    ) {
        RateCardEditorScreen(onBack = { navController.popBackStack() })
    }
}
