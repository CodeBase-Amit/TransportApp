package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * STATUS_EVENT_E (§7.2) — append-only. There is deliberately no update or delete path in
 * the DAO: the timeline and the derived status projection can only ever agree with the log.
 * `client_event_id` is unique per company so offline replays are idempotent (§3.4 #8).
 */
@Entity(
    tableName = "STATUS_EVENT_E",
    indices = [
        Index(value = ["consignment_id", "occurred_at"]),
        Index(value = ["company_id", "client_event_id"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(entity = ConsignmentEntity::class, parentColumns = ["local_id"], childColumns = ["consignment_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class StatusEventEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val consignment_id: String,
    val client_event_id: String,
    /** The §7.1 event types: BOOKED / LOADED / IN_TRANSIT / AT_HUB / ARRIVED / … */
    val event_type: String,
    /** User-editable within a bounded window; the server's recorded_at is separate (§7.2). */
    val occurred_at: Long,
    val recorded_at: Long,
    val actor_member_id: String?,
    /** Denormalised so the timeline survives the member being removed (§7.2). */
    val actor_name: String,
    val branch_id: String,
    val location: String?,
    val photo_ref: String?,
    /** Required when event_type is HELD: SHORTAGE / DAMAGE / DETAINED / OTHER (§7.2). */
    val reason_code: String?,
    val remark: String?,
    /** Set automatically for challan-driven events (§7.2). */
    val challan_ref: String?,
)
