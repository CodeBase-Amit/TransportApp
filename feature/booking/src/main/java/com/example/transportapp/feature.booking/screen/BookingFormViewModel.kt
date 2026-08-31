package com.example.transportapp.feature.booking.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.common.Money
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.common.formatIndianGrouping
import com.example.transportapp.core.ui.sample.BookingFormSampleData
import com.example.transportapp.core.ui.sample.ChargeLine
import com.example.transportapp.core.ui.sample.Party
import com.example.transportapp.data.transport.consignment.BookingDraft
import com.example.transportapp.data.transport.consignment.ConsignmentRepository
import com.example.transportapp.data.transport.rate.BookingCalcSettings
import com.example.transportapp.data.transport.rate.RateCardRepository
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.domain.transport.calc.CalculationInput
import com.example.transportapp.domain.transport.calc.ChargeCalculator
import com.example.transportapp.domain.transport.calc.ChargeHeadDef
import com.example.transportapp.domain.transport.calc.GstConfig
import com.example.transportapp.domain.transport.calc.GstTreatment
import com.example.transportapp.domain.transport.calc.RateBasis
import com.example.transportapp.domain.transport.calc.ResolvedRate
import com.example.transportapp.domain.transport.calc.RoundingRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * T5's live totals and booking (Phase2.md S4/S5). The rate resolves once per booking scope;
 * every packages/weight keystroke recomputes the full §10.4 sequence through the pure engine
 * — the clerk never does arithmetic. "Book and print" persists the consignment
 * transactionally (numbering, charges, event, snapshot, outbox) and navigates to T6.
 *
 * The booking scope is the demo's canonical row (Deepak Steel Traders · Indore → Nashik ·
 * MS pipes) until the party/route pickers are wired; the calculation and persistence behind
 * it are already real.
 */
