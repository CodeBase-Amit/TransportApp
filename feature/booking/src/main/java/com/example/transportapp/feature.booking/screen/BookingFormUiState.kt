package com.example.transportapp.feature.booking.screen

import androidx.compose.runtime.Stable
import com.example.transportapp.core.common.Money
import com.example.transportapp.core.ui.sample.ChargeLine
import com.example.transportapp.core.ui.sample.DeliveryType
import com.example.transportapp.core.ui.sample.Party
import com.example.transportapp.core.ui.sample.Risk
import com.example.transportapp.domain.transport.PaymentMode

/**
 * T5 state. S18: first-frame defaults are empty — a real company books against its own
 * parties, not the demo seed (the ViewModel overwrites every money field per keystroke).
 */
@Stable
data class BookingFormUiState(
    val reservedNumber: String = "",
    val consignor: Party? = null,
    val consignee: Party? = null,
    val goods: String = "",
    val packages: String = "",
    val actualWeightKg: String = "",
    val rate: String = "",
    val rateNote: String = "",
    val chargeableCaption: String = "",
    val paymentMode: PaymentMode = PaymentMode.TOPAY,
    val risk: Risk = Risk.OWNER,
    val delivery: DeliveryType = DeliveryType.DOOR,
    val charges: List<ChargeLine> = emptyList(),
    val taxable: Money = Money(0),
    val gst: Money = Money(0),
    val gstLabel: String = "",
    val showRounding: Boolean = true,
    val roundingLabel: String = "+0.00",
    val grandTotal: Money = Money(0),
    val amountInWords: String = "",
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
    val routeLabel: String = "",
    val weightError: String? = null,
    val rateCardWarning: String? = null,
    val provisionalWarning: String? = null,
    /** §7.1 amendment mode (S15): the original bilty this form supersedes. */
    val amending: String? = null,
    val amendReason: String = "",
    /** S27: the "More details" inputs were dead fields — they hold and submit now. */
    val declaredValueRupees: String = "",
    val ewayBillNo: String = "",
    val bookedBy: String = "",
    /** S21 - the Add-charge dialog */
    val showAddCharge: Boolean = false,
    val chargeLabel: String = "",
    val chargeAmount: String = "",
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
data class ChangeDeclaredValue(val value: String) : BookingFormEvent
data class ChangeEwayBill(val value: String) : BookingFormEvent
    data object ToggleMoreDetails : BookingFormEvent
    /** Removing a charge row disables that head; computed rows are never removable. */
    data class RemoveCharge(val headCode: String?) : BookingFormEvent
    // S21 - the Add-charge dialog (manual charge lines the clerk adds on the form)
    data object ToggleAddCharge : BookingFormEvent
    data object DismissAddCharge : BookingFormEvent
    data class ChangeChargeLabel(val value: String) : BookingFormEvent
    data class ChangeChargeAmount(val value: String) : BookingFormEvent
    data object SaveManualCharge : BookingFormEvent
    data object Submit : BookingFormEvent
}

/** One extra-article row (S15): the first article edits through the main packages/weight fields. */
data class ArticleRow(
    val description: String = "",
    val packages: String = "",
    val weightKg: String = "",
)