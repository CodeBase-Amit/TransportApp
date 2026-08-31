package com.example.transportapp.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.database.outbox.OutboxDao
import com.example.transportapp.core.database.outbox.OutboxEntity
import com.example.transportapp.core.database.outbox.OutboxEntityType
import com.example.transportapp.core.database.outbox.OutboxOp
import com.example.transportapp.core.database.outbox.OutboxPrereqEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Outbox readiness (Phase2.md S1 tests): a party created offline must reach the server
 * before the consignment that references it (TransportApp.md §16.2) — readiness is
 * dependency-aware, and ordering is oldest-first.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OutboxDaoTest {

    private lateinit var database: TransportDatabase
    private lateinit var dao: OutboxDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.outboxDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun row(clientOpId: String, createdAt: Long, nextAttemptAt: Long = 0) = OutboxEntity(
        client_op_id = clientOpId,
        op = OutboxOp.INSERT,
        entity_type = OutboxEntityType.CONSIGNMENT,
        entity_local_id = clientOpId,
        payload_json = "{}",
        next_attempt_at = nextAttemptAt,
        created_at = createdAt,
    )

    @Test
    fun `row with a pending prerequisite is not ready`() = runTest {
        val parentId = dao.upsertRow(row("party-1", createdAt = 1))
        val childId = dao.upsertRow(row("cons-1", createdAt = 2))
        dao.upsertPrereqs(listOf(OutboxPrereqEntity(outbox_id = childId, client_op_id = "party-1")))

        val ready = dao.getReady(now = 10)

        assertEquals(listOf("party-1"), ready.map { it.client_op_id })
        assertEquals(2, dao.getPendingCount())
    }

    @Test
    fun `row becomes ready once its prerequisite is done`() = runTest {
        val parentId = dao.upsertRow(row("party-1", createdAt = 1))
        val childId = dao.upsertRow(row("cons-1", createdAt = 2))
        dao.upsertPrereqs(listOf(OutboxPrereqEntity(outbox_id = childId, client_op_id = "party-1")))

        dao.markDone(listOf(parentId))

        val ready = dao.getReady(now = 10)
        assertEquals(listOf("cons-1"), ready.map { it.client_op_id })
    }

    @Test
    fun `drain order is oldest first`() = runTest {
        dao.upsertRow(row("third", createdAt = 3))
        dao.upsertRow(row("first", createdAt = 1))
        dao.upsertRow(row("second", createdAt = 2))

        val ready = dao.getReady(now = 10)

        assertEquals(listOf("first", "second", "third"), ready.map { it.client_op_id })
    }

    @Test
    fun `row is not ready before its next attempt time`() = runTest {
        dao.upsertRow(row("backoff", createdAt = 1, nextAttemptAt = 100))

        assertEquals(emptyList<OutboxEntity>(), dao.getReady(now = 99))
        assertEquals(listOf("backoff"), dao.getReady(now = 100).map { it.client_op_id })
    }

    @Test
    fun `same client op id is idempotent by replace`() = runTest {
        dao.upsertRow(row("op-1", createdAt = 1))
        dao.upsertRow(row("op-1", createdAt = 1))

        assertEquals(1, dao.getPendingCount())
    }
}
