package com.example.transportapp.feature.booking.screen

import com.example.transportapp.core.common.Money
import com.example.transportapp.core.ui.sample.BookingFormSampleData
import com.example.transportapp.core.ui.sample.ChargeLine
import com.example.transportapp.core.ui.sample.DeliveryType
import com.example.transportapp.core.ui.sample.Party
import com.example.transportapp.core.ui.sample.Risk
import com.example.transportapp.domain.transport.PaymentMode

/**
 * T5 state. First-frame defaults keep the preview honest; the ViewModel overwrites every
 * money field from the S4 engine within the first frame, per keystroke afterwards.
 */
data class BookingFormUiState(
    val reservedNumber: String = BookingFormSampleData.RESERVED_NUMBER,
    val consignor: Party? = BookingFormSampleData.deepakSteel,
    val consignee: Party? = BookingFormSampleData.nashikHardware,
    val goods: String = BookingFormSampleData.GOODS,
    val packages: String = BookingFormSampleData.PACKAGES,
    val actualWeightKg: String = BookingFormSampleData.ACTUAL_WEIGHT_KG,
    val rate: String = BookingFormSampleData.RATE,
    val rateNote: String = BookingFormSampleData.RATE_NOTE,
    val chargeableCaption: String = "Chargeable 780 kg · minimum 500 kg on this route",
    val paymentMode: PaymentMode = PaymentMode.TOPAY,
    val risk: Risk = Risk.OWNER,
    val delivery: DeliveryType = DeliveryType.DOOR,
    val charges: List<ChargeLine> = BookingFormSampleData.defaultCharges,
    val taxable: Money = BookingFormSampleData.TAXABLE,
    val gst: Money = BookingFormSampleData.GST,
    val gstLabel: String = BookingFormSampleData.GST_LABEL,
    val showRounding: Boolean = true,
    val roundingLabel: String = "+${BookingFormSampleData.ROUNDING.formatted()}",
    val grandTotal: Money = BookingFormSampleData.GRAND_TOTAL,
    val amountInWords: String = BookingFormSampleData.AMOUNT_IN_WORDS,
    val isSearchingConsignor: Boolean = false,
    val isSearchingConsignee: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Party> = emptyList(),
    val showMoreDetails: Boolean = false,
    /** Dimension inputs for the volumetric branch (S14): cm; blank disables volumetric. */
    val lengthCm: String = "",
    val breadthCm: String = "",
    val heightCm: String = "",
    /** Extra articles beyond the first (S15 multi-article): description, packages, weight kg. */
    val extraItems: List<ArticleRow> = emptyList(),
    /** The route picker's inline list (S14) — visible choice rows, not a popup. */
    val showRoutePicker: Boolean = false,
    /** The pickers' options (S14): id → label pairs loaded from the seeded masters. */
    val routeOptions: List<Pair<String, String>> = emptyList(),
    val goodsOptions: List<Pair<String, String>> = emptyList(),
    val routeLabel: String = BookingFormSampleData.ROUTE_LABEL,
    val weightError: String? = null,
    val rateCardWarning: String? = null,
    val provisionalWarning: String? = null,
    /** §7.1 amendment mode (S15): the original bilty this form supersedes. */
    val amending: String? = null,
    val amendReason: String = "",
    val bookedBy: String = BookingFormSampleData.BOOKED_BY,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface BookingFormEvent {
    /** Tap the empty "Tap to add" card to enter search mode (S14 pickers). */
    data object StartConsignorSearch : BookingFormEvent
    data object StartConsigneeSearch : BookingFormEvent
    data class SelectConsignor(val party: Party) : BookingFormEvent
    data class SelectConsignee(val party: Party) : BookingFormEvent
    data object ClearConsignor : BookingFormEvent
    data object ClearConsignee : BookingFormEvent
    data class SearchConsignor(val query: String) : BookingFormEvent
    data class SearchConsignee(val query: String) : BookingFormEvent
    data class SelectRoute(val routeId: String) : BookingFormEvent
    data class SelectGoods(val goodsId: String) : BookingFormEvent
    data object ToggleRoutePicker : BookingFormEvent
    data object AddArticle : BookingFormEvent
    data class RemoveArticle(val index: Int) : BookingFormEvent
    data class ChangeArticleDescription(val index: Int, val value: String) : BookingFormEvent
    data class ChangeArticlePackages(val index: Int, val value: String) : BookingFormEvent
    data class ChangeArticleWeight(val index: Int, val value: String) : BookingFormEvent
    data class ChangeLengthCm(val value: String) : BookingFormEvent
    data class ChangeBreadthCm(val value: String) : BookingFormEvent
    data class ChangeHeightCm(val value: String) : BookingFormEvent
    data class ChangePackages(val value: String) : BookingFormEvent
    data class ChangeWeight(val value: String) : BookingFormEvent
    data class ChangePaymentMode(val mode: PaymentMode) : BookingFormEvent
    data class ChangeRisk(val risk: Risk) : BookingFormEvent
    data class ChangeDelivery(val delivery: DeliveryType) : BookingFormEvent
    data class ChangeAmendReason(val value: String) : BookingFormEvent
    data object ToggleMoreDetails : BookingFormEvent
    /** Removing a charge row disables that head; computed rows are never removable. */
    data class RemoveCharge(val headCode: String?) : BookingFormEvent
    data object Submit : BookingFormEvent
}

/** One extra-article row (S15): the first article edits through the main packages/weight fields. */
data class ArticleRow(
    val description: String = "",
    val packages: String = "",
    val weightKg: String = "",
)