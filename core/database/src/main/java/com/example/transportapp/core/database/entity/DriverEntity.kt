package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/** DRIVER_E (§16.1) — name, licence, phone. Licence numbers print in Plex Mono (Design.md §A6). */
@Entity(
    tableName = "DRIVER_E",
    indices = [Index(value = ["company_id"])],
)
data class DriverEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val name: String,
    val licence: String?,
    val phone: String?,
)
