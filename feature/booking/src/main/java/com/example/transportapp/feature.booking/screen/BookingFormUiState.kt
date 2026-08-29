package com.example.transportapp.feature.booking.screen

import com.example.transportapp.core.common.Money
import com.example.transportapp.core.ui.sample.BookingFormSampleData
import com.example.transportapp.core.ui.sample.ChargeLine
import com.example.transportapp.core.ui.sample.DeliveryType
import com.example.transportapp.core.ui.sample.Party
import com.example.transportapp.core.ui.sample.Risk
import com.example.transportapp.domain.transport.PaymentMode

data class BookingFormUiState(
    val reservedNumber: String = BookingFormSampleData.RESERVED_NUMBER,
    val consignor: Party? = BookingFormSampleData.deepakSteel,
    val consignee: Party? = BookingFormSampleData.nashikHardware,
    val goods: String = BookingFormSampleData.GOODS,
    val packages: String = BookingFormSampleData.PACKAGES,
    val actualWeightKg: String = BookingFormSampleData.ACTUAL_WEIGHT_KG,
    val rate: String = BookingFormSampleData.RATE,
    val rateNote: String = BookingFormSampleData.RATE_NOTE,
    val paymentMode: PaymentMode = PaymentMode.TOPAY,
    val risk: Risk = Risk.OWNER,
    val delivery: DeliveryType = DeliveryType.DOOR,
    val charges: List<ChargeLine> = BookingFormSampleData.defaultCharges,
    val taxable: Money = BookingFormSampleData.TAXABLE,
    val gst: Money = BookingFormSampleData.GST,
    val gstLabel: String = BookingFormSampleData.GST_LABEL,
    val rounding: Money = BookingFormSampleData.ROUNDING,
    val grandTotal: Money = BookingFormSampleData.GRAND_TOTAL,
    val amountInWords: String = BookingFormSampleData.AMOUNT_IN_WORDS,
    val isSearchingConsignor: Boolean = false,
    val isSearchingConsignee: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Party> = emptyList(),
    val showMoreDetails: Boolean = false,
    val weightError: String? = null,
    val rateCardWarning: String? = null,
    val bookedBy: String = BookingFormSampleData.BOOKED_BY,
    val isLoading: Boolean = false,
    val error: String? = null
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
