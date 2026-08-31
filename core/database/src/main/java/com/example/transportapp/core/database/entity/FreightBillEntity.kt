package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * FREIGHT_BILL_E (§12.1, §16.1) — one party, one period, many consignments. A draft carries
 * no number (the number is consumed only at issue, which Phase 2 keeps server-side —
 * `issue` answers OFFLINE_UNAVAILABLE); a cancelled bill retains its number for continuity.
 * `gst_treatment` is frozen from the consignments at draft time; a bill mixing treatments is
 * refused before it exists (BILL_MIXED_TREATMENT).
 *
 * No foreign keys to parties or consignments on purpose: masters tombstone rather than
 * hard-delete, and the one-consignment-one-live-bill rule is a repository concern here
 * (the partial unique index is the server's guarantee, §12.1).
 */
@Entity(
    tableName = "FREIGHT_BILL_E",
    indices = [
        Index(value = ["company_id"]),
        Index(value = ["company_id", "bill_no"], unique = true),
        Index(value = ["company_id", "party_id", "state"]),
        Index(value = ["company_id", "state"]),
    ],
)
data class FreightBillEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val series_id: String,
    /** Null while Draft; stamped at issue; retained on cancel (§12.1 number continuity). */
    val bill_no: String?,
    /** DRAFT / ISSUED / CANCELLED. */
    val state: String,
    val party_id: String,
    val period_start: Long,
    val period_end: Long,
    val due_at: Long?,
    /** Σ consignment freight. */
    val freight_paise: Long,
    /** Σ other charge lines (hamali, door delivery, rounding) — total − freight − gst. */
    val other_charges_paise: Long,
    /** Σ taxable charge lines across the bill's consignments. */
    val taxable_paise: Long,
    val gst_paise: Long,
    val total_paise: Long,
    /** INTERSTATE / INTRASTATE — frozen from the consignments' place of supply (§12.1). */
    val gst_treatment: String,
    val notes: String?,
    val issued_at: Long?,
    val issued_by_name: String?,
    val cancelled_at: Long?,
)
