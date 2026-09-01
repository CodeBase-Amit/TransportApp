package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * TEMPLATE_E (§16.2, Phase 3 S11) — a document template as data. Versions are rows, never
 * updates in place: a reprint resolves the version a document was created against (§17.2),
 * so pruning a version any snapshot references is forbidden (§9.12).
 *
 * `content_json` stores the raw template JSON string, not a decomposed relational form —
 * that single choice means a template written against engine schema v1 still re-parses
 * years later (§6.8).
 */
@Entity(
    tableName = "TEMPLATE_E",
    indices = [
        Index(value = ["company_id"]),
        Index(value = ["company_id", "template_key", "version"], unique = true),
        Index(value = ["company_id", "is_active"]),
    ],
)
data class TemplateEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    /** Stable per-template identity (e.g. "tpl-bilty-default"); versions are rows. */
    val template_key: String,
    val version: Int,
    /** Exactly one active row per (company, template_key). */
    val is_active: Boolean,
    /** The engine compatibility gate carried inside the JSON itself (§6.8). */
    val schema_version: Int,
    val content_json: String,
    val content_hash: String,
    /** BUILT-IN / COMPANY. */
    val visibility: String,
    val created_by_name: String,
)
