package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * CREDIT_NOTE_E (§12.1, §16.1) — the only legal way to correct an issued bill: a linked
 * credit note plus a fresh bill, never an edit. The table exists from S9 (the schema is
 * final) but Phase 2 ships no correction flow; the statement read path includes notes
 * when they appear via sync.
 */
@Entity(
    tableName = "CREDIT_NOTE_E",
    indices = [
        Index(value = ["company_id"]),
        Index(value = ["freight_bill_id"]),
    ],
)
data class CreditNoteEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val series_id: String,
    val note_no: String?,
    val freight_bill_id: String,
    val party_id: String,
    val reason: String,
    val amount_paise: Long,
    /** The fresh bill that replaces the corrected one, when raised. */
    val replacement_bill_id: String?,
    val created_at: Long,
    val created_by_name: String,
)
