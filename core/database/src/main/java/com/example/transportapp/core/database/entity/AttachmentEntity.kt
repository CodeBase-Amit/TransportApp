package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * ATTACHMENT_E (§4.1/§7.2) — a photo of the party's invoice, an e-way bill printout, a
 * packing list, a challan photo. Stored locally, uploaded when online (the sync envelope's
 * PENDING state is the upload queue; the drain worker reads it in the sync phase).
 */
@Entity(
    tableName = "ATTACHMENT_E",
    indices = [Index(value = ["consignment_id"])],
    foreignKeys = [
        ForeignKey(entity = ConsignmentEntity::class, parentColumns = ["local_id"], childColumns = ["consignment_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class AttachmentEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val consignment_id: String,
    /** CHALLAN_PHOTO / WEIGHBRIDGE_SLIP / GOODS / OTHER. */
    val kind: String,
    /** Local file path until the sync phase swaps in object storage. */
    val file_ref: String,
    val size_bytes: Long,
    val caption: String?,
)
