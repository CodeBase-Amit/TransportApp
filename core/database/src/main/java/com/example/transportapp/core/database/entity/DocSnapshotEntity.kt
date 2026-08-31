package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * DOC_SNAPSHOT_E (§8) — the immutable print payload of one document: every value that
 * prints, as JSON, with the template version pinned. A snapshot is never edited — it is
 * superseded: provisional renumbering appends version n+1 and the provisional snapshot
 * survives untouched with its content hash (§8, §9).
 */
@Entity(
    tableName = "DOC_SNAPSHOT_E",
    indices = [Index(value = ["consignment_id", "version"])],
    foreignKeys = [
        ForeignKey(entity = ConsignmentEntity::class, parentColumns = ["local_id"], childColumns = ["consignment_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class DocSnapshotEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val consignment_id: String,
    /** BILTY / LOADING_CHALLAN / FREIGHT_BILL / MONEY_RECEIPT / POD / … */
    val document_type: String,
    val template_id: String,
    val template_version: String,
    val version: Int,
    val payload_json: String,
    /** FNV-1a over the payload; proves a reprint reproduces the same bytes (§8). */
    val content_hash: String,
    val copy_count: Int,
)
