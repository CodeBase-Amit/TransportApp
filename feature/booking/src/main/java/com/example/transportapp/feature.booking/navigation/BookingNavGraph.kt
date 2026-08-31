package com.example.transportapp.feature.booking.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.feature.booking.screen.BiltyPreviewScreen
import com.example.transportapp.feature.booking.screen.BookingFormScreen

fun NavGraphBuilder.bookingNavGraph(navController: NavController) {
    composable(Routes.BOOKING_FORM) {
        BookingFormScreen(
            onClose = { navController.popBackStack() },
            onBooked = { biltyNo ->
                navController.navigate(Routes.biltyPreview(biltyNo))
            },
            onSetRate = {
                navController.navigate(Routes.rateCardEditor(SeedIds.PARTY_DEEPAK_STEEL))
            }
        )
    }
    composable(Routes.BILTY_PREVIEW) {
        BiltyPreviewScreen(
            onBack = { navController.popBackStack() },
            onPrint = {},
            onShare = {},
            onSaveNew = {
                navController.navigate(Routes.BOOKING_FORM) {
                    popUpTo(Routes.BOOKING_FORM) { inclusive = true }
                }
            },
            onDone = { navController.popBackStack() }
        )
    }
}