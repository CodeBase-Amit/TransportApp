package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * RECEIPT_ALLOCATION_E (§12.2, §16.1) — the explicit "what is this money for" row. Exactly
 * one of [bill_id] / [consignment_id] is set, or the target is ON_ACCOUNT (parked credit).
 * Σ allocations of a receipt may be less than the receipt's amount — the remainder is an
 * on-account credit; it may never exceed it (validated in the repository).
 */
@Entity(
    tableName = "RECEIPT_ALLOCATION_E",
    indices = [
        Index(value = ["receipt_id"]),
        Index(value = ["bill_id"]),
        Index(value = ["consignment_id"]),
    ],
)
data class ReceiptAllocationEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val receipt_id: String,
    /** BILL / TOPAY_CONSIGNMENT / ON_ACCOUNT. */
    val target_type: String,
    val bill_id: String?,
    val consignment_id: String?,
    val amount_paise: Long,
)
