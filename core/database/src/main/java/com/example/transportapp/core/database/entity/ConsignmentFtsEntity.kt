package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * CONSIGNMENT_FTS (§16.2, §3.3, D7: FTS4 + unicode61) — the offline bilty-search index.
 * External-content table over CONSIGNMENT_E: `local_id` is indexed so the search query
 * joins on a declared column. Room maintains the sync triggers on fresh databases;
 * MIGRATION_4_5 recreates them for upgraded databases (proven by test). Note (D7): the
 * S6 register search runs as a bounded LIKE until the Room/KSP MATCH crash is fixed —
 * this table is the sync-phase index.
 */
@Fts4(contentEntity = ConsignmentEntity::class)
@Entity(tableName = "CONSIGNMENT_FTS")
data class ConsignmentFtsEntity(
    val local_id: String,
    val bilty_no: String,
    val party_names: String,
)
