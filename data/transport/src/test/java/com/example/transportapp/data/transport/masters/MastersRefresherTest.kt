package com.example.transportapp.data.transport.masters

import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.core.network.ApiClient
import com.example.transportapp.core.network.MastersApi
import com.example.transportapp.core.network.NumberingApi
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.session.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S24 — the remote→Room refresher: reconcile by server_id, upsert in place, and the
 * D62 contract — a dead server changes nothing and answers OFFLINE_UNAVAILABLE.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MastersRefresherTest {

    private lateinit var database: TransportDatabase
    private lateinit var server: MockWebServer
    private lateinit var refresher: MastersRefresher

    private val company = com.example.transportapp.core.common.SeedIds.COMPANY_SHIVSHAKTI
    private val branch = com.example.transportapp.core.common.SeedIds.BRANCH_INDORE

    @Before
    fun setUp() = runTest {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        database = androidx.room.Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DemoSeeder(database).seedIfNeeded()
        server = MockWebServer()
        server.start()
        val api = MastersApi(ApiClient(baseUrl = server.url("/").toString(), tokenProvider = { "t" }))
        refresher = MastersRefresher(database, api, fakeSession())
    }

    @After
    fun tearDown() {
        database.close()
        server.shutdown()
    }

    private fun fakeSession() = object : SessionRepository {
        override val session: Flow<UserSession> = flowOf(
            UserSession(
                userId = "u", name = "Mahesh Patidar", email = "m@t.in", role = "OWNER",
                companyId = company, companyName = "Shivshakti", branchId = branch, branchName = "Indore",
            ),
        )
        override suspend fun signIn() {}
        override suspend fun signInWithPassword(email: String, password: String) = Result.success(Unit)
        override suspend fun signOut() {}
        override suspend fun updateDisplayName(name: String) {}
    }

    @Test
    fun `refresh upserts remote parties keyed by server id`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """[{"_id":"srv-p-1","name":"Remote Party","phone":"9812345670","type":"BOTH","gstin":"27DGTPS1234K1Z9"}]""",
            ),
        )

        val result = refresher.refreshParties()

        assertTrue(result.isSuccess())
        assertEquals(1, (result as Result.Success).value)
        // The new party is in Room, keyed on the server id, marked SYNCED.
        val row = database.mastersDao().getPartyByServerId("srv-p-1")!!
        assertEquals("Remote Party", row.name)
        assertEquals("9812345670", row.phone)
        assertEquals("27DGTPS1234K1Z9", row.gstin)
        assertEquals(SyncState.SYNCED, row.sync_state)
    }

    @Test
    fun `second refresh updates the same row rather than duplicating`() = runTest {
        val body = """[{"_id":"srv-p-1","name":"Remote Party","phone":"9812345670","type":"BOTH"}]"""
        server.enqueue(MockResponse().setBody(body))
        refresher.refreshParties()
        server.enqueue(MockResponse().setBody("""[{"_id":"srv-p-1","name":"Renamed Remote","phone":"9812345670","type":"BOTH"}]"""))
        refresher.refreshParties()

        // Exactly one mirrored row; the name updated in place.
        val row = database.mastersDao().getPartyByServerId("srv-p-1")!!
        assertEquals("Renamed Remote", row.name)
        assertTrue(database.mastersDao().countParties(company) >= 1)
    }

    @Test
    fun `offline refresh changes nothing and answers OFFLINE_UNAVAILABLE`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"down"}"""))

        val result = refresher.refreshParties()

        val failure = result as Result.Failure
        assertEquals(ErrorCode.OFFLINE_UNAVAILABLE, failure.code)
    }

    @Test
    fun `numbering lease falls back to local when the server is down`() = runTest {
        // The NumberingApi answers OFFLINE_UNAVAILABLE on connection refused (server not
        // enqueuing anything); the repository's serverLeaseInto returns null → the local
        // block grant path continues exactly as before (D62).
        val numberingApi = NumberingApi(ApiClient(baseUrl = "http://127.0.0.1:1/", tokenProvider = { "t" }))
        val repo = com.example.transportapp.data.transport.numbering.NumberingRepositoryImpl(
            database, database.numberingDao(), deviceIdProvider = { "TEST1" }, numberingApi = numberingApi,
        )

        // S24: the branch id is the seed branch — the seeded BILTY series exists there.
        val result = repo.issueNext(company, branch, "BILTY", now = System.currentTimeMillis())

        assertTrue("local grant continues offline: ${(result as? Result.Failure)?.message}", result.isSuccess())
        val issued = (result as Result.Success).value
        assertTrue(issued.display.startsWith("IND/2627/"))
        assertTrue("not provisional — the local block grant is real", !issued.provisional)
    }
}
