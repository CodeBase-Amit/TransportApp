package com.example.transportapp.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.feature.auth.screen.CarouselScreen
import com.example.transportapp.feature.auth.screen.CompanyPickerScreen
import com.example.transportapp.feature.auth.screen.ProfileScreen
import com.example.transportapp.feature.auth.screen.SetupWizardScreen
import com.example.transportapp.feature.auth.screen.SplashDestination
import com.example.transportapp.feature.auth.screen.SignInScreen
import com.example.transportapp.feature.auth.screen.SplashScreen

fun NavGraphBuilder.authNavGraph(navController: NavController) {
    composable(Routes.SPLASH) {
        SplashScreen(
            onResolved = { destination ->
                val target = if (destination == SplashDestination.SIGN_IN) Routes.SIGN_IN else Routes.COMPANY_PICKER
                navController.navigate(target) { popUpTo(Routes.SPLASH) { inclusive = true } }
            }
        )
    }
    composable(Routes.CAROUSEL) {
        CarouselScreen(
            onGetStarted = { navController.navigate(Routes.SIGN_IN) { popUpTo(Routes.CAROUSEL) { inclusive = true } } },
            onSkip = { navController.navigate(Routes.SIGN_IN) { popUpTo(Routes.CAROUSEL) { inclusive = true } } }
        )
    }
    composable(Routes.SIGN_IN) {
        SignInScreen(
            onSignedIn = { navController.navigate(Routes.COMPANY_PICKER) { popUpTo(Routes.SIGN_IN) { inclusive = true } } },
            onTerms = {},
            onPrivacy = {}
        )
    }
    composable(Routes.COMPANY_PICKER) {
        CompanyPickerScreen(
            onCompanySelected = { navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.COMPANY_PICKER) { inclusive = true } } },
            onRegisterCompany = { navController.navigate(Routes.SETUP_WIZARD) },
            onSignOut = { navController.navigate(Routes.SPLASH) { popUpTo(0) { inclusive = true } } }
        )
    }
    composable(Routes.SETUP_WIZARD) {
        SetupWizardScreen(
            onFinish = { navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.SETUP_WIZARD) { inclusive = true } } },
            onSkip = { navController.popBackStack() }
        )
    }
    composable(Routes.PROFILE) {
        ProfileScreen(onBack = { navController.popBackStack() })
    }
}