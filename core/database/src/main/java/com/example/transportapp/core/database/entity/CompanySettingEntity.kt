package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * COMPANY_SETTING_E (Phase 3 S14, §10.5): dated company calculation settings. Rows are
 * history, never edits-in-place — a setting change must not alter already-booked bilties
 * (§12.1's freeze principle applied to configuration), and the effective date is the key:
 * the newest row with `effective_from <= now` governs a new booking.
 *
 * `gst_rate_bp` is basis points (500 = 5%); `volumetric_divisor_g` nullable (null = the
 * full-load house where clerks never see dimension boxes, §10.1).
 */
@Entity(
    tableName = "COMPANY_SETTING_E",
    indices = [
        Index(value = ["company_id"]),
        Index(value = ["company_id", "effective_from"]),
    ],
)
data class CompanySettingEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val effective_from: Long,
    val gst_rate_bp: Int,
    val weight_step_g: Long,
    val volumetric_divisor_g: Long?,
    val gst_treatment: String,
    val rounding: String,
    val created_by_name: String,
)
