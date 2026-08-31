package com.example.transportapp.core.ui.sample

import com.example.transportapp.core.common.Money

data class Party(
    val name: String,
    val phone: String,
    val station: String,
    val gstin: String,
    val biltyCount: Int = 0,
    val usualRoute: String? = null
)

data class Route(
    val from: String,
    val to: String,
    val distance: String = "585 km",
    val transitDays: String = "2 days",
    val expectedArrival: String = "27 Aug"
)

data class ChargeLine(
    val label: String,
    val amount: Money,
    val detail: String,
    val isRemovable: Boolean = false,
    val isComputed: Boolean = false,
    /** The charge head this line came from, so removal can disable the right one. */
    val headCode: String? = null,
)

enum class Risk { OWNER, CARRIER }
enum class DeliveryType { GODOWN, DOOR }

/**
 * T5 Booking form demo data. UiState defaults all come from here so the
 * screen stays stateless and the Content composable never touches sample data.
 */
object BookingFormSampleData {

    const val RESERVED_NUMBER = "IND/2627/04189"

    const val GOODS = "MS pipes"
    const val PACKAGES = "12"
    const val ACTUAL_WEIGHT_KG = "780"
    const val RATE = "4.50 / kg"
    const val RATE_NOTE = "from Deepak Steel Traders rate card"

    val TAXABLE = Money.fromRupees(3756)
    val GST = Money.fromRupees(187, 80)
    const val GST_LABEL = "GST 5% — we pay, forward charge"
    val ROUNDING = Money.fromRupees(0, 20)
    val GRAND_TOTAL = Money.fromRupees(3944)
    const val AMOUNT_IN_WORDS = "Three thousand nine hundred forty four rupees only"

    const val BOOKED_BY = "Mahesh Patidar · Indore · 25 Aug 2026, 11:42 AM"

    val deepakSteel = Party(
        name = "Deepak Steel Traders",
        phone = "+91 94250 61183",
        station = "Indore",
        gstin = "23AACDS8812K1Z4",
        biltyCount = 41,
        usualRoute = "Indore → Nashik"
    )

    val nashikHardware = Party(
        name = "Nashik Hardware Mart",
        phone = "+91 98600 27419",
        station = "Nashik",
        gstin = "27AAFCN3390L1Z8"
    )

    val defaultCharges = listOf(
        ChargeLine("Freight", Money.fromRupees(3510), "780 kg × 4.50", isComputed = true),
        ChargeLine("Hamali", Money.fromRupees(96), "12 × 8.00", isRemovable = true),
        ChargeLine("Door delivery", Money.fromRupees(150), "fixed", isRemovable = true)
    )
}