@HiltViewModel
class BookingFormViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val rateCardRepository: RateCardRepository,
    private val numberingRepository: com.example.transportapp.data.transport.numbering.NumberingRepository,
    private val consignmentRepository: ConsignmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingFormUiState())
    val uiState: StateFlow<BookingFormUiState> = _uiState.asStateFlow()

    /** One-shot: the bilty number of a successful booking; T5 navigates to T6 on it. */
    private val _bookedBiltyNo = MutableStateFlow<String?>(null)
    val bookedBiltyNo: StateFlow<String?> = _bookedBiltyNo.asStateFlow()

    private var settings: BookingCalcSettings? = null
    private var resolvedRate: ResolvedRate? = null
    private var heads: List<ChargeHeadDef> = emptyList()
    private val removedHeadCodes = LinkedHashSet<String>()

    init {
        viewModelScope.launch {
            val session = sessionRepository.session.first()
            _uiState.update {
                it.copy(bookedBy = "${session.name} · ${session.branchName} · ${timestamp()}")
            }
            settings = rateCardRepository.bookingSettings(session.companyId, SeedIds.ROUTE_INDORE_NASHIK)
            resolvedRate = rateCardRepository.resolveBookingRate(
                session.companyId,
                partyId = SeedIds.PARTY_DEEPAK_STEEL,
                routeId = SeedIds.ROUTE_INDORE_NASHIK,
                goodsId = SeedIds.GOODS_MS_PIPES,
            )
            heads = rateCardRepository.autoApplyHeads(session.companyId)
            peekReserved(session.companyId, session.branchId)
            recompute()
        }
    }

    /** The reserved number shows from the first moment and is never typed (§3); a
     *  provisional reservation surfaces the §9 banner. Re-run after every booking. */
    private suspend fun peekReserved(companyId: String, branchId: String) {
        val reserved = numberingRepository.peekNext(companyId, branchId, DOC_TYPE_BILTY)
        _uiState.update {
            it.copy(
                reservedNumber = reserved?.display ?: it.reservedNumber,
                provisionalWarning = if (reserved?.provisional == true) PROVISIONAL_WARNING else null,
            )
        }
    }

    fun onEvent(event: BookingFormEvent) {
        when (event) {
            is BookingFormEvent.SelectConsignor -> _uiState.update {
                it.copy(consignor = event.party, isSearchingConsignor = false, searchQuery = "", searchResults = emptyList())
            }
            is BookingFormEvent.SelectConsignee -> _uiState.update {
                it.copy(consignee = event.party, isSearchingConsignee = false, searchQuery = "", searchResults = emptyList())
            }
            BookingFormEvent.ClearConsignor -> _uiState.update { it.copy(consignor = null) }
            BookingFormEvent.ClearConsignee -> _uiState.update { it.copy(consignee = null) }
            is BookingFormEvent.SearchConsignor -> _uiState.update {
                it.copy(isSearchingConsignor = true, searchQuery = event.query, searchResults = searchParties(event.query))
            }
            is BookingFormEvent.SearchConsignee -> _uiState.update {
                it.copy(isSearchingConsignee = true, searchQuery = event.query, searchResults = searchParties(event.query))
            }
            is BookingFormEvent.ChangePackages -> _uiState.update {
                it.copy(packages = event.value.filter { ch -> ch.isDigit() })
            }.also { recompute() }
            is BookingFormEvent.ChangeWeight -> _uiState.update {
                val filtered = event.value.filter { ch -> ch.isDigit() }
                val weightError = if ((filtered.toLongOrNull() ?: 0) > MAX_VEHICLE_KG) {
                    "Weight can't be more than the vehicle's 9,000 kg capacity. Check the figure or split the consignment."
                } else {
                    null
                }
                it.copy(actualWeightKg = filtered, weightError = weightError)
            }.also { recompute() }
            is BookingFormEvent.ChangePaymentMode -> _uiState.update { it.copy(paymentMode = event.mode) }
            is BookingFormEvent.ChangeRisk -> _uiState.update { it.copy(risk = event.risk) }
            is BookingFormEvent.ChangeDelivery -> _uiState.update { it.copy(delivery = event.delivery) }
            BookingFormEvent.ToggleMoreDetails -> _uiState.update { it.copy(showMoreDetails = !it.showMoreDetails) }
            is BookingFormEvent.RemoveCharge -> {
                event.headCode?.let { removedHeadCodes += it }
                recompute()
            }
            BookingFormEvent.Submit -> submit()
        }
    }

    /** Persists the consignment transactionally, then hands the stamped number to T6. */
    private fun submit() {
        val current = _uiState.value
        if (current.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val session = sessionRepository.session.first()
            val s = settings
            val input = CalculationInput(
                packages = current.packages.toLongOrNull() ?: 0L,
                actualWeightG = (current.actualWeightKg.toLongOrNull() ?: 0L) * 1000,
                volumetricDivisor = s?.volumetricDivisor,
                weightStepG = s?.weightStepG ?: DEFAULT_WEIGHT_STEP_G,
                rate = resolvedRate,
                heads = heads,
                removedHeadCodes = removedHeadCodes.toSet(),
                gst = GstConfig(
                    treatment = s?.gstTreatment ?: GstTreatment.FORWARD,
                    rateBp = s?.gstRateBp ?: DEFAULT_GST_RATE_BP,
                    placeOfSupplyState = s?.defaultPlaceOfSupplyState,
                    companyRegisteredState = s?.companyRegisteredState,
                ),
                rounding = s?.rounding ?: RoundingRule.NEAREST_RUPEE,
            )
            val draft = BookingDraft(
                consignorId = SeedIds.PARTY_DEEPAK_STEEL,
                consigneeId = SeedIds.PARTY_NASHIK_HARDWARE,
                routeId = SeedIds.ROUTE_INDORE_NASHIK,
                goodsId = SeedIds.GOODS_MS_PIPES,
                goodsDescription = current.goods,
                paymentMode = current.paymentMode,
                risk = "OWNERS",
                deliveryType = if (current.delivery == com.example.transportapp.core.ui.sample.DeliveryType.DOOR) "DOOR" else "GODOWN",
                packages = input.packages,
                actualWeightG = input.actualWeightG,
                declaredValuePaise = 0,
                ewayBillNo = null,
                privateMark = null,
                calculationInput = input,
            )
            val result = consignmentRepository.book(draft)
            val failure = result as? com.example.transportapp.core.common.Result.Failure
            _uiState.update { it.copy(isLoading = false, error = failure?.message ?: failure?.code?.name) }
            result.getOrNull()?.let { booked ->
                _bookedBiltyNo.value = booked.biltyNo
                peekReserved(session.companyId, session.branchId)
            }
        }
    }

    fun consumeBookedBiltyNo() {
        _bookedBiltyNo.value = null
    }

    /** One §10.4 pass; integer paise end to end, well inside the 16 ms budget. */
    private fun recompute() {
        val current = _uiState.value
        val s = settings
        val result = ChargeCalculator.calculate(
            CalculationInput(
                packages = current.packages.toLongOrNull() ?: 0L,
                actualWeightG = (current.actualWeightKg.toLongOrNull() ?: 0L) * 1000,
                volumetricDivisor = s?.volumetricDivisor,
                weightStepG = s?.weightStepG ?: DEFAULT_WEIGHT_STEP_G,
                rate = resolvedRate,
                heads = heads,
                removedHeadCodes = removedHeadCodes.toSet(),
                gst = GstConfig(
                    treatment = s?.gstTreatment ?: GstTreatment.FORWARD,
                    rateBp = s?.gstRateBp ?: DEFAULT_GST_RATE_BP,
                    placeOfSupplyState = s?.defaultPlaceOfSupplyState,
                    companyRegisteredState = s?.companyRegisteredState,
                ),
                rounding = s?.rounding ?: RoundingRule.NEAREST_RUPEE,
            ),
        )
        _uiState.update { state ->
            state.copy(
                rate = rateLabel(resolvedRate),
                rateNote = rateNote(resolvedRate),
                chargeableCaption = chargeableCaption(result.chargeableWeightG, resolvedRate),
                charges = result.lines
                    .filter { it.headCode != GST_CODE && it.headCode != ROUNDING_CODE }
                    .map { line ->
                        ChargeLine(
                            label = line.label,
                            amount = Money(line.amountPaise),
                            detail = line.detail,
                            isRemovable = line.removable,
                            isComputed = line.computed,
                            headCode = line.headCode,
                        )
                    },
                taxable = Money(result.taxablePaise),
                gst = Money(result.gst?.totalPaise ?: 0L),
                gstLabel = result.lines.firstOrNull { it.headCode == GST_CODE }?.label ?: state.gstLabel,
                showRounding = result.roundingDeltaPaise != 0L,
                roundingLabel = (if (result.roundingDeltaPaise > 0) "+" else "−") + Money(kotlin.math.abs(result.roundingDeltaPaise)).formatted(),
                grandTotal = Money(result.grandTotalPaise),
                amountInWords = result.amountInWords,
                rateCardWarning = rateWarning(state, resolvedRate),
            )
        }
    }

    private fun rateLabel(rate: ResolvedRate?): String {
        val candidate = rate?.candidate ?: return "no rate"
        val figure = Money(candidate.ratePaise).formatted()
        return when (candidate.basis) {
            RateBasis.PER_KG -> "$figure / kg"
            RateBasis.PER_TONNE -> "$figure / tonne"
            RateBasis.PER_QUINTAL -> "$figure / quintal"
            RateBasis.PER_PACKAGE -> "$figure / package"
            RateBasis.PER_TRIP, RateBasis.FIXED -> "$figure flat"
        }
    }

    private fun rateNote(rate: ResolvedRate?): String = when (rate?.step) {
        1, 2 -> "from Deepak Steel Traders rate card"
        3, 4 -> "from company rate card"
        5 -> "company default"
        else -> "no rate card"
    }

    private fun chargeableCaption(chargeableG: Long, rate: ResolvedRate?): String {
        val kg = chargeableG / 1000
        val base = "Chargeable ${formatIndianGrouping(kg)} kg"
        val min = rate?.candidate?.minQtyLabel?.let { " · minimum $it on this route" } ?: ""
        return base + min
    }

    /** Design T5's error-state banner: the company default stepped in for the party card. */
    private fun rateWarning(state: BookingFormUiState, rate: ResolvedRate?): String? = when {
        rate == null -> "No rate found for ${state.goods} on Indore → Nashik. Add one in the rate card before booking."
        rate.step == 5 -> "No rate found for ${state.goods} on Indore → Nashik for this party. Using the company default of ${rateLabel(rate)}."
        else -> null
    }

    /** Sample-backed until S5 wires the real party search into these fields. */
    private fun searchParties(query: String): List<Party> =
        if (query.length >= 2) {
            listOf(BookingFormSampleData.deepakSteel).filter { it.name.contains(query, ignoreCase = true) }
        } else {
            emptyList()
        }

    private fun timestamp(): String = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.ENGLISH).format(Date())

    private companion object {
        const val DEFAULT_WEIGHT_STEP_G = 1000L
        const val DEFAULT_GST_RATE_BP = 500
        const val MAX_VEHICLE_KG = 9000L
        const val GST_CODE = "gst"
        const val ROUNDING_CODE = "rounding"
        const val DOC_TYPE_BILTY = "BILTY"
        const val PROVISIONAL_WARNING = "You are booking on provisional numbers. Connect once to assign final numbers."
    }
}
