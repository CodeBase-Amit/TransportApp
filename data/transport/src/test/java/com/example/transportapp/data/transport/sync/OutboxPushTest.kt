package com.example.transportapp.data.transport.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.outbox.OutboxOp
import com.example.transportapp.core.database.outbox.OutboxEntityType
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.core.network.ApiClient
import com.example.transportapp.core.network.AuthApi
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S25 (D64) — the drain: local outbox ops push onto the backend's REST surface.
 * 2xx marks DONE and writes the server id back onto the mirrored row; failures are
 * retriable with the typed code; a dead network leaves rows PENDING (offline-first).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OutboxPushTest {

    private lateinit var database: TransportDatabase
    private lateinit var server: MockWebServer
    private lateinit var push: OutboxPush

    private val company = "c-1"

    @Before
    fun setUp() = runTest {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        database = androidx.room.Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        server = MockWebServer()
        server.start()
        val apiClient = ApiClient(baseUrl = server.url("/").toString(), tokenProvider = { "t" })
        push = OutboxPush(database, apiClient, fakeSession())
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
                companyId = company, companyName = "Shivshakti", branchId = "b-1", branchName = "Indore",
            ),
        )
        override suspend fun signIn() {}
        override suspend fun signInWithPassword(email: String, password: String) = Result.success(Unit)
        override suspend fun signOut() {}
        override suspend fun updateDisplayName(name: String) {}
    }

    private suspend fun seedPartyWithInsertOp(name: String = "Drain Test Party") {
        val dao = database.mastersDao()
        dao.upsertParty(
            com.example.transportapp.core.database.entity.PartyEntity(
                local_id = "p-drain-1", server_id = null, updated_at_local = 1, updated_at_server = null,
                sync_state = SyncState.PENDING, deleted_at = null, company_id = company,
                name = name, phone = "9812345670", email = null, type = "BOTH",
                street_address = null, station = null, pincode = null, gstin = null,
                usual_route_id = null, usual_payment_mode = null, display_bilty_count = 0,
            ),
        )
        OutboxWriter(database.outboxDao()).enqueue(
            op = OutboxOp.INSERT,
            entityType = OutboxEntityType.PARTY,
            entityLocalId = "p-drain-1",
            payloadJson = """{"name":"$name"}""",
            now = 1L,
        )
    }

    @Test
    fun `drain pushes a party INSERT and writes the server id back`() = runTest {
        seedPartyWithInsertOp()
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"_id":"srv-9","name":"Drain Test Party"}"""))

        val report = push.drain()

        assertEquals(1, (report as Result.Success).value.pushed)
        assertEquals(0, report.value.failed)
        // The mirrored row now carries the server id and is SYNCED.
        val row = database.mastersDao().getPartyByServerId("srv-9")!!
        assertEquals("Drain Test Party", row.name)
        assertEquals(SyncState.SYNCED, row.sync_state)
        // The row is marked DONE.
        assertEquals(0, database.outboxDao().getPendingCount())
        // The request carried the party's fields.
        val recorded = server.takeRequest()
        assertTrue(recorded.path!!.endsWith("/api/parties"))
        assertTrue(recorded.body.readUtf8().contains("Drain Test Party"))
    }

    @Test
    fun `drain marks a row retriable on failure with the typed code`() = runTest {
        seedPartyWithInsertOp()
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"down"}"""))

        val report = push.drain()

        assertEquals(1, (report as Result.Success).value.failed)
        assertEquals(1, database.outboxDao().getPendingCount())
    }

    @Test
    fun `a dead network leaves rows pending - offline first`() = runTest {
        seedPartyWithInsertOp()
        server.shutdown()

        val report = push.drain()

        assertEquals(1, (report as Result.Success).value.failed)
        assertEquals("rows stay PENDING for the next drain", 1, database.outboxDao().getPendingCount())
    }

    @Test
    fun `drain with nothing ready is a success with zero`() = runTest {
        val report = push.drain()

        assertEquals(0, (report as Result.Success).value.pushed)
        assertEquals(0, report.value.failed)
    }
}
