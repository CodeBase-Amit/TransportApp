package com.example.transportapp.core.ui.sample

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
 * T32 First-run carousel demo data. Three panels matched to the design.
 */
object CarouselSampleData {

    val PANELS = listOf(
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
    )

    const val SKIP = "Skip"
    const val NEXT = "Next"
    const val GET_STARTED = "Get started"
    const val USED_BEFORE = "I've used this before"
}
