package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * COMPANY_E (TransportApp.md §16.1/§16.2). Column names mirror the server table; every
 * tenant-owned row carries the sync envelope. `display_bilty_series` is a denormalised
 * display cache for T2 until NUMBER_SERIES_E lands in S5 and it is computed properly.
 */
@Entity(tableName = "COMPANY_E", indices = [Index(value = ["name"])])
data class CompanyEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val name: String,
    val legal_name: String?,
    val address: String?,
    val gstin: String?,
    val pan: String?,
    val transporter_id: String?,
    /** FORWARD / REVERSE / EXEMPT — the company default GTA GST treatment (§10.5). */
    val gst_treatment: String,
    val display_bilty_series: String?,
)
