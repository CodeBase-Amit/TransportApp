package com.example.transportapp.data.transport.outbox

import com.example.transportapp.core.database.outbox.OutboxDao
import com.example.transportapp.core.database.outbox.OutboxEntity
import com.example.transportapp.core.database.outbox.OutboxEntityType
import com.example.transportapp.core.database.outbox.OutboxOp
import com.example.transportapp.core.database.outbox.OutboxPrereqEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enqueues outbox rows for a write (Phase2.md §4.2). MUST be called inside the caller's
 * Room transaction so the entity row and its outbox row commit atomically.
 *
 * `clientOpId` is one UUID per user action — pass the same value on retries (Spec.md §6.3).
 * `prerequisites` carries the `client_op_id`s this operation depends on, so the dependency-
 * aware drain never sends a consignment before the party it references.
 */
@Singleton
class OutboxWriter @Inject constructor(
    private val outboxDao: OutboxDao,
) {

    suspend fun enqueue(
        op: OutboxOp,
        entityType: OutboxEntityType,
        entityLocalId: String,
        payloadJson: String,
        prerequisites: List<String> = emptyList(),
        clientOpId: String = UUID.randomUUID().toString(),
        now: Long = System.currentTimeMillis(),
    ): String {
        val rowId = outboxDao.upsertRow(
            OutboxEntity(
                client_op_id = clientOpId,
                op = op,
                entity_type = entityType,
                entity_local_id = entityLocalId,
                payload_json = payloadJson,
                next_attempt_at = now,
                created_at = now,
            ),
        )
        if (prerequisites.isNotEmpty()) {
            outboxDao.upsertPrereqs(prerequisites.map { OutboxPrereqEntity(outbox_id = rowId, client_op_id = it) })
        }
        return clientOpId
    }
}
