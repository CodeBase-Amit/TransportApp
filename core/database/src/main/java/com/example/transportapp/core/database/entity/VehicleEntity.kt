package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/** VEHICLE_E (§16.1) — registration, capacity in grams-of-thousands? No: kg budget §1 is grams; capacity stored in kg for display, grams for guards. */
@Entity(
    tableName = "VEHICLE_E",
    indices = [
        Index(value = ["company_id"]),
        Index(value = ["company_id", "number"], unique = true),
    ],
)
data class VehicleEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val number: String,
    /** Registered capacity in kg (display); guards convert to grams via [core.common.Weight]. */
    val capacity_kg: Int,
    /** OWN / ATTACHED / MARKET (§11.4). */
    val ownership: String,
)
