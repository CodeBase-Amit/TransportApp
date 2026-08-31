package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * NUMBER_LEASE_E (§9) — a contiguous block of numbers granted to one device for one
 * series. Phase 2 simulates the grant locally (50 numbers); the server-side partial
 * unique index that prevents two live leases overlapping a range becomes a repository
 * transaction guard here (SQLite has no range-exclusion constraint).
 */
@Entity(
    tableName = "NUMBER_LEASE_E",
    indices = [Index(value = ["series_id"]), Index(value = ["series_id", "device_id"])],
)
data class NumberLeaseEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val series_id: String,
    val device_id: String,
    val range_start: Long,
    val range_end: Long,
    val next_value: Long,
    val expires_at: Long,
)
