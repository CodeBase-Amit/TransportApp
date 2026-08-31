package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * CHARGE_HEAD_E (§10.3) — charges are data, not code: code, label, basis, default value,
 * taxable, bearer. `auto_apply` marks the §3 charge templates that attach themselves to a
 * booking from the rate card; `display_value` is the T20 line ("₹8.00 / art"). `basis`
 * carries a machine token (FLAT / PER_PACKAGE / PER_KG / PERCENT_OF_FREIGHT /
 * PERCENT_OF_VALUE / PER_DAY) that the S4 calculator interprets; `default_value_paise`
 * is paise for flat/per-package bases and percent×100 for percent bases.
 */
@Entity(
    tableName = "CHARGE_HEAD_E",
    indices = [
        Index(value = ["company_id"]),
        Index(value = ["company_id", "code"], unique = true),
    ],
)
data class ChargeHeadEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val code: String,
    val label: String,
    val basis: String,
    val display_value: String?,
    /** Paise for flat/per-package heads; percent×100 (500 = 5.00%) for percent heads. */
    val default_value_paise: Long,
    /** Who bears the charge by default (§10.3) — CONSIGNOR / CONSIGNEE / SHARED. */
    val bearer: String,
    val taxable: Boolean,
    val auto_apply: Boolean,
    val sort_order: Int,
)
