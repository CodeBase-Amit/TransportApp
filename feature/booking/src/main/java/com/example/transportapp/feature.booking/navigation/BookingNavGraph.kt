package com.example.transportapp.feature.booking.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.feature.booking.screen.BiltyPreviewScreen
import com.example.transportapp.feature.booking.screen.BookingFormScreen

fun NavGraphBuilder.bookingNavGraph(navController: NavController) {
    composable(
        route = Routes.BOOKING_FORM,
        arguments = listOf(navArgument("amends") { type = NavType.StringType; defaultValue = "" }),
    ) { entry ->
        val amends = entry.arguments?.getString("amends").orEmpty()
        BookingFormScreen(
            onClose = { navController.popBackStack() },
            onBooked = { biltyNo ->
                navController.navigate(Routes.biltyPreview(biltyNo))
            },
            onSetRate = if (amends.isEmpty()) {
                { navController.navigate(Routes.rateCardEditor(SeedIds.PARTY_DEEPAK_STEEL)) }
            } else null
        )
    }
    composable(Routes.BILTY_PREVIEW) {
        BiltyPreviewScreen(
            onBack = { navController.popBackStack() },
            onSaveNew = {
                navController.navigate(Routes.BOOKING_FORM) {
                    popUpTo(Routes.BOOKING_FORM) { inclusive = true }
                }
            },
            onDone = { navController.popBackStack() }
        )
    }
}