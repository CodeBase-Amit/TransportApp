package com.example.transportapp.feature.templates.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.feature.templates.screen.TemplateRequestsScreen
import com.example.transportapp.feature.templates.screen.TemplatesScreen

fun NavGraphBuilder.templatesNavGraph(navController: NavController) {
    composable(Routes.TEMPLATES) {
        TemplatesScreen(onBack = { navController.popBackStack() })
    }
    composable(Routes.TEMPLATE_REQUESTS) {
        TemplateRequestsScreen(onBack = { navController.popBackStack() })
    }
}