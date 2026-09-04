package com.example.transportapp.data.transport.billing

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.entity.ConsignmentEntity
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.data.transport.numbering.NumberingRepositoryImpl
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.data.transport.tracking.PhotoImporter
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.session.UserSession
import com.example.transportapp.data.transport.tracking.StatusRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S9 (Phase2.md test charter): the §12 money rules — one consignment on at most one live
 * bill, the frozen GST treatment (mixed refused), the offline issue refusal, cancel
 * returning consignments, explicit allocation that may not exceed the receipt, and a
 * statement that reconciles to the rupee.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BillingRepositoryTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: BillingRepositoryImpl
    private lateinit var statusRepository: StatusRepositoryImpl
    private lateinit var sessionFlow: kotlinx.coroutines.flow.MutableStateFlow<UserSession>

    private val company = SeedIds.COMPANY_SHIVSHAKTI
    private val branch = SeedIds.BRANCH_INDORE
    private val deepak = SeedIds.PARTY_DEEPAK_STEEL
    private val now = System.currentTimeMillis()

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DemoSeeder(database).seedIfNeeded()
        sessionFlow = kotlinx.coroutines.flow.MutableStateFlow(ownerSession())
        val numbering = NumberingRepositoryImpl(database, database.numberingDao(), deviceIdProvider = { "TEST1" })
        val importer = PhotoImporter(context)
        val outbox = OutboxWriter(database.outboxDao())
        val sessions = object : SessionRepository {
            override val session = sessionFlow
            override suspend fun signIn() {}
        override suspend fun updateDisplayName(name: String) {}
        override suspend fun signOut() {}
        }
        repository = BillingRepositoryImpl(database, sessions, numbering, outbox)
        statusRepository = StatusRepositoryImpl(database, sessions, outbox, importer)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun ownerSession() = UserSession(
        userId = "u", name = "Mahesh Patidar", email = DemoSeeder.EMAIL_DEMO_USER, role = "OWNER",
        companyId = company, companyName = "Shivshakti Roadlines", branchId = branch, branchName = "Indore",
    )

    private fun clerkSession() = ownerSession().copy(role = "BOOKING_CLERK")

    /** The first unbilled pool consignments (seed v6 leaves exactly 23 for Deepak Steel). */
    private suspend fun poolConsignmentIds(count: Int): List<String> =
        repository.observeUnbilledRows(deepak, PoolFilter()).first().take(count).map { it.localId }

    @Test
    fun `the seeded pool leaves twenty-three unbilled consignments for deepak steel`() = runTest {
        val pool = repository.observeUnbilledPool(PoolFilter()).first()
        assertEquals(1, pool.size)
        val deepakGroup = pool.single()
        assertEquals("Deepak Steel Traders", deepakGroup.partyName)
        assertEquals("23 consignments stay unbilled (29 seeded − 3 + 3 on bills)", 23, deepakGroup.consignments)
        assertTrue("the ageing bar has both 0-30 and 31-60 segments", deepakGroup.bucket0to30Paise > 0 && deepakGroup.bucket31to60Paise > 0)
        assertTrue("the freight figure is Σ freight, not Σ total", deepakGroup.freightPaise < deepakGroup.totalPaise)
    }

    @Test
    fun `building a draft moves the consignments out of the pool atomically`() = runTest {
        val ids = poolConsignmentIds(2)
        val bill = repository.buildDraftBill(deepak, ids, dueAt = now + 30L * BillingRepositoryImpl.DAY_MS, notes = null, now = now).getOrNull()!!
        assertEquals("DRAFT", bill.state)
        assertNull("a draft carries no number until issue (§12.1)", bill.billNo)
        assertEquals("total = Σ consignment totals", ids.sumOf { database.consignmentDao().getConsignment(it)!!.total_paise }, bill.totalPaise)

        val pool = repository.observeUnbilledPool(PoolFilter()).first().single()
        assertEquals("two consignments left the pool", 21, pool.consignments)
    }

    @Test
    fun `a consignment can sit on at most one live bill`() = runTest {
        val ids = poolConsignmentIds(2)
        repository.buildDraftBill(deepak, ids, null, null, now).getOrNull()!!

        val again = repository.buildDraftBill(deepak, listOf(ids[0]), null, null, now)
        assertTrue(again is Result.Failure)
        assertEquals(ErrorCode.ALREADY_BILLED, (again as Result.Failure).code)
    }

    @Test
    fun `mixed gst treatments refuse the bill and name the treatments`() = runTest {
        val intra = insertTbbConsignment("IND/2627/90050", placeOfSupply = "Madhya Pradesh")
        val ids = poolConsignmentIds(1) + listOf(intra)

        val refused = repository.buildDraftBill(deepak, ids, null, null, now)
        assertTrue(refused is Result.Failure)
        assertEquals(ErrorCode.BILL_MIXED_TREATMENT, (refused as Result.Failure).code)
        assertTrue((refused.message ?: "").contains("Inter-state"))
        assertTrue((refused.message ?: "").contains("Intra-state"))
        assertNull("nothing was billed", database.consignmentDao().getConsignment(ids[0])!!.freight_bill_id)
    }

    @Test
    fun `issue answers offline and the draft survives with no number`() = runTest {
        val ids = poolConsignmentIds(1)
        val draft = repository.buildDraftBill(deepak, ids, null, null, now).getOrNull()!!

        val issued = repository.issueBill(draft.localId, now + 1)
        assertTrue(issued is Result.Failure)
        assertEquals(ErrorCode.OFFLINE_UNAVAILABLE, (issued as Result.Failure).code)
        assertTrue((issued.message ?: "").contains("draft is saved"))

        val saved = database.billingDao().getBill(draft.localId)!!
        assertEquals("the draft is saved", "DRAFT", saved.state)
        assertNull("no number was consumed offline", saved.bill_no)
        assertNotNull("the consignment is still held by the draft", database.consignmentDao().getConsignment(ids[0])!!.freight_bill_id)
    }

    @Test
    fun `cancelling returns the consignments to the pool`() = runTest {
        val ids = poolConsignmentIds(2)
        val draft = repository.buildDraftBill(deepak, ids, null, null, now).getOrNull()!!

        repository.cancelBill(draft.localId, now + 1).getOrNull()!!

        assertEquals("CANCELLED", database.billingDao().getBill(draft.localId)!!.state)
        ids.forEach { id ->
            assertNull("back in the pool", database.consignmentDao().getConsignment(id)!!.freight_bill_id)
        }
        assertEquals("the pool is whole again", 23, repository.observeUnbilledPool(PoolFilter()).first().single().consignments)
    }

    @Test
    fun `a To Pay collection stamps the next receipt number with an explicit allocation`() = runTest {
        val before = database.outboxDao().getPendingCount()
        val receipt = repository.recordReceipt(
            payerPartyId = SeedIds.PARTY_NASHIK_HARDWARE,
            amountPaise = 241_000,
            instrument = "CASH",
            instrumentRef = null,
            allocations = listOf(AllocationInput(targetType = "TOPAY_CONSIGNMENT", consignmentId = "seed-consignment-4185", amountPaise = 241_000)),
            now = now,
        ).getOrNull()!!
        assertEquals("the seeded RCPT series continues at 129", "RCPT/IND/2627/00129", receipt.receiptNo)
        assertEquals(before + 2, database.outboxDao().getPendingCount()) // receipt + allocation

        val awaiting = repository.observeTopayAwaiting().first().map { it.displayNo }
        assertTrue("04185 left the collection list", "IND/2627/04185" !in awaiting)
    }

    @Test
    fun `the allocation may not exceed the receipt or the target`() = runTest {
        val over = repository.recordReceipt(deepak, 500, "CASH", null, listOf(AllocationInput(targetType = "ON_ACCOUNT", amountPaise = 600)), now)
        assertEquals("allocation > receipt refused", ErrorCode.MASTER_IN_USE, (over as Result.Failure).code)

        val overBill = repository.recordReceipt(
            deepak, 600_000, "NEFT", "SBIN0001",
            listOf(AllocationInput(targetType = "BILL", billId = "seed-bill-00298", amountPaise = 600_000)),
            now,
        )
        assertEquals("allocation > bill outstanding refused", ErrorCode.MASTER_IN_USE, (overBill as Result.Failure).code)
    }

    @Test
    fun `the statement reconciles to the rupee`() = runTest {
        val statement = repository.statement(deepak, 0, now, now).getOrNull()!!

        assertEquals("no documents precede the seeded ones", 0L, statement.openingPaise)
        assertEquals("two issued bills debit, one receipt credits", 3, statement.rows.size)
        val debits = statement.rows.sumOf { it.debitPaise }
        val credits = statement.rows.sumOf { it.creditPaise }
        assertEquals("debits are bills 00298 + 00311", 1_176_000L, debits)
        assertEquals("the credit is receipt 00128 on account", 5_000_000L, credits)
        assertEquals("closing = opening + Σ(debits − credits)", statement.openingPaise + debits - credits, statement.closingPaise)
        assertEquals("the overpayment lands as a credit balance", -3_824_000L, statement.closingPaise)
        assertTrue("the ledger runs chronologically", statement.rows.zipWithNext().all { (a, b) -> a.at <= b.at })
    }

    @Test
    fun `allocating a receipt shrinks the bill's outstanding`() = runTest {
        val before = repository.outstandingBillsForParty(deepak).first { it.billNo == "FB/IND/2627/00298" }
        assertEquals(546_000L, before.outstandingPaise)

        repository.recordReceipt(
            deepak, 100_000, "CASH", null,
            listOf(AllocationInput(targetType = "BILL", billId = before.localId, amountPaise = 100_000)),
            now,
        ).getOrNull()!!

        val after = repository.outstandingBillsForParty(deepak).first { it.billNo == "FB/IND/2627/00298" }
        assertEquals("546,000 − 100,000", 446_000L, after.outstandingPaise)
    }

    @Test
    fun `the waiver writes an audit row and unblocks a clerk's delivery`() = runTest {
        val bilty = "IND/2627/04185" // seeded To Pay, Held at Indore

        // Arrive first (Held → Arrived is legal) with a POD in place, so the money gate is
        // the only gate left; a clerk is then refused for the uncollected To Pay.
        statusRepository.recordPod(bilty, "Nashik Hardware Mart", null, null, null, now)
        sessionFlow.value = clerkSession()
        assertTrue(
            statusRepository.append(
                com.example.transportapp.data.transport.tracking.NewStatusEvent(biltyNo = bilty, eventType = "ARRIVED"),
                now,
            ).isSuccess(),
        )
        val refused = statusRepository.append(
            com.example.transportapp.data.transport.tracking.NewStatusEvent(biltyNo = bilty, eventType = "DELIVERED"),
            now,
        )
        assertEquals(ErrorCode.TOPAY_UNCOLLECTED, (refused as Result.Failure).code)

        // The Manager waiver is an append-only audit event.
        sessionFlow.value = ownerSession()
        val waived = statusRepository.waiveTopPay(bilty, "Shortage settled with the party at the counter", now + 1)
        assertTrue(waived.isSuccess())
        val events = database.consignmentDao().getEvents("seed-consignment-4185")
        val audit = events.last { it.event_type == StatusRepositoryImpl.WAIVE_EVENT_TYPE }
        assertEquals("MANAGER_WAIVER", audit.reason_code)
        assertEquals("Mahesh Patidar", audit.actor_name)
        assertEquals("the projection only moved by the Arrived event", "ARRIVED", database.consignmentDao().getConsignment("seed-consignment-4185")!!.status_projection)

        // Now the clerk's delivery passes the money gate.
        sessionFlow.value = clerkSession()
        val delivered = statusRepository.append(
            com.example.transportapp.data.transport.tracking.NewStatusEvent(biltyNo = bilty, eventType = "DELIVERED"),
            now + 2,
        )
        assertTrue(delivered.isSuccess())
        assertEquals("DELIVERED", database.consignmentDao().getConsignment("seed-consignment-4185")!!.status_projection)
    }

    @Test
    fun `a held consignment is not collectable until waived`() = runTest {
        val rows = repository.observeTopayAwaiting().first()
        val held = rows.first { it.displayNo == "IND/2627/04185" }
        assertTrue("04185 is Held", held.status == "HELD")
        assertTrue("and not yet waived", !held.collectable)

        statusRepository.waiveTopPay("IND/2627/04185", "Settled in person", now)
        val after = repository.observeTopayAwaiting().first().first { it.displayNo == "IND/2627/04185" }
        assertTrue("waived means collectable", after.collectable)
    }

    private suspend fun insertTbbConsignment(biltyNo: String, placeOfSupply: String): String {
        val route = database.mastersDao().getRoute(SeedIds.ROUTE_INDORE_NASHIK)!!
        val id = "cn-$biltyNo"
        database.consignmentDao().upsertConsignment(
            ConsignmentEntity(
                local_id = id, server_id = null, updated_at_local = 1, updated_at_server = null,
                sync_state = SyncState.SYNCED, deleted_at = null, company_id = company,
                series_id = SeedIds.SERIES_BILTY_INDORE, bilty_no = biltyNo, provisional_no = null,
                status_projection = "BOOKED", booking_branch_id = branch, dest_branch_id = null,
                consignor_id = deepak, consignee_id = SeedIds.PARTY_NASHIK_HARDWARE,
                route_id = SeedIds.ROUTE_INDORE_NASHIK,
                from_station_id = route.origin_station_id, to_station_id = route.dest_station_id,
                payment_mode = "TBB", risk = "OWNERS", delivery_type = "GODOWN",
                place_of_supply_state = placeOfSupply, eway_bill_no = null, private_mark = null,
                packages = 1, actual_weight_g = 1000, chargeable_weight_g = 1000,
                declared_value_paise = 0, freight_paise = 100_000, gst_paise = 5_000, total_paise = 105_000,
                booked_at = now - 10L * BillingRepositoryImpl.DAY_MS, booked_by_name = "Seeder",
                expected_arrival = now, party_names = "Deepak Steel Traders; Nashik Hardware Mart",
                freight_bill_id = null, amends_id = null, amendment_reason = null,
            ),
        )
        return id
    }
}
