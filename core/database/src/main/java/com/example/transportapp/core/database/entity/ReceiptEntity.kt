package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * RECEIPT_E (§12.2, §16.1) — money in. The payer, the instrument and its reference, and the
 * branch that took it. What the receipt is *for* lives entirely in RECEIPT_ALLOCATION_E:
 * freight bills, To Pay consignments, or an explicit on-account park — never inferred
 * oldest-first, because the statement reads the allocation.
 */
@Entity(
    tableName = "RECEIPT_E",
    indices = [
        Index(value = ["company_id"]),
        Index(value = ["company_id", "receipt_no"], unique = true),
        Index(value = ["company_id", "party_id", "received_at"]),
    ],
)
data class ReceiptEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val series_id: String,
    val receipt_no: String?,
    /** The payer (§16.1 PARTY pays). */
    val party_id: String,
    val amount_paise: Long,
    /** CASH / UPI / CHEQUE / NEFT. */
    val instrument: String,
    /** UPI reference, cheque number or bank transfer reference; null for cash. */
    val instrument_ref: String?,
    val received_at: Long,
    val received_at_branch_id: String,
    val received_by_name: String,
    val notes: String?,
)
