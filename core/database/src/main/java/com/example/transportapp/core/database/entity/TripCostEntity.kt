package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * TRIP_COST_E (§11.3) — optional trip-cost lines: halting, detention, toll, fuel advance,
 * other. The remark is required (§11.1's trip-cost row). The vehicle is denormalised so
 * the §14.3 Expense register sheet needs no join at export time.
 */
@Entity(
    tableName = "TRIP_COST_E",
    indices = [Index(value = ["trip_id"]), Index(value = ["vehicle_id"])],
    foreignKeys = [
        ForeignKey(entity = TripEntity::class, parentColumns = ["local_id"], childColumns = ["trip_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class TripCostEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val trip_id: String,
    val vehicle_id: String,
    /** halting / detention / toll / fuel_advance / other (§11.3). */
    val head: String,
    val incurred_on: Long,
    val amount_paise: Long,
    val payment_mode: String,
    /** Required (§11.3). */
    val remark: String,
)
