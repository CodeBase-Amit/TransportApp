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
import com.example.transportapp.domain.transport.calc.PackageDims
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
 * S14: the booking scope comes from real pickers — party search over the seeded masters
 * (bounded LIKE per D7, benchmark-tested at 5k), route and goods picked from lists — and the
 * dated company setting governs the calculation (GST rate, volumetric divisor). The canonical
 * demo row loads as the initial selection so the form opens ready to book.
 */
@HiltViewModel
class BookingFormViewModel @Inject constructor(
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val rateCardRepository: RateCardRepository,
    private val numberingRepository: com.example.transportapp.data.transport.numbering.NumberingRepository,
    private val consignmentRepository: ConsignmentRepository,
    private val mastersRepository: com.example.transportapp.data.transport.masters.MastersRepository,
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

    /** The picker scope (S14): route + goods ids the rate resolves against. */
    private var routeId: String = SeedIds.ROUTE_INDORE_NASHIK
    private var goodsId: String? = SeedIds.GOODS_MS_PIPES
    /** §7.1 amendment mode (S15): the original bilty this form supersedes, or null. */
    private var amendingOriginalId: String? = null
    private val savedAmendSource: String? = savedStateHandle["amends"]

    init {
        viewModelScope.launch {
            val session = sessionRepository.session.first()
            _uiState.update {
                it.copy(bookedBy = "${session.name} · ${session.branchName} · ${timestamp()}")
            }
            // S15 amendment prefill: "booking?amends=<biltyNo>" loads the original's scope.
            val amendSource: String? = savedAmendSource
            if (amendSource != null) {
                val prefill = consignmentRepository.loadForAmendment(session.companyId, amendSource)
                if (prefill != null) {
                    amendingOriginalId = amendSource
                    routeId = prefill.routeId
                    goodsId = prefill.goodsId
                    resolveScope(partyId = prefill.consignorId)
                    heads = rateCardRepository.autoApplyHeads(session.companyId)
                    val routes = rateCardRepository.routeOptions(session.companyId)
                    val goods = rateCardRepository.goodsOptions(session.companyId)
                    _uiState.update {
                        it.copy(
                            consignor = com.example.transportapp.core.ui.sample.Party(
                                id = prefill.consignorId, name = "", phone = "", station = "", gstin = "",
                            ),
                            consignee = com.example.transportapp.core.ui.sample.Party(
                                id = prefill.consigneeId, name = "", phone = "", station = "", gstin = "",
                            ),
                            goods = prefill.goodsDescription,
                            packages = prefill.packages.toString(),
                            actualWeightKg = (prefill.actualWeightG / 1000).toString(),
                            routeOptions = routes.map { it.id to it.label },
                            goodsOptions = goods.map { it.id to it.name },
                            routeLabel = routes.firstOrNull { it.id == prefill.routeId }?.label ?: "",
                            amending = amendSource,
                            bookedBy = "Amending $amendSource · ${session.name} · ${timestamp()}",
                        )
                    }
                    recompute()
                    peekReserved(session.companyId, session.branchId)
                    return@launch
                }
            }
            resolveScope(partyId = SeedIds.PARTY_DEEPAK_STEEL)
            heads = rateCardRepository.autoApplyHeads(session.companyId)
            // The pickers' options (S14): every company route and goods type, loaded once.
            val routes = rateCardRepository.routeOptions(session.companyId)
            val goods = rateCardRepository.goodsOptions(session.companyId)
            if (routes.none { it.id == routeId }) {
                routeId = routes.firstOrNull()?.id ?: routeId
                goodsId = null
                resolveScope(partyId = SeedIds.PARTY_DEEPAK_STEEL)
            }
            _uiState.update { state ->
                state.copy(
                    routeOptions = routes.map { it.id to it.label },
                    goodsOptions = goods.map { it.id to it.name },
                    routeLabel = routes.firstOrNull { it.id == routeId }?.label ?: state.routeLabel,
                    goods = goods.firstOrNull { it.id == goodsId }?.name ?: state.goods,
                )
            }
            peekReserved(session.companyId, session.branchId)
            recompute()
        }
    }

    /**
     * Re-resolve the booking scope: settings (dated), rate (5-step walk over the selected
     * party/route/goods), and the reserved number. Called at init and after every picker
     * selection — a scope change changes the rate row, the GST place of supply and the
     * chargeable caption together.
     */
    private suspend fun resolveScope(partyId: String?) {
        val session = sessionRepository.session.first()
        settings = rateCardRepository.bookingSettings(session.companyId, routeId)
        resolvedRate = rateCardRepository.resolveBookingRate(
            session.companyId,
            partyId = partyId,
            routeId = routeId,
            goodsId = goodsId,
        )
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
            BookingFormEvent.StartConsignorSearch -> _uiState.update { it.copy(isSearchingConsignor = true) }
            BookingFormEvent.StartConsigneeSearch -> _uiState.update { it.copy(isSearchingConsignee = true) }
            is BookingFormEvent.SelectConsignor -> _uiState.update {
                it.copy(consignor = event.party, isSearchingConsignor = false, searchQuery = "", searchResults = emptyList())
            }
            is BookingFormEvent.SelectConsignee -> _uiState.update {
                it.copy(consignee = event.party, isSearchingConsignee = false, searchQuery = "", searchResults = emptyList())
            }
            BookingFormEvent.ClearConsignor -> _uiState.update { it.copy(consignor = null) }
            BookingFormEvent.ClearConsignee -> _uiState.update { it.copy(consignee = null) }
            is BookingFormEvent.SearchConsignor -> searchParties(event.query) { results ->
                _uiState.update { it.copy(isSearchingConsignor = true, searchQuery = event.query, searchResults = results) }
            }
            is BookingFormEvent.SearchConsignee -> searchParties(event.query) { results ->
                _uiState.update { it.copy(isSearchingConsignee = true, searchQuery = event.query, searchResults = results) }
            }
            is BookingFormEvent.SelectRoute -> {
                _uiState.update { it.copy(showRoutePicker = false) }
                onRouteSelected(event.routeId)
            }
            is BookingFormEvent.SelectGoods -> onGoodsSelected(event.goodsId)
            BookingFormEvent.ToggleRoutePicker -> _uiState.update { it.copy(showRoutePicker = !it.showRoutePicker) }
            is BookingFormEvent.ChangeLengthCm -> _uiState.update { it.copy(lengthCm = event.value.filter { ch -> ch.isDigit() }) }.also { recompute() }
            is BookingFormEvent.ChangeBreadthCm -> _uiState.update { it.copy(breadthCm = event.value.filter { ch -> ch.isDigit() }) }.also { recompute() }
            is BookingFormEvent.ChangeHeightCm -> _uiState.update { it.copy(heightCm = event.value.filter { ch -> ch.isDigit() }) }.also { recompute() }
            is BookingFormEvent.AddArticle -> _uiState.update { it.copy(extraItems = it.extraItems + com.example.transportapp.feature.booking.screen.ArticleRow()) }
            is BookingFormEvent.RemoveArticle -> _uiState.update { state ->
                state.copy(extraItems = state.extraItems.filterIndexed { i, _ -> i != event.index })
            }.also { recompute() }
            is BookingFormEvent.ChangeArticleDescription -> _uiState.update { state ->
                state.copy(extraItems = state.extraItems.mapIndexed { i, row -> if (i == event.index) row.copy(description = event.value) else row })
            }
            is BookingFormEvent.ChangeArticlePackages -> _uiState.update { state ->
                state.copy(extraItems = state.extraItems.mapIndexed { i, row -> if (i == event.index) row.copy(packages = event.value.filter { ch -> ch.isDigit() }) else row })
            }.also { recompute() }
            is BookingFormEvent.ChangeArticleWeight -> _uiState.update { state ->
                state.copy(extraItems = state.extraItems.mapIndexed { i, row -> if (i == event.index) row.copy(weightKg = event.value.filter { ch -> ch.isDigit() }) else row })
            }.also { recompute() }
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
            is BookingFormEvent.ChangeAmendReason -> _uiState.update { it.copy(amendReason = event.value) }
            BookingFormEvent.ToggleMoreDetails -> _uiState.update { it.copy(showMoreDetails = !it.showMoreDetails) }
            is BookingFormEvent.RemoveCharge -> {
                event.headCode?.let { removedHeadCodes += it }
                recompute()
            }
            BookingFormEvent.Submit -> submit()
        }
    }

    /** A route or goods change changes the rate walk; the consignor's party card drives step 1. */
    private fun onRouteSelected(newRouteId: String) {
        routeId = newRouteId
        viewModelScope.launch {
            val routes = rateCardRepository.routeOptions(sessionRepository.session.first().companyId)
            resolveScope(partyId = _uiState.value.consignor?.id)
            _uiState.update { state ->
                state.copy(routeLabel = routes.firstOrNull { it.id == newRouteId }?.label ?: state.routeLabel)
            }
            recompute()
        }
    }

    private fun onGoodsSelected(newGoodsId: String) {
        goodsId = newGoodsId.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            val session = sessionRepository.session.first()
            val goods = rateCardRepository.goodsOptions(session.companyId)
            resolveScope(partyId = _uiState.value.consignor?.id)
            _uiState.update { state ->
                state.copy(goods = goods.firstOrNull { it.id == goodsId }?.name ?: state.goods)
            }
            recompute()
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
            val dims = volumetricDims(current)
            val extraPackages = current.extraItems.sumOf { it.packages.toLongOrNull() ?: 0L }
            val extraWeightG = current.extraItems.sumOf { (it.weightKg.toLongOrNull() ?: 0L) * 1000 }
            val input = CalculationInput(
                packages = (current.packages.toLongOrNull() ?: 0L) + extraPackages,
                actualWeightG = ((current.actualWeightKg.toLongOrNull() ?: 0L) * 1000) + extraWeightG,
                dims = dims,
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
                consignorId = current.consignor?.id ?: SeedIds.PARTY_DEEPAK_STEEL,
                consigneeId = current.consignee?.id ?: SeedIds.PARTY_NASHIK_HARDWARE,
                routeId = routeId,
                goodsId = goodsId,
                goodsDescription = current.goods,
                paymentMode = current.paymentMode,
                risk = "OWNERS",
                deliveryType = if (current.delivery == com.example.transportapp.core.ui.sample.DeliveryType.DOOR) "DOOR" else "GODOWN",
                packages = current.packages.toLongOrNull() ?: 0L,
                actualWeightG = (current.actualWeightKg.toLongOrNull() ?: 0L) * 1000,
                declaredValuePaise = 0,
                ewayBillNo = null,
                privateMark = null,
                calculationInput = input,
                extraItems = current.extraItems.map { row ->
                    com.example.transportapp.data.transport.consignment.BookingItem(
                        goodsId = goodsId,
                        description = row.description.ifBlank { "Article" },
                        packages = row.packages.toLongOrNull() ?: 0L,
                        actualWeightG = (row.weightKg.toLongOrNull() ?: 0L) * 1000,
                    )
                },
            )
            // §7.1: in amendment mode the submission books the linked successor.
            val result = amendingOriginalId?.let { originalId ->
                consignmentRepository.amend(originalId, current.amendReason, draft)
            } ?: consignmentRepository.book(draft)
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
        // S15 multi-article: the rate walk prices the aggregate — Σ packages, Σ weight.
        val extraPackages = current.extraItems.sumOf { it.packages.toLongOrNull() ?: 0L }
        val extraWeightG = current.extraItems.sumOf { (it.weightKg.toLongOrNull() ?: 0L) * 1000 }
        val result = ChargeCalculator.calculate(
            CalculationInput(
                packages = (current.packages.toLongOrNull() ?: 0L) + extraPackages,
                actualWeightG = ((current.actualWeightKg.toLongOrNull() ?: 0L) * 1000) + extraWeightG,
                dims = volumetricDims(current),
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

    /**
     * The §10.1 volumetric input: dimensions count only when all three are filled and the
     * company's dated setting enables the divisor. Blank boxes keep the full-load house.
     */
    private fun volumetricDims(state: BookingFormUiState): PackageDims? {
        val l = state.lengthCm.toLongOrNull() ?: return null
        val b = state.breadthCm.toLongOrNull() ?: return null
        val h = state.heightCm.toLongOrNull() ?: return null
        if (l <= 0 || b <= 0 || h <= 0) return null
        return PackageDims(l, b, h)
    }

    /**
     * Real party search over the seeded masters (S14): bounded LIKE per D7, benchmarked at
     * 5,000 parties inside the 120 ms budget (PartySearchBenchmarkTest). Name or phone
     * substring, minimum two characters. Runs off the main thread and updates the state
     * when the (local, single-digit-ms) query answers.
     */
    private fun searchParties(query: String, into: (List<Party>) -> Unit) {
        if (query.trim().length < 2) {
            into(emptyList())
            return
        }
        viewModelScope.launch {
            val session = sessionRepository.session.first()
            val rows = mastersRepository.searchPartiesOnce(session.companyId, query.trim())
            into(rows.map { row ->
                Party(
                    id = row.localId,
                    name = row.name,
                    phone = row.phone,
                    station = row.station ?: "",
                    gstin = "",
                    biltyCount = row.biltyCount,
                    usualRoute = null,
                )
            })
        }
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
