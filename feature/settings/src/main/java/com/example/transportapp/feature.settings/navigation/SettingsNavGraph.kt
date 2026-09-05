package com.example.transportapp.feature.settings.navigation

import android.content.pm.ApplicationInfo
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
            onRowClick = { label ->
                when (label) {
                    "Company profile" -> navController.navigate(Routes.COMPANY_PROFILE)
                    "Branches" -> navController.navigate(Routes.BRANCHES)
                    "Members and roles" -> navController.navigate(Routes.MEMBERS)
                    "Numbering series" -> navController.navigate(Routes.NUMBERING)
                    "Templates" -> navController.navigate(Routes.TEMPLATES)
                    "Template requests" -> navController.navigate(Routes.TEMPLATE_REQUESTS)
                    "Version" -> navController.navigate(Routes.ACCOUNT_DATA)
                }
            },
            onSignedOut = {
                navController.navigate(Routes.SPLASH) {
                    popUpTo(0) { inclusive = true }
                }
            }
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
        // D53: the dev screen map is reachable only from here, only in debug builds.
        val isDebuggable = navController.context.applicationInfo.flags and
            ApplicationInfo.FLAG_DEBUGGABLE != 0
        AccountDataScreen(
            onBack = { navController.popBackStack() },
            onOpenScreenMap = if (isDebuggable) {
                { navController.navigate(Routes.SCREEN_INDEX) }
            } else null,
            // S27: Section C's sign-out card is live now — same route as T24's sign-out.
            onSignedOut = {
                navController.navigate(Routes.SPLASH) {
                    popUpTo(0) { inclusive = true }
                }
            },
        )
    }
}
