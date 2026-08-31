package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * CONSIGNMENT_ITEM_E (§16.1) — one goods row of a bilty (multi-article bilties carry many).
 * The printed goods table reads straight from here: packages, description, actual vs
 * chargeable weight, rate, freight — stored per item so a rate card change cannot alter an
 * issued bilty (§16.1 charge_line note).
 */
@Entity(
    tableName = "CONSIGNMENT_ITEM_E",
    indices = [Index(value = ["consignment_id"])],
    foreignKeys = [
        ForeignKey(entity = ConsignmentEntity::class, parentColumns = ["local_id"], childColumns = ["consignment_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class ConsignmentItemEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val consignment_id: String,
    val goods_id: String?,
    val description: String,
    val packages: Long,
    val actual_weight_g: Long,
    val chargeable_weight_g: Long,
    /** The rate that printed, in paise per the basis unit. */
    val rate_paise: Long?,
    /** PER_KG / PER_TONNE / PER_PACKAGE / PER_TRIP / FIXED (§10.2). */
    val basis: String?,
    val freight_paise: Long,
)
