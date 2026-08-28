package com.example.transportapp.feature.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.feature.settings.screen.AccountDataScreen
import com.example.transportapp.feature.settings.screen.BranchesScreen
import com.example.transportapp.feature.settings.screen.CompanyProfileScreen
import com.example.transportapp.feature.settings.screen.MembersScreen
import com.example.transportapp.feature.settings.screen.NumberingScreen
import com.example.transportapp.feature.settings.screen.SettingsHubScreen

fun NavGraphBuilder.settingsNavGraph(navController: NavController) {
    composable(Routes.SETTINGS_HUB) {
        SettingsHubScreen(
            onBack = { navController.popBackStack() },
            onProfile = { navController.navigate(Routes.PROFILE) },
            onRowClick = { }
        )
    }
    composable(Routes.COMPANY_PROFILE) {
        CompanyProfileScreen(onBack = { navController.popBackStack() })
    }
    composable(Routes.BRANCHES) {
        BranchesScreen(onBack = { navController.popBackStack() })
    }
    composable(Routes.MEMBERS) {
        MembersScreen(onBack = { navController.popBackStack() })
    }
    composable(Routes.NUMBERING) {
        NumberingScreen(onBack = { navController.popBackStack() })
    }
    composable(Routes.ACCOUNT_DATA) {
        AccountDataScreen(onBack = { navController.popBackStack() })
    }
}