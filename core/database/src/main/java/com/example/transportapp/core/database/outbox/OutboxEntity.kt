package com.example.transportapp.core.database.outbox

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The outbox row (TransportApp.md §16.2): operation type, target entity and local id, a JSON
 * payload, a `client_op_id` for server-side idempotency, attempt bookkeeping and last-error code.
 * The drain order is dependency-aware (see [OutboxPrereqEntity]), not a flat list.
 *
 * Column names mirror the server-side contract deliberately (snake_case).
 */
@Entity(
    tableName = "outbox",
    indices = [
        Index(value = ["client_op_id"], unique = true),
        Index(value = ["state", "next_attempt_at"]),
        Index(value = ["entity_type", "entity_local_id"]),
    ],
)
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** One UUID per user action; retried requests reuse it (`DUP_CLIENT_OP` is a no-op, §0.3). */
    val client_op_id: String,
    val op: OutboxOp,
    val entity_type: OutboxEntityType,
    val entity_local_id: String,
    val payload_json: String,
    val state: OutboxState = OutboxState.PENDING,
    val attempt_count: Int = 0,
    /** Epoch millis; backoff writes a future value on failure. */
    val next_attempt_at: Long = 0,
    val last_error_code: String? = null,
    val created_at: Long,
)

/**
 * A declared prerequisite: this row must not drain before the outbox row carrying
 * `client_op_id` has drained. Example — a party created offline must reach the server
 * before the consignment that references it (TransportApp.md §16.2).
 */
@Entity(
    tableName = "outbox_prereq",
    primaryKeys = ["outbox_id", "client_op_id"],
    indices = [Index(value = ["client_op_id"])],
    foreignKeys = [
        ForeignKey(
            entity = OutboxEntity::class,
            parentColumns = ["id"],
            childColumns = ["outbox_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class OutboxPrereqEntity(
    val outbox_id: Long,
    val client_op_id: String,
)
