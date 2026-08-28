package com.example.transportapp.feature.booking.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.transportapp.core.ui.Routes
import com.example.transportapp.feature.booking.screen.BiltyPreviewContent
import com.example.transportapp.feature.booking.screen.BookingFormScreen
import com.example.transportapp.feature.booking.screen.BiltyPreviewUiState

fun NavGraphBuilder.bookingNavGraph(navController: NavController) {
    composable(Routes.BOOKING_FORM) {
        BookingFormScreen(
            onClose = { navController.popBackStack() },
            onBookAndPrint = {
                navController.navigate(Routes.biltyPreview("IND/2627/04188"))
            }
        )
    }
    composable(Routes.BILTY_PREVIEW) {
        BiltyPreviewContent(
            state = BiltyPreviewUiState(),
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