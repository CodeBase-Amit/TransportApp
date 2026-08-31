package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * POD_E (§4.1/§16.1) — one per consignment (the §16.1 one-to-or-zero). Consignee name,
 * signature drawn on screen or photographed, date, remarks. A POD record is what unblocks
 * `Delivered` (§7.1) — or a Manager waiver instead.
 */
@Entity(
    tableName = "POD_E",
    indices = [Index(value = ["consignment_id"], unique = true)],
    foreignKeys = [
        ForeignKey(entity = ConsignmentEntity::class, parentColumns = ["local_id"], childColumns = ["consignment_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class PodEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val consignment_id: String,
    val consignee_name: String,
    /** Local file path of the drawn signature or the photo. */
    val signature_ref: String?,
    val photo_ref: String?,
    val pod_date: Long,
    val remarks: String?,
)
