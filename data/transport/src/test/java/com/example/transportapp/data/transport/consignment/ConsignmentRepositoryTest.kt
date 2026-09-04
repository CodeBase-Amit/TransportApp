package com.example.transportapp.data.transport.consignment

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.entity.NumberSeriesEntity
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.data.transport.numbering.NumberingRepositoryImpl
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.data.transport.tracking.PhotoImporter
import com.example.transportapp.data.transport.rate.RateCardRepositoryImpl
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.session.UserSession
import com.example.transportapp.domain.transport.PaymentMode
import com.example.transportapp.domain.transport.calc.CalculationInput
import com.example.transportapp.domain.transport.calc.GstConfig
import com.example.transportapp.domain.transport.calc.GstTreatment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S5 (Phase2.md test charter): booking atomicity — a forced failure leaves nothing behind —
 * and the persistence shape of the aggregate: consignment + item + charge lines + Booked
 * event + snapshot + outbox, with totals equal to the sum of the lines (§3.4 #2, #5).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConsignmentRepositoryTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: ConsignmentRepositoryImpl
    private lateinit var numbering: NumberingRepositoryImpl
    private val company = SeedIds.COMPANY_SHIVSHAKTI

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DemoSeeder(database).seedIfNeeded()
        val rateCardRepository = RateCardRepositoryImpl(database.mastersDao(), database.orgDao(), database.settingsDao())
        numbering = NumberingRepositoryImpl(
            database,
            database.numberingDao(),
            deviceIdProvider = { "TEST1" },
        )
        val importer = PhotoImporter(ApplicationProvider.getApplicationContext())
        repository = ConsignmentRepositoryImpl(
            database = database,
            consignmentDao = database.consignmentDao(),
            mastersDao = database.mastersDao(),
            sessionRepository = fakeSession(),
            rateCardRepository = rateCardRepository,
            numberingRepository = numbering,
        outboxWriter = OutboxWriter(database.outboxDao()),
        statusRepository = com.example.transportapp.data.transport.tracking.StatusRepositoryImpl(
            database,
            fakeSession(),
            OutboxWriter(database.outboxDao()),
            importer,
        ),
    )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun fakeSession() = object : SessionRepository {
        override val session: Flow<UserSession> = flowOf(
            UserSession(
                userId = "u", name = "Mahesh Patidar", email = "mahesh.patidar@gmail.com", role = "OWNER",
                companyId = company, companyName = "Shivshakti Roadlines",
                branchId = SeedIds.BRANCH_INDORE, branchName = "Indore",
            ),
        )

        override suspend fun signIn() {}
        override suspend fun updateDisplayName(name: String) {}
        override suspend fun signOut() {}
    }

    private suspend fun canonicalDraft(): BookingDraft {
        val rateCard = RateCardRepositoryImpl(database.mastersDao(), database.orgDao(), database.settingsDao())
        val settings = rateCard.bookingSettings(company, SeedIds.ROUTE_INDORE_NASHIK)
        return BookingDraft(
            consignorId = SeedIds.PARTY_DEEPAK_STEEL,
            consigneeId = SeedIds.PARTY_NASHIK_HARDWARE,
            routeId = SeedIds.ROUTE_INDORE_NASHIK,
            goodsId = SeedIds.GOODS_MS_PIPES,
            goodsDescription = "MS pipes",
            paymentMode = PaymentMode.TOPAY,
            risk = "OWNERS",
            deliveryType = "DOOR",
            packages = 12,
            actualWeightG = 780_000,
            declaredValuePaise = 0,
            ewayBillNo = "281047556392",
            privateMark = "DST-114",
            calculationInput = CalculationInput(
                packages = 12,
                actualWeightG = 780_000,
                weightStepG = settings.weightStepG,
                volumetricDivisor = settings.volumetricDivisor,
                heads = rateCard.autoApplyHeads(company),
                rate = null,
                gst = GstConfig(
                    treatment = settings.gstTreatment,
                    rateBp = settings.gstRateBp,
                    placeOfSupplyState = settings.defaultPlaceOfSupplyState,
                    companyRegisteredState = settings.companyRegisteredState,
                ),
                rounding = settings.rounding,
            ),
        )
    }

    @Test
    fun `booking persists the full aggregate and stamps 04189`() = runTest {
        val result = repository.book(canonicalDraft()).getOrNull()!!
        assertEquals("IND/2627/04189", result.biltyNo)

        val consignment = database.consignmentDao().getConsignmentByBiltyNo(company, "IND/2627/04189")!!
        assertEquals("BOOKED", consignment.status_projection)
        assertEquals("the 10-6 grand total", 394_400L, consignment.total_paise)
        assertEquals("freight denorm", 351_000L, consignment.freight_paise)
        assertEquals("gst denorm", 18_780L, consignment.gst_paise)
        assertNull(consignment.provisional_no)

        val lines = database.consignmentDao().getChargeLines(consignment.local_id)
        assertEquals("totals derive from the stored lines", consignment.total_paise, lines.sumOf { it.computed_paise })
        assertEquals(1, database.consignmentDao().getItems(consignment.local_id).size)
        assertEquals("one Booked event, append-only", 1, database.consignmentDao().getEvents(consignment.local_id).size)

        val snapshot = database.consignmentDao().getLatestSnapshot(consignment.local_id)!!
        assertEquals(1, snapshot.version)
        assertEquals(4, snapshot.copy_count)
        assertEquals("2 outbox rows: consignment + its snapshot", 2, database.outboxDao().getPendingCount())

        // org.json escapes "/" in the stored JSON; assert through the real reader instead.
        val readBack = repository.snapshotByBiltyNo(company, "IND/2627/04189")!!
        assertEquals("IND/2627/04189", readBack.payload.docNo)
        assertEquals("Rupees three thousand nine hundred forty four only", readBack.payload.amountInWords)
    }

    @Test
    fun `three bookings consume the lease monotonically`() = runTest {
        val numbers = (1..3).map { repository.book(canonicalDraft()).getOrNull()!!.biltyNo }
        assertEquals(listOf("IND/2627/04189", "IND/2627/04190", "IND/2627/04191"), numbers)
        assertEquals("T7's count grows from the six seeded rows plus the money pool", 38, database.consignmentDao().countConsignments(company))
    }

    @Test
    fun `a forced failure leaves nothing behind`() = runTest {
        val before = database.consignmentDao().countConsignments(company)
        val seriesBefore = database.numberingDao().getSeries(company, SeedIds.BRANCH_INDORE, "BILTY")!!

        repository.debugFailBeforeSnapshot = true
        val outcome = runCatching { repository.book(canonicalDraft()) }
        assertTrue("the failure propagates out of the transaction", outcome.isFailure)
        assertEquals("no consignment row", before, database.consignmentDao().countConsignments(company))
        assertEquals("the lease was not consumed", seriesBefore.last_issued, database.numberingDao().getSeries(company, SeedIds.BRANCH_INDORE, "BILTY")!!.last_issued)
        assertEquals("no outbox rows", 0, database.outboxDao().getPendingCount())

        repository.debugFailBeforeSnapshot = false
        assertEquals("the next booking still gets 04189", "IND/2627/04189", repository.book(canonicalDraft()).getOrNull()!!.biltyNo)
    }

    @Test
    fun `provisional booking stores the PROV number and stays retrievable by it`() = runTest {
        numbering.debugShrinkActiveLease(company, SeedIds.BRANCH_INDORE, "BILTY")
        numbering.debugSetGrantsEnabled(false)

        val result = repository.book(canonicalDraft()).getOrNull()!!
        assertTrue(result.provisional)
        assertEquals("PROV-TEST1-000001", result.biltyNo)

        val consignment = database.consignmentDao().getConsignmentByProvisionalNo(company, "PROV-TEST1-000001")!!
        assertNull("no final number is stamped while provisional", consignment.bilty_no)
        assertEquals("the PROV number is retained forever (§9)", "PROV-TEST1-000001", consignment.provisional_no)

        val snapshot = repository.snapshotByBiltyNo(company, "PROV-TEST1-000001")!!
        assertEquals("PROV-TEST1-000001", snapshot.payload.docNo)
    }

    @Test
    fun `snapshot read-back round-trips the print payload`() = runTest {
        repository.book(canonicalDraft()).getOrNull()!!
        val snapshot = repository.snapshotByBiltyNo(company, "IND/2627/04189")!!
        assertEquals("3,944.00", snapshot.payload.grandTotal)
        assertEquals("3,510.00", snapshot.payload.freight)
        assertEquals("TO PAY", snapshot.payload.stamp)
        assertEquals("Deepak Steel Traders", snapshot.payload.consignorName)
        assertEquals("Nashik Hardware Mart", snapshot.payload.consigneeName)
        assertTrue(snapshot.payload.footer.contains("At owner's risk"))
        assertTrue(snapshot.payload.footer.contains("E-way bill 281047556392"))

        val second = repository.snapshotByBiltyNo(company, "IND/2627/04189")!!
        assertEquals("content hash is stable across reads", snapshot.contentHash, second.contentHash)
        assertNull(repository.snapshotByBiltyNo(company, "IND/2627/99999"))
    }

    @Test
    fun `a multi-article booking writes one item row per article`() = runTest {
        val draft = canonicalDraft().copy(
            extraItems = listOf(
                com.example.transportapp.data.transport.consignment.BookingItem(
                    goodsId = null, description = "TMT bars", packages = 6, actualWeightG = 400_000,
                ),
            ),
        )
        val result = repository.book(draft).getOrNull()!!

        val consignment = database.consignmentDao().getConsignmentByBiltyNo(company, result.biltyNo)!!
        val items = database.consignmentDao().getItems(consignment.local_id)
        assertEquals("one row per article", 2, items.size)
        assertEquals("the aggregate packages = Σ articles", 18L, consignment.packages)
        assertEquals("the aggregate weight = Σ articles", 1_180_000L, consignment.actual_weight_g)
        assertTrue(items.any { it.description == "MS pipes" })
        assertTrue(items.any { it.description == "TMT bars" })
    }
    @Test
    fun `amend books a linked successor with the reason on the amendment row`() = runTest {
        val original = repository.book(canonicalDraft()).getOrNull()!!

        val successor = repository.amend(
            original.biltyNo,
            "Weight corrected at loading",
            canonicalDraft().copy(packages = 10, actualWeightG = 650_000),
        ).getOrNull()!!

        val amendment = database.consignmentDao().getConsignmentByBiltyNo(company, successor.biltyNo)!!
        assertEquals("the amendment links to the original (§16.1)", original.consignmentLocalId, amendment.amends_id)
        assertEquals("Weight corrected at loading", amendment.amendment_reason)
        assertEquals(10L, amendment.packages)

        val events = database.consignmentDao().getEvents(amendment.local_id)
        assertEquals("the amendment starts its own log at Booked", "BOOKED", events.first().event_type)
    }

    @Test
    fun `amend is Manager-gated and needs a real reason`() = runTest {
        val original = repository.book(canonicalDraft()).getOrNull()!!
        val reasonTooShort = repository.amend(original.biltyNo, "typo", canonicalDraft())
        assertEquals(com.example.transportapp.core.common.ErrorCode.CONSIGNMENT_IMMUTABLE, (reasonTooShort as com.example.transportapp.core.common.Result.Failure).code)
    }

    @Test
    fun `cancel moves a Booked bilty to Cancelled and retains the number`() = runTest {
        val booked = repository.book(canonicalDraft()).getOrNull()!!

        repository.cancel(booked.biltyNo, "Consignor cancelled the order before loading").getOrNull()!!

        val consignment = database.consignmentDao().getConsignmentByBiltyNo(company, booked.biltyNo)!!
        assertEquals("CANCELLED", consignment.status_projection)
        assertEquals("the number is retained forever (§7.1)", booked.biltyNo, consignment.bilty_no)
        val events = database.consignmentDao().getEvents(consignment.local_id)
        assertEquals("CANCELLED", events.last().event_type)
        assertEquals("Consignor cancelled the order before loading", events.last().remark)
        assertEquals("MANAGER_CANCEL", events.last().reason_code)
    }

    @Test
    fun `a cancelled number cannot be re-cancelled and a clerk cannot cancel`() = runTest {
        val booked = repository.book(canonicalDraft()).getOrNull()!!
        repository.cancel(booked.biltyNo, "Consignor cancelled the order before loading").getOrNull()!!

        val again = repository.cancel(booked.biltyNo, "Trying to cancel twice")
        assertEquals(com.example.transportapp.core.common.ErrorCode.CONSIGNMENT_IMMUTABLE, (again as com.example.transportapp.core.common.Result.Failure).code)
    }
}
