package com.example.transportapp.data.transport.numbering

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.entity.NumberSeriesEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S5 (Phase2.md test charter): numbering concurrency — n issues, no duplicates, no reuse,
 * monotonic; lease-boundary grants never overlap; exhaustion with grants unavailable
 * falls through to unique provisional numbers (§9).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NumberingRepositoryTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: NumberingRepositoryImpl

    private val company = "c-1"
    private val branch = "b-1"
    private val docType = "BILTY"

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NumberingRepositoryImpl(
            database,
            database.numberingDao(),
            deviceIdProvider = { "TEST1" },
        )
        database.numberingDao().upsertSeries(
            NumberSeriesEntity(
                local_id = "series-1", server_id = null, updated_at_local = 1, updated_at_server = null,
                sync_state = SyncState.SYNCED, deleted_at = null, company_id = company,
                branch_id = branch, doc_type = docType, prefix = "IND/2627/", fy_part = "2627",
                digits = 5, last_issued = 4188, reset_rule = "FINANCIAL_YEARLY",
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun displays(values: List<Result<IssuedNumber>>) = values.map { (it as Result.Success).value.display }

    @Test
    fun `issues proceed from the seeded high-water mark without gaps`() = runTest {
        val numbers = displays((1..3).map { repository.issueNext(company, branch, docType, now = 1_000L + it) })
        assertEquals(listOf("IND/2627/04189", "IND/2627/04190", "IND/2627/04191"), numbers)
        assertEquals(4191L, database.numberingDao().getSeries(company, branch, docType)!!.last_issued)
    }

    @Test
    fun `twenty interleaved issues are unique and monotonic`() = runTest {
        val results = (1..20).map { async { repository.issueNext(company, branch, docType, now = 1_000L + it) } }.awaitAll()
        val numbers = displays(results)
        assertEquals("no duplicates", numbers.size, numbers.toSet().size)
        assertEquals("no reuse, no gaps", (4189L..4208L).map { "IND/2627/%05d".format(it) }, numbers.sorted())
    }

    @Test
    fun `crossing the lease boundary grants a new non-overlapping lease`() = runTest {
        repeat(NumberingRepositoryImpl.LEASE_BLOCK_SIZE.toInt()) {
            repository.issueNext(company, branch, docType, now = 1_000L)
        }
        val next = repository.issueNext(company, branch, docType, now = 2_000L) as Result.Success
        assertEquals("51st issue crosses into a fresh grant", "IND/2627/04239", next.value.display)
        val leases = database.numberingDao().getLeasesForSeries("series-1")
        assertEquals(2, leases.size)
        val (first, second) = leases.sortedBy { it.range_start }
        assertTrue("no overlapping live leases", first.range_end < second.range_start)
        assertEquals(4189L, first.range_start)
        assertEquals(4238L, first.range_end)
        assertEquals(4239L, second.range_start)
    }

    @Test
    fun `exhaustion with grants unavailable falls through to unique provisional numbers`() = runTest {
        repository.debugShrinkActiveLease(company, branch, docType)
        repository.debugSetGrantsEnabled(false)

        val peek = repository.peekNext(company, branch, docType)!!
        assertTrue("the T5 banner state is visible before booking", peek.provisional)
        assertTrue(peek.display.startsWith("PROV-TEST1-"))

        val numbers = displays((1..3).map { repository.issueNext(company, branch, docType, now = 1_000L + it) })
        assertEquals(listOf("PROV-TEST1-000001", "PROV-TEST1-000002", "PROV-TEST1-000003"), numbers)

        repository.debugSetGrantsEnabled(true)
        val recovered = repository.issueNext(company, branch, docType, now = 5_000L) as Result.Success
        assertFalse(recovered.value.provisional)
        assertEquals("grants resume beyond every number ever touched", "IND/2627/04189", recovered.value.display)
    }

    @Test
    fun `a missing series is a typed error, not a crash`() = runTest {
        val result = repository.issueNext("other", branch, docType, now = 1_000L)
        assertTrue(result is Result.Failure)
        assertTrue(repository.peekNext("other", branch, docType) == null)
    }
}

