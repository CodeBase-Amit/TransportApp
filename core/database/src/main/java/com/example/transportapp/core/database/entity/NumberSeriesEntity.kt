package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * NUMBER_SERIES_E (§9) — the company/branch/document-type triple that names every number:
 * prefix + fy_part + a zero-padded counter ("IND/2627/" + "04189"). `last_issued` is the
 * high-water mark of *issued* numbers, not granted ones; leases cover the gap between.
 */
@Entity(
    tableName = "NUMBER_SERIES_E",
    indices = [Index(value = ["company_id", "branch_id", "doc_type"], unique = true)],
)
data class NumberSeriesEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val branch_id: String,
    /** BILTY / CHALLAN / FREIGHT_BILL / RECEIPT. */
    val doc_type: String,
    val prefix: String,
    val fy_part: String,
    val digits: Int,
    val last_issued: Long,
    /** NEVER / YEARLY / FINANCIAL_YEARLY / MONTHLY (§9). */
    val reset_rule: String,
)
