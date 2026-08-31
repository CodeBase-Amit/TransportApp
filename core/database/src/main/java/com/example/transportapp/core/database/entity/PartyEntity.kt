package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * PARTY_E (§16.1) — the party master. Display caches (`display_bilty_count`) are
 * denormalised conveniences, recomputable from consignments once S5 lands.
 * Duplicate detection is derived: parties sharing `phone` (T18 merge banner).
 */
@Entity(
    tableName = "PARTY_E",
    indices = [
        Index(value = ["company_id"]),
        Index(value = ["company_id", "name"]),
        Index(value = ["phone"]),
    ],
)
data class PartyEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val name: String,
    val phone: String,
    val email: String?,
    val type: String, // CONSIGNOR / CONSIGNEE / BOTH
    val street_address: String?,
    val station: String?,
    val pincode: String?,
    val gstin: String?,
    val usual_route_id: String?,
    val usual_payment_mode: String?,
    val display_bilty_count: Int,
)
