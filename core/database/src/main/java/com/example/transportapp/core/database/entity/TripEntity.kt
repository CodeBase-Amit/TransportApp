package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * TRIP_E (§11) — one vehicle's journey from an origin branch to a destination station.
 * The challan is the same aggregate's printed face. `challan_no` is stamped at issue
 * (§11.2); a trip being built carries no number. `hire_paise`/`advance_paise`/`balance_paise`
 * are the working denorm of the LORRY_HIRE row — the lorry-hire table stays the ledger.
 */
@Entity(
    tableName = "TRIP_E",
    indices = [
        Index(value = ["company_id"]),
        Index(value = ["company_id", "challan_no"], unique = true),
        Index(value = ["vehicle_id"]),
        Index(value = ["company_id", "state"]),
    ],
)
data class TripEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val series_id: String,
    /** Stamped at issue; null while the trip is being built (§11.1). */
    val challan_no: String?,
    /** OPEN / ISSUED / DISPATCHED / CLOSED / CANCELLED (§11.1). */
    val state: String,
    val vehicle_id: String,
    val driver_id: String,
    val origin_branch_id: String,
    val dest_station_id: String,
    /** Station ids joined for the "via" line — display-only (§11.2). */
    val via_stations: String?,
    val hire_paise: Long,
    val advance_paise: Long,
    val balance_paise: Long,
    val expected_arrival: Long,
    val created_at: Long,
    val created_by_name: String,
    val dispatched_at: Long?,
    val closed_at: Long?,
    val cancel_reason: String?,
)
