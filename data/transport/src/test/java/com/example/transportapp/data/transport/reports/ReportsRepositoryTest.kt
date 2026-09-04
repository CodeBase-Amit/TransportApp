package com.example.transportapp.data.transport.reports

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.session.UserSession
import com.example.transportapp.export.engine.CsvWriter
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
 * S10: the freight register reads the seed true — the §10.6 canonical row prints exactly and
 * the totals band equals the sum of the rows — and its CSV round-trips through the engine.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReportsRepositoryTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: ReportsRepositoryImpl
    private val company = SeedIds.COMPANY_SHIVSHAKTI
    private val branch = SeedIds.BRANCH_INDORE
    private val now = System.currentTimeMillis()

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DemoSeeder(database).seedIfNeeded()
        repository = ReportsRepositoryImpl(
            database,
            object : SessionRepository {
                override val session = flowOf(
                    UserSession(
                        userId = "u", name = "Mahesh Patidar", email = DemoSeeder.EMAIL_DEMO_USER, role = "OWNER",
                        companyId = company, companyName = "Shivshakti Roadlines", branchId = branch, branchName = "Indore",
                    ),
                )

                override suspend fun signIn() {}
        override suspend fun signInWithPassword(email: String, password: String) = com.example.transportapp.core.common.Result.success(Unit)
        override suspend fun updateDisplayName(name: String) {}
        override suspend fun signOut() {}
            },
            context,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `the freight register reads the seed and totals equal the rows`() = runTest {
        val (rows, totals) = repository.freightRegister(companyWide = true, from = 0, to = now)

        assertEquals("6 fixtures + 29 money consignments", 35, totals.rows)
        assertEquals(totals.packages, rows.sumOf { it.packages })
        assertEquals(totals.totalPaise, rows.sumOf { it.totalPaise })
        assertTrue("newest first", rows.zipWithNext().all { (a, b) -> a.bookedAt >= b.bookedAt })

        val canonical = rows.first { it.biltyNo == "IND/2627/04188" }
        assertEquals("Deepak Steel Traders", canonical.consignor)
        assertEquals("Nashik Hardware Mart", canonical.consignee)
        assertEquals(394_400L, canonical.totalPaise)
        assertEquals("Indore → Nashik", canonical.route)
    }

    @Test
    fun `branch scope narrows the register`() = runTest {
        val (all, _) = repository.freightRegister(companyWide = true, from = 0, to = now)
        val (indore, _) = repository.freightRegister(companyWide = false, from = 0, to = now)
        assertEquals("6 fixtures + 29 money cns", 35, all.size)
        assertEquals("only the 5 Indore fixtures — the money cns and 04184 book at Nagpur", 5, indore.size)
    }

    @Test
    fun `the register exports to the golden CSV shape`() = runTest {
        val (rows, totals) = repository.freightRegister(companyWide = true, from = 0, to = now)
        val csv = CsvWriter.writeBiltyRegister(repository.freightRegisterAsCsvRows(rows))

        val lines = csv.trimEnd().split("\r\n")
        assertEquals("header + one line per row", totals.rows + 1, lines.size)
        assertTrue(lines[0].startsWith("Bilty no.,Date,Branch,"))
        val canonical = lines.first { it.contains("IND/2627/04188") }
        assertTrue("money prints in two decimals", canonical.contains("3944.00"))
        assertTrue(canonical.endsWith(",OK"))
    }

    @Test
    fun `hub entries carry cached figures`() = runTest {
        val entries = repository.hubEntries(now)
        assertEquals(14, entries.size)
        assertEquals("Freight register", entries.first().label)
        assertTrue("the freight headline is a real figure, not a placeholder", entries.first().headline!!.length > 4)
        val continuity = entries.first { it.id == "number_continuity" }
        assertEquals("no cancellations on the fresh seed", "No gaps", continuity.headline)
    }
}
