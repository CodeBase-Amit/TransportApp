package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * TRIP_LEG_E (§16.1) — `(trip_id, consignment_id)` unique. This join is what makes
 * transhipment representable: one consignment, several legs, one unchanged bilty number
 * (§11.2).
 */
@Entity(
    tableName = "TRIP_LEG_E",
    indices = [
        Index(value = ["trip_id", "consignment_id"], unique = true),
        Index(value = ["consignment_id"]),
    ],
    foreignKeys = [
        ForeignKey(entity = TripEntity::class, parentColumns = ["local_id"], childColumns = ["trip_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ConsignmentEntity::class, parentColumns = ["local_id"], childColumns = ["consignment_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class TripLegEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val trip_id: String,
    val consignment_id: String,
    val leg_no: Int,
    val loaded_at: Long,
)
