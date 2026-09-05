package com.example.transportapp.data.transport.settings

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.data.transport.account.SettingsRepository
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.session.UserSession
import com.example.transportapp.data.transport.tracking.PhotoImporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S27 regression for the §9 counter change (T28 Edit): ConfirmCounter resolved the branch
 * by parsing the series label, but the label carries the raw branch_id — so
 * branchIdForName("seed-branch-indore") matched nothing and every confirm failed with
 * "That branch could not be found". The repo must resolve the series by its local id
 * directly, no name parsing anywhere.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SeriesCounterResolutionTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: SettingsRepository
    private val company = SeedIds.COMPANY_SHIVSHAKTI
    private val branch = SeedIds.BRANCH_INDORE

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DemoSeeder(database).seedIfNeeded()
        val sessionFlow = kotlinx.coroutines.flow.MutableStateFlow(ownerSession())
        val sessions = object : SessionRepository {
            override val session = sessionFlow
            override suspend fun signIn() {}
            override suspend fun signInWithPassword(email: String, password: String) = com.example.transportapp.core.common.Result.success(Unit)
            override suspend fun updateDisplayName(name: String) {}
            override suspend fun signOut() {}
        }
        repository = SettingsRepository(
            orgDao = database.orgDao(),
            numberingDao = database.numberingDao(),
            sessionRepository = sessions,
            outboxWriter = OutboxWriter(database.outboxDao()),
            photoImporter = PhotoImporter(context),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun ownerSession() = UserSession(
        userId = "u", name = "Mahesh Patidar", email = DemoSeeder.EMAIL_DEMO_USER, role = "OWNER",
        companyId = company, companyName = "Shivshakti Roadlines", branchId = branch, branchName = "Indore",
    )

    @Test
    fun `the counter change resolves the series by local id and moves forward`() = runTest {
        val series = database.numberingDao().observeSeriesForCompany(company).first()
            .first { it.doc_type == "BILTY" && it.branch_id == branch }
        val before = series.last_issued

        val result = repository.changeSeriesCounterById(
            companyId = company,
            seriesLocalId = series.local_id,
            newLastIssued = before + 10,
        )

        assertTrue("confirm must succeed for a seeded series: ${result}", result.isSuccess())
        val after = database.numberingDao().observeSeriesForCompany(company).first()
            .first { it.local_id == series.local_id }
        assertEquals(before + 10, after.last_issued)
    }

    @Test
    fun `a missing series id fails with the series-not-found error`() = runTest {
        val result = repository.changeSeriesCounterById(company, "no-such-series", 999)
        assertTrue(result.isFailure())
        assertNotNull(result.toString(), result)
    }
}
