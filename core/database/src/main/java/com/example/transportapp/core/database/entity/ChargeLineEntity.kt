package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * CHARGE_LINE_E (§16.1) — the stored charge computation of one consignment: head, basis,
 * the input value that printed, and the computed paise. A rate card change can never alter
 * an issued bilty because the computation is frozen here at booking time.
 */
@Entity(
    tableName = "CHARGE_LINE_E",
    indices = [Index(value = ["consignment_id"])],
    foreignKeys = [
        ForeignKey(entity = ConsignmentEntity::class, parentColumns = ["local_id"], childColumns = ["consignment_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class ChargeLineEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val consignment_id: String,
    /** freight / hamali / door_delivery / gst / rounding / … (§10.3 codes). */
    val head_code: String,
    /** The label that prints, denormalised at issue time. */
    val label: String,
    val basis: String,
    /** The human-readable input ("780 kg × 4.50", "fixed", "5% of freight"). */
    val input_value: String?,
    val computed_paise: Long,
    val taxable: Boolean,
    val sort_order: Int,
)
