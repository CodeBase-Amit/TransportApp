package com.example.transportapp.feature.booking

import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.data.transport.consignment.BookingResult
import com.example.transportapp.data.transport.consignment.ConsignmentRepository
import com.example.transportapp.data.transport.numbering.IssuedNumber
import com.example.transportapp.data.transport.numbering.NumberingRepository
import com.example.transportapp.data.transport.numbering.ReservedNumber
import com.example.transportapp.data.transport.rate.BookingCalcSettings
import com.example.transportapp.data.transport.rate.RateCardRepository
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.session.UserSession
import com.example.transportapp.domain.transport.calc.ChargeHeadBasis
import com.example.transportapp.domain.transport.calc.ChargeHeadDef
import com.example.transportapp.domain.transport.calc.GstTreatment
import com.example.transportapp.domain.transport.calc.RateBasis
import com.example.transportapp.domain.transport.calc.RateCandidate
import com.example.transportapp.domain.transport.calc.ResolvedRate
import com.example.transportapp.domain.transport.calc.RoundingRule
import com.example.transportapp.feature.booking.screen.BookingFormEvent
import com.example.transportapp.feature.booking.screen.BookingFormViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * S4: T5's live totals come from the real rate path and the pure engine. The canonical
 * row must reproduce the §10.6 figures exactly, per keystroke.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookingFormViewModelTest {

    private class FakeSessionRepository : SessionRepository {
        override val session: Flow<UserSession> = flowOf(
            UserSession(
                userId = "u", name = "Mahesh Patidar", email = "mahesh.patidar@gmail.com", role = "OWNER",
                companyId = SeedIds.COMPANY_SHIVSHAKTI, companyName = "Shivshakti Roadlines",
                branchId = SeedIds.BRANCH_INDORE, branchName = "Indore",
            ),
        )

        override suspend fun signIn() {}
        override suspend fun updateDisplayName(name: String) {}
        override suspend fun signOut() {}
    }

    private class FakeRateCardRepository : RateCardRepository {
        var rate: ResolvedRate? = canonicalRate()
        var heads: List<ChargeHeadDef> = canonicalHeads()
        var settings: BookingCalcSettings = canonicalSettings()

        override suspend fun resolveBookingRate(companyId: String, partyId: String?, routeId: String?, goodsId: String?) = rate
        override suspend fun autoApplyHeads(companyId: String) = heads
        override suspend fun bookingSettings(companyId: String, routeId: String?) = settings
        override suspend fun routeOptions(companyId: String) = listOf(
            com.example.transportapp.data.transport.rate.RouteChoice(
                id = SeedIds.ROUTE_INDORE_NASHIK,
                label = "Indore → Nashik · 585 km · usually 2 days",
                distanceKm = 585, transitDays = 2,
            ),
        )
        override suspend fun goodsOptions(companyId: String) = listOf(
            com.example.transportapp.data.transport.rate.GoodsChoice(SeedIds.GOODS_MS_PIPES, "MS pipes"),
        )

        companion object {
            fun canonicalRate() = ResolvedRate(
                RateCandidate(
                    localId = "seed-rate-deepak-0", partyId = SeedIds.PARTY_DEEPAK_STEEL,
                    routeId = SeedIds.ROUTE_INDORE_NASHIK, goodsId = SeedIds.GOODS_MS_PIPES,
                    basis = RateBasis.PER_KG, ratePaise = 450, minQtyLabel = "500 kg",
                ),
                step = 1,
            )

            fun canonicalHeads() = listOf(
                ChargeHeadDef("h1", "hamali", "Hamali", ChargeHeadBasis.PER_PACKAGE, 800, taxable = true, sortOrder = 1),
                ChargeHeadDef("h2", "door_delivery", "Door delivery", ChargeHeadBasis.FLAT, 15_000, taxable = true, sortOrder = 2),
            )

            fun canonicalSettings() = BookingCalcSettings(
                weightStepG = 1000, volumetricDivisor = null, gstTreatment = GstTreatment.FORWARD,
                gstRateBp = 500, rounding = RoundingRule.NEAREST_RUPEE,
                companyRegisteredState = "Madhya Pradesh", defaultPlaceOfSupplyState = "Maharashtra",
            )
        }
    }

    private class FakeNumberingRepository : NumberingRepository {
        var reserved: ReservedNumber? = ReservedNumber("IND/2627/04189", provisional = false)

        override suspend fun peekNext(companyId: String, branchId: String, docType: String) = reserved
        override suspend fun issueNext(companyId: String, branchId: String, docType: String, now: Long) =
            com.example.transportapp.core.common.Result.success(IssuedNumber("IND/2627/04189", provisional = false, rawValue = 4189))
        override suspend fun debugShrinkActiveLease(companyId: String, branchId: String, docType: String) {}
        override suspend fun debugSetGrantsEnabled(enabled: Boolean) {}
        override suspend fun ensureSeries(companyId: String, branchId: String, docType: String, prefix: String) {}
    }

    private class FakeConsignmentRepository : ConsignmentRepository {
        var booked: Int = 0

        override suspend fun book(draft: com.example.transportapp.data.transport.consignment.BookingDraft): com.example.transportapp.core.common.Result<BookingResult> {
            booked++
            return com.example.transportapp.core.common.Result.success(
                BookingResult(
                    consignmentLocalId = "cn-test-$booked",
                    biltyNo = "IND/2627/%05d".format(4188 + booked),
                    provisional = false,
                ),
            )
        }

        override suspend fun snapshotByBiltyNo(companyId: String, biltyNo: String) = null
        override suspend fun amend(originalLocalId: String, reason: String, draft: com.example.transportapp.data.transport.consignment.BookingDraft) = com.example.transportapp.core.common.Result.failure(com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE, "test")
        override suspend fun cancel(biltyNo: String, reason: String) = com.example.transportapp.core.common.Result.success(Unit)
        override suspend fun loadForAmendment(companyId: String, biltyNo: String) = null
    }

    private class FakeMastersRepository : com.example.transportapp.data.transport.masters.MastersRepository {
        override suspend fun counts(companyId: String) = com.example.transportapp.domain.transport.masters.MasterCounts(0, 0, 0, 0, 0, 0, 0, 0, 0)
        override fun observeParties(companyId: String, query: String, letter: String, duplicatesOnly: Boolean) = flowOf(emptyList<com.example.transportapp.domain.transport.masters.PartyListRow>())
        override fun observeDuplicateCount(companyId: String) = flowOf(0)
        override fun observeDuplicatePair(companyId: String) = flowOf(null)
        override suspend fun resolveParty(idOrName: String) = null
        override suspend fun partyDetail(localId: String) = null
        override suspend fun searchPartiesOnce(companyId: String, query: String) = emptyList<com.example.transportapp.domain.transport.masters.PartyListRow>()
        override suspend fun createOrUpdateParty(
            companyId: String,
            localId: String?,
            name: String,
            phone: String,
            email: String?,
            street: String?,
            station: String?,
            pincode: String?,
            gstin: String?,
            type: String,
            usualRouteId: String?,
            usualPaymentMode: String?,
        ): com.example.transportapp.core.common.Result<String> = com.example.transportapp.core.common.Result.success("p-1")
        override suspend fun deleteParty(localId: String) = com.example.transportapp.core.common.Result.success(Unit)
        override suspend fun mergeParties(keepId: String, mergeId: String) = com.example.transportapp.core.common.Result.success(Unit)
        override suspend fun rateRowsForParty(partyId: String) = emptyList<com.example.transportapp.domain.transport.masters.RateRow>()
        override suspend fun autoCharges(companyId: String) = emptyList<com.example.transportapp.domain.transport.masters.AutoCharge>()
        override suspend fun addRateRow(companyId: String, partyId: String, ratePaise: Long) = com.example.transportapp.core.common.Result.success(Unit)
        override suspend fun saveRateRow(localId: String, ratePaise: Long) = com.example.transportapp.core.common.Result.success(Unit)
    }

    private val rateRepo = FakeRateCardRepository()
    private val numberingRepo = FakeNumberingRepository()
    private val consignmentRepo = FakeConsignmentRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = BookingFormViewModel(androidx.lifecycle.SavedStateHandle(), FakeSessionRepository(), rateRepo, numberingRepo, consignmentRepo, FakeMastersRepository())

    /** S18: the form starts empty — the canonical row is typed, not defaulted. */
    private fun BookingFormViewModel.typeCanonicalRow() {
        onEvent(BookingFormEvent.ChangePackages("12"))
        onEvent(BookingFormEvent.ChangeWeight("780"))
    }

    @Test
    fun `canonical row reproduces the 10-6 figures on first frame`() = runTest {
        val vm = viewModel()
        vm.typeCanonicalRow()

        assertEquals(351_000L, vm.uiState.value.charges.first { it.label == "Freight" }.amount.paise)
        assertEquals(9_600L, vm.uiState.value.charges.first { it.label == "Hamali" }.amount.paise)
        assertEquals(15_000L, vm.uiState.value.charges.first { it.label == "Door delivery" }.amount.paise)
        assertEquals(375_600L, vm.uiState.value.taxable.paise)
        assertEquals(18_780L, vm.uiState.value.gst.paise)
        assertEquals("GST 5% — we pay, forward charge", vm.uiState.value.gstLabel)
        assertEquals("+0.20", vm.uiState.value.roundingLabel)
        assertEquals(394_400L, vm.uiState.value.grandTotal.paise)
        assertEquals("Three thousand nine hundred forty four rupees only", vm.uiState.value.amountInWords)
        assertEquals("4.50 / kg", vm.uiState.value.rate)
        assertEquals("from Deepak Steel Traders rate card", vm.uiState.value.rateNote)
        assertEquals("Chargeable 780 kg · minimum 500 kg on this route", vm.uiState.value.chargeableCaption)
        assertNull(vm.uiState.value.rateCardWarning)
    }

    @Test
    fun `weight keystroke recomputes everything including the words`() = runTest {
        val vm = viewModel()
        vm.typeCanonicalRow()

        vm.onEvent(BookingFormEvent.ChangeWeight("100"))

        val state = vm.uiState.value
        assertEquals(225_000L, state.charges.first { it.label == "Freight" }.amount.paise)
        assertEquals(249_600L, state.taxable.paise)
        assertEquals(12_480L, state.gst.paise)
        assertEquals(262_100L, state.grandTotal.paise)
        assertEquals("Two thousand six hundred twenty one rupees only", state.amountInWords)
        assertEquals("Chargeable 500 kg · minimum 500 kg on this route", state.chargeableCaption)
    }

    @Test
    fun `missing rate shows the no-rate banner`() = runTest {
        rateRepo.rate = null
        val vm = viewModel()

        assertTrue(vm.uiState.value.rateCardWarning!!.startsWith("No rate found"))
        assertEquals("no rate card", vm.uiState.value.rateNote)
    }

    @Test
    fun `company default step shows the fallback banner`() = runTest {
        rateRepo.rate = FakeRateCardRepository.canonicalRate().copy(
            candidate = FakeRateCardRepository.canonicalRate().candidate.copy(
                partyId = null, routeId = null, goodsId = null,
            ),
            step = 5,
        )
        val vm = viewModel()

        assertTrue(vm.uiState.value.rateCardWarning!!.contains("Using the company default of 4.50 / kg"))
        assertEquals("company default", vm.uiState.value.rateNote)
    }

    @Test
    fun `removing a head drops its line and reprices`() = runTest {
        val vm = viewModel()
        vm.typeCanonicalRow()

        vm.onEvent(BookingFormEvent.RemoveCharge("hamali"))

        val state = vm.uiState.value
        assertNull(state.charges.firstOrNull { it.label == "Hamali" })
        assertEquals(366_000L, state.taxable.paise)
    }

    @Test
    fun `the reserved number is peeked from the series, not typed`() = runTest {
        val vm = viewModel()

        assertEquals("IND/2627/04189", vm.uiState.value.reservedNumber)
        assertNull(vm.uiState.value.provisionalWarning)
    }

    @Test
    fun `a provisional reservation surfaces the section 9 banner`() = runTest {
        numberingRepo.reserved = ReservedNumber("PROV-TEST1-000001", provisional = true)
        val vm = viewModel()

        assertEquals("PROV-TEST1-000001", vm.uiState.value.reservedNumber)
        assertEquals(
            "You are booking on provisional numbers. Connect once to assign final numbers.",
            vm.uiState.value.provisionalWarning,
        )
    }

    @Test
    fun `submit books the consignment and emits the stamped number`() = runTest {
        val vm = viewModel()

        vm.onEvent(BookingFormEvent.Submit)

        assertEquals(1, consignmentRepo.booked)
        assertEquals("IND/2627/04189", vm.bookedBiltyNo.value)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `typed draft survives process death via the same SavedStateHandle`() = runTest {
        val sharedHandle = androidx.lifecycle.SavedStateHandle()
        val first = BookingFormViewModel(sharedHandle, FakeSessionRepository(), rateRepo, numberingRepo, consignmentRepo, FakeMastersRepository())

        first.onEvent(BookingFormEvent.ChangePackages("12"))
        first.onEvent(BookingFormEvent.ChangeWeight("780"))
        first.onEvent(BookingFormEvent.ChangePaymentMode(com.example.transportapp.domain.transport.PaymentMode.PAID))
        first.onEvent(BookingFormEvent.AddArticle)
        first.onEvent(BookingFormEvent.ChangeArticleDescription(0, "Steel pipes"))

        // "Process death": a brand-new ViewModel over the same handle restores the draft.
        val reborn = BookingFormViewModel(sharedHandle, FakeSessionRepository(), rateRepo, numberingRepo, consignmentRepo, FakeMastersRepository())

        assertEquals("12", reborn.uiState.value.packages)
        assertEquals("780", reborn.uiState.value.actualWeightKg)
        assertEquals(com.example.transportapp.domain.transport.PaymentMode.PAID, reborn.uiState.value.paymentMode)
        assertEquals(1, reborn.uiState.value.extraItems.size)
        assertEquals("Steel pipes", reborn.uiState.value.extraItems.first().description)
    }
}
