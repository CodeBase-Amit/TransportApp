package com.example.transportapp.feature.booking.screen

import com.example.transportapp.core.common.Money
import com.example.transportapp.domain.transport.PaymentMode

data class BookingFormUiState(
    val reservedNumber: String = "IND/2627/04189",
    val consignor: Party? = deepakSteel,
    val consignee: Party? = nashikHardware,
    val goods: String = "MS pipes",
    val packages: String = "12",
    val actualWeightKg: String = "780",
    val rate: String = "4.50 / kg",
    val rateNote: String = "from Deepak Steel Traders rate card",
    val paymentMode: PaymentMode = PaymentMode.TOPAY,
    val risk: Risk = Risk.OWNER,
    val delivery: DeliveryType = DeliveryType.DOOR,
    val charges: List<ChargeLine> = defaultCharges,
    val taxable: Money = Money.fromRupees(3756),
    val gst: Money = Money.fromRupees(187, 80),
    val gstLabel: String = "GST 5% — we pay, forward charge",
    val rounding: Money = Money.fromRupees(0, 20),
    val grandTotal: Money = Money.fromRupees(3944),
    val amountInWords: String = "Three thousand nine hundred forty four rupees only",
    val isSearchingConsignor: Boolean = false,
    val isSearchingConsignee: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Party> = emptyList(),
    val showMoreDetails: Boolean = false,
    val weightError: String? = null,
    val rateCardWarning: String? = null,
    val bookedBy: String = "Mahesh Patidar · Indore · 25 Aug 2026, 11:42 AM",
    val isLoading: Boolean = false,
    val error: String? = null
)

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
    val isComputed: Boolean = false
)

enum class Risk { OWNER, CARRIER }
enum class DeliveryType { GODOWN, DOOR }

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

private val defaultCharges = listOf(
    ChargeLine("Freight", Money.fromRupees(3510), "780 kg × 4.50", isComputed = true),
    ChargeLine("Hamali", Money.fromRupees(96), "12 × 8.00", isRemovable = true),
    ChargeLine("Door delivery", Money.fromRupees(150), "fixed", isRemovable = true)
)

sealed interface BookingFormEvent {
    data class SelectConsignor(val party: Party) : BookingFormEvent
    data class SelectConsignee(val party: Party) : BookingFormEvent
    data object ClearConsignor : BookingFormEvent
    data object ClearConsignee : BookingFormEvent
    data class SearchConsignor(val query: String) : BookingFormEvent
    data class SearchConsignee(val query: String) : BookingFormEvent
    data class ChangePackages(val value: String) : BookingFormEvent
    data class ChangeWeight(val value: String) : BookingFormEvent
    data class ChangePaymentMode(val mode: PaymentMode) : BookingFormEvent
    data class ChangeRisk(val risk: Risk) : BookingFormEvent
    data class ChangeDelivery(val delivery: DeliveryType) : BookingFormEvent
    data object ToggleMoreDetails : BookingFormEvent
    data class RemoveCharge(val index: Int) : BookingFormEvent
    data object Submit : BookingFormEvent
}