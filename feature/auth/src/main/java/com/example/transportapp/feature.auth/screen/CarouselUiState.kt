package com.example.transportapp.feature.auth.screen

import com.example.transportapp.domain.transport.PaymentMode

data class StampGraphic(
    val mode: PaymentMode,
    val caption: String
)

data class CarouselPanel(
    val title: String,
    val body: String,
    val emoji: String? = null,
    val stamp: StampGraphic? = null
)

/**
 * T32 first-run carousel (S18): the three design panels are static UI copy, inline — the
 * sample singleton is gone (§5 decoupling).
 */
data class CarouselUiState(
    val currentPage: Int = 0,
    val panels: List<CarouselPanel> = listOf(
        CarouselPanel(
            title = "One form, four copies.",
            body = "Fill the booking form once. We automatically generate office, consignor, driver, and consignee copies just like your physical bilty book.",
            emoji = "📄"
        ),
        CarouselPanel(
            title = "Works without signal.",
            body = "Don't let bad connectivity stop your business. Create dockets offline, and they'll sync securely when your signal returns.",
            emoji = "📡"
        ),
        CarouselPanel(
            title = "",
            body = "",
            stamp = StampGraphic(PaymentMode.PAID, "Offline Area")
        )
    ),
    val skipLabel: String = "Skip",
    val nextLabel: String = "Next",
    val getStartedLabel: String = "Get started",
    val usedBeforeLabel: String = "I've used this before"
)

sealed interface CarouselEvent {
    data object Next : CarouselEvent
    data class SelectPage(val page: Int) : CarouselEvent
}
