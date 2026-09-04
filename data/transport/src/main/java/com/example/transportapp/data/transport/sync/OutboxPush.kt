package com.example.transportapp.data.transport.sync

import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.entity.PartyEntity
import com.example.transportapp.core.database.outbox.OutboxEntity
import com.example.transportapp.core.database.outbox.OutboxEntityType
import com.example.transportapp.core.database.outbox.OutboxOp
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.network.ApiClient
import com.example.transportapp.data.transport.session.SessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * S25 (D64) — the outbox drain: local ops → the backend's REST surface.
 *
 * Maps the outbox vocabulary onto the test backend's entity routes:
 *   PARTY   INSERT → POST /api/parties · UPDATE → PATCH /api/parties/:serverId ·
 *           DELETE → DELETE /api/parties/:serverId (by name lookup when unsynced).
 *   Other entity families drain in later sprints (consignments need the booking POST).
 *
 * Semantics: a 2xx marks the row DONE; a typed failure marks it retriable with
 * exponential backoff and records the code for T31's sync queue. **Offline-first:**
 * every attempt is best-effort — nothing here can fail the caller's workflow, and a
 * dead network simply leaves rows PENDING for the next drain.
 *
 * On INSERT success the server's `_id` is written back onto the local row's
 * `server_id`, so later UPDATE/DELETE ops address the real server document.
 */
@Singleton
class OutboxPush @Inject constructor(
    private val database: TransportDatabase,
    private val apiClient: ApiClient,
    private val sessionRepository: SessionRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    data class DrainReport(val pushed: Int, val failed: Int) {
        val pending: Int get() = pushed + failed
    }

    suspend fun drain(limit: Int = 50): Result<DrainReport> {
        val session = sessionRepository.session.first()
        if (!session.isSignedIn) {
            return Result.failure(ErrorCode.AUTH_NO_ACCESS, "No active session")
        }
        val now = System.currentTimeMillis()
        val ready = database.outboxDao().getReady(now = now, limit = limit)
        if (ready.isEmpty()) return Result.success(DrainReport(0, 0))

        var pushed = 0
        var failed = 0
        ready.forEach { row ->
            when (val outcome = pushOne(row)) {
                is Result.Success -> {
                    database.outboxDao().markDone(listOf(row.id))
                    pushed++
                }
                is Result.Failure -> {
                    val attempts = row.attempt_count + 1
                    val backoffMs = RETRY_BASE_MS * (1L shl attempts.coerceAtMost(6))
                    database.outboxDao().markRetriable(
                        ids = listOf(row.id),
                        nextAttemptAt = now + backoffMs,
                        errorCode = outcome.code.name,
                    )
                    failed++
                }
            }
        }
        return Result.success(DrainReport(pushed, failed))
    }

    private suspend fun pushOne(row: OutboxEntity): Result<Unit> = when (row.entity_type) {
        OutboxEntityType.PARTY -> pushParty(row)
        else -> skipUntilSupported(row)
    }

    /** Entity families whose REST mapping lands with their own sync sprints. */
    private fun skipUntilSupported(row: OutboxEntity): Result<Unit> =
        Result.failure(ErrorCode.OFFLINE_UNAVAILABLE, "${row.entity_type} sync lands with the online tier")

    private suspend fun pushParty(row: OutboxEntity): Result<Unit> {
        val party = database.mastersDao().getParty(row.entity_local_id)
            ?: return markGone(row)
        return when (row.op) {
            OutboxOp.INSERT, OutboxOp.UPDATE -> {
                val body = partyJson(party, row.payload_json)
                if (party.server_id.isNullOrEmpty()) {
                    when (val result = apiClient.post("api/parties", body)) {
                        is Result.Success -> {
                            val serverId = runCatching { result.value.jsonObject["_id"]?.jsonPrimitive?.content }
                                .getOrNull().orEmpty()
                            if (serverId.isNotEmpty()) {
                                database.mastersDao().upsertParty(party.copy(server_id = serverId, sync_state = SyncState.SYNCED))
                            }
                            Result.success(Unit)
                        }
                        is Result.Failure -> result
                    }
                } else {
                    apiClient.patch("api/parties/${party.server_id}", body).mapUnit()
                }
            }
            OutboxOp.DELETE -> {
                val sid = party.server_id
                if (sid.isNullOrEmpty()) {
                    // Never reached the server — the tombstone is enough locally.
                    Result.success(Unit)
                } else {
                    apiClient.delete("api/parties/$sid").mapUnit()
                }
            }
        }
    }

    private fun <T> Result<T>.mapUnit(): Result<Unit> = when (this) {
        is Result.Success -> Result.success(Unit)
        is Result.Failure -> this
    }

    private fun partyJson(party: PartyEntity, payloadJson: String): JsonObject {
        // Start from the row itself (authoritative), overlay the op's payload delta.
        val delta = runCatching { json.parseToJsonElement(payloadJson).jsonObject }.getOrElse { JsonObject(emptyMap()) }
        val nameFromDelta = (delta["name"] as? JsonPrimitive)?.content
        return buildJsonObject {
            put("name", JsonPrimitive(nameFromDelta ?: party.name))
            put("phone", JsonPrimitive(party.phone))
            put("address", JsonPrimitive(party.street_address ?: party.station ?: ""))
            party.gstin?.let { put("gstin", JsonPrimitive(it)) }
            put("type", JsonPrimitive(party.type))
        }
    }

    /** The local row was hard-removed (e.g. a merge winner tombstoned the loser after drain). */
    private suspend fun markGone(row: OutboxEntity): Result<Unit> {
        database.outboxDao().markDone(listOf(row.id))
        return Result.success(Unit)
    }

    companion object {
        private const val RETRY_BASE_MS = 60_000L
    }
}
