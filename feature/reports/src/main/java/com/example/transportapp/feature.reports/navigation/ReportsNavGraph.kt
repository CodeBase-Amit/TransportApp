package com.example.transportapp.feature.reports.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.feature.reports.screen.ExportCentreScreen
import com.example.transportapp.feature.reports.screen.ReportViewerScreen
import com.example.transportapp.feature.reports.screen.ReportsHubScreen

fun NavGraphBuilder.reportsNavGraph(navController: NavController) {
    composable(Routes.REPORTS_HUB) {
        ReportsHubScreen(
            onBack = { navController.popBackStack() },
            onReportClick = { id -> navController.navigate(Routes.reportViewer(id)) }
        )
    }
    composable(
        route = Routes.REPORT_VIEWER,
        arguments = listOf(navArgument("reportId") { type = NavType.StringType })
    ) {
        ReportViewerScreen(onBack = { navController.popBackStack() })
    }
    composable(Routes.EXPORT_CENTRE) {
        ExportCentreScreen(onBack = { navController.popBackStack() })
    }
}