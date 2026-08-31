package com.example.transportapp.core.database.outbox

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Outbox access. Readiness is computed here (dependency-aware drain, TransportApp.md §16.2):
 * a row is ready when it is PENDING, due, and **none of its prerequisites is itself pending**.
 */
@Dao
interface OutboxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRow(row: OutboxEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrereqs(rows: List<OutboxPrereqEntity>)

    /** Rows drainable right now, oldest first. */
    @Query(
        """
        SELECT o.* FROM outbox o
        WHERE o.state = 'PENDING'
          AND o.next_attempt_at <= :now
          AND NOT EXISTS (
              SELECT 1 FROM outbox_prereq p
              JOIN outbox o2 ON o2.client_op_id = p.client_op_id
              WHERE p.outbox_id = o.id AND o2.state = 'PENDING'
          )
        ORDER BY o.created_at ASC
        LIMIT :limit
        """,
    )
    suspend fun getReady(now: Long, limit: Int = 50): List<OutboxEntity>

    @Query("SELECT COUNT(*) FROM outbox WHERE state = 'PENDING'")
    suspend fun getPendingCount(): Int

    /** T31 sync queue ("WAITING TO SYNC · N"). */
    @Query("SELECT COUNT(*) FROM outbox WHERE state = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE outbox SET state = 'DONE' WHERE id IN (:ids)")
    suspend fun markDone(ids: List<Long>)

    @Query(
        """
        UPDATE outbox
        SET state = 'PENDING',
            attempt_count = attempt_count + 1,
            next_attempt_at = :nextAttemptAt,
            last_error_code = :errorCode
        WHERE id IN (:ids)
        """,
    )
    suspend fun markRetriable(ids: List<Long>, nextAttemptAt: Long, errorCode: String?)
}
