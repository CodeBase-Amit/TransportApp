package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * PARTY_FTS — the offline party-search index (§16.2, D7: FTS4 + unicode61).
 * External-content table over PARTY_E: `local_id` is indexed too so the search query joins
 * on declared columns (Room validates every referenced column; rowid is not one).
 * Room maintains the sync triggers on fresh databases; MIGRATION_2_3 recreates them for
 * upgraded databases (proven by test).
 */
@Fts4(contentEntity = PartyEntity::class)
@Entity(tableName = "PARTY_FTS")
data class PartyFtsEntity(
    val local_id: String,
    val name: String,
    val phone: String,
)
