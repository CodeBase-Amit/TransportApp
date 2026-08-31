package com.example.transportapp.data.transport

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.outbox.OutboxEntityType
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.core.datastore.context.ActiveContextStore
import com.example.transportapp.core.datastore.session.SessionStore
import com.example.transportapp.data.transport.masters.MastersRepository
import com.example.transportapp.data.transport.masters.MastersRepositoryImpl
import com.example.transportapp.data.transport.outbox.OutboxWriter
import kotlinx.coroutines.flow.first
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
 * S3 repository tests (Phase2.md §7): offline search, merge integrity (refs moved, loser
 * tombstoned, outbox rows), MASTER_IN_USE refusal, and rate-row persistence.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MastersRepositoryTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: MastersRepository

    private val companyId = SeedIds.COMPANY_SHIVSHAKTI

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MastersRepositoryImpl(
            database = database,
            mastersDao = database.mastersDao(),
            orgDao = database.orgDao(),
            outboxWriter = OutboxWriter(database.outboxDao()),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `created party is found by search as-you-type`() = runTest {
        DemoSeeder(database).seedIfNeeded()
        val create = repository.createOrUpdateParty(
            companyId = companyId, localId = null, name = "Sharma Traders", phone = "+91 99999 12345",
            email = null, street = null, station = "Indore", pincode = null, gstin = null,
            type = "BOTH", usualRouteId = null, usualPaymentMode = null,
        )
        assertTrue(create.isSuccess())

        val results = repository.observeParties(companyId, query = "Sharma Tra", letter = "", duplicatesOnly = false).first()
        assertTrue("expected Sharma Traders in ${results.size} matches", results.any { it.name == "Sharma Traders" })
    }

    @Test
    fun `merge moves rate rows tombstones the loser and enqueues outbox rows`() = runTest {
        DemoSeeder(database).seedIfNeeded()
        val outboxBefore = database.outboxDao().getPendingCount()

        val result = repository.mergeParties(SeedIds.PARTY_DEEPAK_STEEL, SeedIds.PARTY_DEEPAK_DUPLICATE)

        assertTrue(result.isSuccess())
        val loser = database.mastersDao().getParty(SeedIds.PARTY_DEEPAK_DUPLICATE)
        assertEquals(null, loser) // tombstoned
        assertEquals(43, database.mastersDao().getParty(SeedIds.PARTY_DEEPAK_STEEL)!!.display_bilty_count) // 41 + 2
        val rowsNow = database.mastersDao().getRateRowsForParty(SeedIds.PARTY_DEEPAK_DUPLICATE)
        assertEquals(0, rowsNow.size)
        // outbox delta = 1 tombstone (PARTY DELETE) + 1 kept-party update
        val outboxDelta = database.outboxDao().getPendingCount() - outboxBefore
        assertTrue("expected outbox rows for merge, got $outboxDelta", outboxDelta >= 2)
        val types = database.outboxDao().getReady(now = Long.MAX_VALUE).map { it.entity_type }.toSet()
        assertTrue(OutboxEntityType.PARTY in types)
    }

    @Test
    fun `deleting a referenced party is refused with MASTER_IN_USE copy`() = runTest {
        DemoSeeder(database).seedIfNeeded()
        val result = repository.deleteParty(SeedIds.PARTY_DEEPAK_STEEL)
        assertTrue(result.isFailure())
        val message = (result as com.example.transportapp.core.common.Result.Failure).message.orEmpty()
        assertTrue(message, message.contains("41 bilties") && message.contains("can't be deleted"))
    }

    @Test
    fun `rate rows for Deepak resolve with route and goods labels`() = runTest {
        DemoSeeder(database).seedIfNeeded()
        val rows = repository.rateRowsForParty(SeedIds.PARTY_DEEPAK_STEEL)
        assertEquals(12, rows.size)
        assertTrue(rows.any { it.routeLabel == "Indore – Nashik" && it.goodsLabel == "MS pipes" })
        assertEquals(450L, rows.first().ratePaise) // ₹4.50
    }
}
