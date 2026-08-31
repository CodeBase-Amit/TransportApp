package com.example.transportapp.core.database.cursor

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * Per-entity-family delta cursor (TransportApp.md §17.1). Written only by the sync phase;
 * the table exists from v1 so the sync envelope contract is complete and no migration is
 * needed when the delta feed lands.
 */
@Entity(tableName = "sync_cursor")
data class SyncCursorEntity(
    @PrimaryKey val entity_family: String,
    val cursor: String,
    val updated_at: Long,
)

@Dao
interface SyncCursorDao {

    @Upsert
    suspend fun upsert(cursor: SyncCursorEntity)

    @Query("SELECT * FROM sync_cursor WHERE entity_family = :family")
    suspend fun get(family: String): SyncCursorEntity?
}
