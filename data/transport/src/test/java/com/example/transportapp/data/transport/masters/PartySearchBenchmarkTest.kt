package com.example.transportapp.data.transport.masters

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.entity.PartyEntity
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.seed.DemoSeeder
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
 * S14 (Phase 3 plan): the §8 party-search budget measured as a fail-the-build test —
 * bounded LIKE over 5,000+ parties must answer inside 120 ms (§17.5's "party search
 * as-you-type"). Two warmups, then five measured runs; the max must fit the budget. This
 * is the benchmark D7's LIKE-vs-FTS decision leans on: as long as it passes, LIKE stays.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PartySearchBenchmarkTest {

    private lateinit var database: TransportDatabase
    private val company = SeedIds.COMPANY_SHIVSHAKTI
    private val fillerCount = 5_000

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DemoSeeder(database).seedIfNeeded()
        // The §B6 seed carries 1,284 parties; pad to 5,000+ with realistic filler names
        // across the same surname distribution the search sees in production.
        val dao = database.mastersDao()
        val surnames = listOf("Sharma", "Patel", "Verma", "Yadav", "Gupta", "Agarwal", "Bansal", "Jain", "Rathore", "Singh")
        val suffixes = listOf("Traders", "Transport", "Logistics", "Roadlines", "Carriers")
        for (i in 0 until fillerCount) {
            dao.upsertParty(
                PartyEntity(
                    local_id = "bench-party-$i", server_id = null, updated_at_local = 1, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, company_id = company,
                    name = "${surnames[i % surnames.size]} ${suffixes[(i / surnames.size) % suffixes.size]} ${"%04d".format(i)}",
                    phone = "+91 9%08d".format(70_000_000 + i),
                    email = null, type = "BOTH",
                    street_address = null, station = null, pincode = null, gstin = null,
                    usual_route_id = null, usual_payment_mode = null, display_bilty_count = 0,
                ),
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun timedSearch(pattern: String): Pair<Long, Int> {
        val start = System.nanoTime()
        val rows = database.mastersDao().searchParties(company, pattern).first()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000L
        return elapsedMs to rows.size
    }

    @Test
    fun `party search answers inside the 120 ms budget at 5k parties`() = runTest {
        val dao = database.mastersDao()
        val totalCount = dao.searchParties(company, "%").first().size
        assertTrue("the benchmark needs 5,000+ parties, found $totalCount", totalCount >= 5_000)

        // Warmups (JIT + connection pool), then five measured runs on two query shapes.
        timedSearch("%Sharma%")
        timedSearch("%Traders 00%")

        var worst = 0L
        repeat(5) {
            val (msName, nameHits) = timedSearch("%Sharma Traders%")
            val (msPrefix, prefixHits) = timedSearch("%Traders 00%")
            assertTrue("name search hit nothing", nameHits > 0)
            assertTrue("prefix search hit nothing", prefixHits > 0)
            worst = maxOf(worst, msName, msPrefix)
        }
        assertTrue(
            "party search took ${worst}ms at $totalCount parties — budget is 120ms; revisit D7 (FTS) if this fails",
            worst <= 120L,
        )
        assertEquals("the §B6 seed parties are in the search space", 6_284, totalCount)
    }
}
