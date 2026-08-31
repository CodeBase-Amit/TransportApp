package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/** ROUTE_E (§16.1) — a pair of stations; transit days drive the §7.3 ageing clock. */
@Entity(
    tableName = "ROUTE_E",
    indices = [
        Index(value = ["company_id"]),
        Index(value = ["company_id", "origin_station_id", "dest_station_id"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(entity = StationEntity::class, parentColumns = ["local_id"], childColumns = ["origin_station_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = StationEntity::class, parentColumns = ["local_id"], childColumns = ["dest_station_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class RouteEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val origin_station_id: String,
    val dest_station_id: String,
    val distance_km: Int,
    val transit_days: Int,
)
