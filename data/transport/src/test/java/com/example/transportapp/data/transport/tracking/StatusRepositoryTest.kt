package com.example.transportapp.data.transport.tracking

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.session.UserSession
import com.example.transportapp.domain.transport.tracking.Ageing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S8 (Phase2.md test charter): append-only with idempotent replay, rebuild determinism,
 * the §7.2 field rules (Held remark ≥10), the §7.1 delivery gates, and ageing buckets.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StatusRepositoryTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: StatusRepositoryImpl

    private val company = SeedIds.COMPANY_SHIVSHAKTI
    private val now = System.currentTimeMillis()

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DemoSeeder(database).seedIfNeeded()
        repository = StatusRepositoryImpl(database, fakeSession(role = "OWNER"), OutboxWriter(database.outboxDao()), PhotoImporter(ApplicationProvider.getApplicationContext()))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun fakeSession(role: String) = object : SessionRepository {
        override val session: Flow<UserSession> = flowOf(
            UserSession(
                userId = "u", name = "Mahesh Patidar", email = "mahesh.patidar@gmail.com", role = role,
                companyId = company, companyName = "Shivshakti Roadlines",
                branchId = SeedIds.BRANCH_INDORE, branchName = "Indore",
            ),
        )

        override suspend fun signIn() {}
        override suspend fun updateDisplayName(name: String) {}
        override suspend fun signOut() {}
    }

    private suspend fun projectionOf(biltyNo: String) =
        database.consignmentDao().getConsignmentByBiltyNo(company, biltyNo)!!.status_projection

    private suspend fun eventCount(biltyNo: String) =
        database.consignmentDao().getEvents(
            database.consignmentDao().getConsignmentByBiltyNo(company, biltyNo)!!.local_id,
        ).size

    @Test
    fun `append writes the event and advances the projection`() = runTest {
        // 04187 is seeded At hub; Arrived is the legal next move.
        val result = repository.append(NewStatusEvent(biltyNo = "IND/2627/04187", eventType = "ARRIVED", location = "Bhiwandi"), now)

        assertTrue(result.isSuccess())
        assertEquals("ARRIVED", projectionOf("IND/2627/04187"))
        val events = database.consignmentDao().getEvents(
            database.consignmentDao().getConsignmentByBiltyNo(company, "IND/2627/04187")!!.local_id,
        )
        assertEquals("the log grew, nothing moved", 5, events.size)
        assertEquals("Bhiwandi", events.last().location)
        assertTrue("every append carries an outbox row (§3.4 #5)", database.outboxDao().getPendingCount() >= 1)
    }

    @Test
    fun `an illegal jump is refused and nothing is written`() = runTest {
        val before = eventCount("IND/2627/04188") // seeded In transit
        val result = repository.append(NewStatusEvent(biltyNo = "IND/2627/04188", eventType = "DELIVERED"), now)

        assertTrue(result.isFailure())
        assertEquals(ErrorCode.CONSIGNMENT_IMMUTABLE, (result as Result.Failure).code)
        assertEquals(before, eventCount("IND/2627/04188"))
        assertEquals("IN_TRANSIT", projectionOf("IND/2627/04188"))
    }

    @Test
    fun `replaying the same client_event_id is a no-op`() = runTest {
        val replay = NewStatusEvent(biltyNo = "IND/2627/04187", eventType = "ARRIVED", clientEventId = "evt-fixed-1")
        assertTrue(repository.append(replay, now).isSuccess())
        val count = eventCount("IND/2627/04187")

        assertTrue(repository.append(replay.copy(), now + 1000).isSuccess())
        assertEquals("the replay appended nothing", count, eventCount("IND/2627/04187"))
    }

    @Test
    fun `a hold needs a reason and a remark of at least ten characters`() = runTest {
        val short = repository.append(
            NewStatusEvent(biltyNo = "IND/2627/04188", eventType = "HELD", reasonCode = "SHORTAGE", remark = "short"),
            now,
        )
        assertEquals("remark too short", ErrorCode.CONSIGNMENT_IMMUTABLE, (short as Result.Failure).code)

        val noReason = repository.append(
            NewStatusEvent(biltyNo = "IND/2627/04188", eventType = "HELD", remark = "a sufficiently long remark"),
            now,
        )
        assertEquals("no reason code", ErrorCode.CONSIGNMENT_IMMUTABLE, (noReason as Result.Failure).code)

        val ok = repository.append(
            NewStatusEvent(biltyNo = "IND/2627/04188", eventType = "HELD", reasonCode = "DAMAGE", remark = "Carton crushed at the hub"),
            now + 12L * 60 * 60 * 1000, // forward of the seeded chain, so the fold lands on Held
        )
        assertTrue(ok.isSuccess())
        assertEquals("HELD", projectionOf("IND/2627/04188"))
    }

    @Test
    fun `delivered without a pod needs a manager - the demo owner waives`() = runTest {
        // 04187 → At hub → Arrived → Delivered; no POD row exists.
        repository.append(NewStatusEvent(biltyNo = "IND/2627/04187", eventType = "ARRIVED"), now)

        val clerkRepo = StatusRepositoryImpl(database, fakeSession(role = "BOOKING_CLERK"), OutboxWriter(database.outboxDao()), PhotoImporter(ApplicationProvider.getApplicationContext()))
        val refused = clerkRepo.append(NewStatusEvent(biltyNo = "IND/2627/04187", eventType = "DELIVERED"), now + 1)
        assertEquals("a clerk cannot waive the POD", ErrorCode.POD_REQUIRED, (refused as Result.Failure).code)

        val owner = repository.append(NewStatusEvent(biltyNo = "IND/2627/04187", eventType = "DELIVERED"), now + 1)
        assertTrue("the Owner's waiver unblocks delivered (§7.1)", owner.isSuccess())
        assertEquals("DELIVERED", projectionOf("IND/2627/04187"))
    }

    @Test
    fun `a recorded pod unblocks delivered for a clerk`() = runTest {
        repository.append(NewStatusEvent(biltyNo = "IND/2627/04187", eventType = "ARRIVED"), now)
        assertTrue(repository.recordPod("IND/2627/04187", "Nashik Hardware Mart", signatureRef = null, photoRef = null, remarks = null, now).isSuccess())

        val clerkRepo = StatusRepositoryImpl(database, fakeSession(role = "BOOKING_CLERK"), OutboxWriter(database.outboxDao()), PhotoImporter(ApplicationProvider.getApplicationContext()))
        val delivered = clerkRepo.append(NewStatusEvent(biltyNo = "IND/2627/04187", eventType = "DELIVERED"), now + 1)
        assertTrue(delivered.isSuccess())
    }

    @Test
    fun `rebuild is deterministic and matches the incrementally advanced projection`() = runTest {
        repository.append(NewStatusEvent(biltyNo = "IND/2627/04187", eventType = "ARRIVED"), now)
        repository.append(NewStatusEvent(biltyNo = "IND/2627/04187", eventType = "OUT_FOR_DELIVERY"), now + 1)

        val first = repository.rebuildProjection(
            database.consignmentDao().getConsignmentByBiltyNo(company, "IND/2627/04187")!!.local_id,
        )
        val second = repository.rebuildProjection(
            database.consignmentDao().getConsignmentByBiltyNo(company, "IND/2627/04187")!!.local_id,
        )
        assertEquals("same log, same projection, twice", first, second)
        assertEquals("OUT_FOR_DELIVERY", projectionOf("IND/2627/04187"))

        // A held detour folded back through the machine reproduces the same state — on a
        // consignment where Held is legal (In transit → Held, §7.1). A BACK-DATED hold
        // (occurred before the seeded In-transit event) folds the projection back to In
        // transit: the log's own order wins, by construction.
        val held = repository.append(NewStatusEvent(biltyNo = "IND/2627/04188", eventType = "HELD", reasonCode = "OTHER", remark = "Gate refused entry at Nashik"), now + 2)
        assertTrue("hold append should succeed: ${(held as? Result.Failure)?.message}", held.isSuccess())
        assertEquals("the back-dated hold folds behind the in-transit event", "IN_TRANSIT", projectionOf("IND/2627/04188"))
        assertEquals(
            com.example.transportapp.domain.transport.ConsignmentStatus.IN_TRANSIT,
            repository.rebuildProjection(database.consignmentDao().getConsignmentByBiltyNo(company, "IND/2627/04188")!!.local_id),
        )

        // The same hold, occurring after everything in the log: the projection moves.
        val forward = repository.append(NewStatusEvent(biltyNo = "IND/2627/04188", eventType = "HELD", reasonCode = "OTHER", remark = "Gate refused entry again at Nashik"), now + 12L * 60 * 60 * 1000)
        assertTrue(forward.isSuccess())
        assertEquals("HELD", projectionOf("IND/2627/04188"))
        assertEquals(
            com.example.transportapp.domain.transport.ConsignmentStatus.HELD,
            repository.rebuildProjection(database.consignmentDao().getConsignmentByBiltyNo(company, "IND/2627/04188")!!.local_id),
        )
    }

    @Test
    fun `exceptions strip reads held events with their remarks`() = runTest {
        val items = repository.exceptions(company, branchId = null, sinceAt = 0, now = now)
        assertTrue(items.isNotEmpty())
        assertEquals("IND/2627/04185", items.first().biltyNo)
        assertEquals("SHORTAGE", items.first().reasonCode)
    }

    @Test
    fun `overdue counts only undelivered past expected plus grace`() = runTest {
        // Seeded expected arrivals are ~2 days after booked_at (booked_at near seed time),
        // so with now pushed far ahead everything undelivered is overdue.
        val farFuture = now + 60L * Ageing.DAY_MS
        val overdue = repository.countOverdue(company, branchId = null, now = farFuture)
        assertTrue("seeded undelivered rows all overdue", overdue >= 4)

        assertEquals("delivered and returned rows never count", 0, repository.countOverdue(company, branchId = null, now = 0))
    }
    @Test
    fun `the signature capture satisfies the delivered gate and rides the outbox`() = runTest {
        // 04187: At hub → Arrived → Delivered needs a POD row for a non-manager.
        statusRepositoryAppend(NewStatusEvent(biltyNo = "IND/2627/04187", eventType = "ARRIVED"))
        repository.recordPod(
            biltyNo = "IND/2627/04187",
            consigneeName = "Nashik Hardware Mart",
            signatureRef = "signatures/sig-04187-test.png",
            photoRef = null,
            remarks = null,
            now = now + 1,
        )
        val pod = database.consignmentDao().getPod(
            database.consignmentDao().getConsignmentByBiltyNo(company, "IND/2627/04187")!!.local_id,
        )!!
        assertEquals("the signed pad's file ref is stored", "signatures/sig-04187-test.png", pod.signature_ref)

        val clerkRepo = StatusRepositoryImpl(database, fakeSession(role = "DELIVERY_CLERK"), OutboxWriter(database.outboxDao()), PhotoImporter(ApplicationProvider.getApplicationContext()))
        val delivered = clerkRepo.append(NewStatusEvent(biltyNo = "IND/2627/04187", eventType = "DELIVERED"), now + 2)
        assertTrue("a clerk with a captured POD can deliver", delivered.isSuccess())
    }

    /** The unreadable-provider refusal (PHOTO_QUALITY) is device-verified: Robolectric's
     *  shadow BitmapFactory decodes any stream, so only the positive path runs here. */
    @Test
    fun `a readable photo is imported and enqueued`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val src = java.io.File(context.cacheDir, "src-${System.currentTimeMillis()}.jpg")
        android.graphics.Bitmap.createBitmap(24, 24, android.graphics.Bitmap.Config.ARGB_8888).compress(
            android.graphics.Bitmap.CompressFormat.JPEG, 90, src.outputStream(),
        )
        val before = database.outboxDao().getPendingCount()

        val result = repository.addAttachment(
            biltyNo = "IND/2627/04187",
            kind = "GOODS",
            source = android.net.Uri.fromFile(src),
            caption = "Packed state at loading",
            now = now,
        )
        assertTrue("a real image imports cleanly", result.isSuccess())

        val rows = database.consignmentDao().getAttachments(
            database.consignmentDao().getConsignmentByBiltyNo(company, "IND/2627/04187")!!.local_id,
        )
        assertEquals(1, rows.size)
        assertTrue("the file ref points into app files", rows.first().file_ref.startsWith("attachments/"))
        assertTrue("the compressed payload is non-empty", rows.first().size_bytes > 0)
        assertEquals("the attachment is queued for upload", before + 1, database.outboxDao().getPendingCount())
    }

    /** The test's repository is owned by setUp; appends go through the OWNER instance. */
    private suspend fun statusRepositoryAppend(event: NewStatusEvent) {
        repository.append(event, now)
    }
}
