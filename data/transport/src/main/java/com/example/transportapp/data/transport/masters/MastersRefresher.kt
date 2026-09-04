package com.example.transportapp.data.transport.masters

import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.network.MastersApi
import com.example.transportapp.core.network.NumberingApi
import com.example.transportapp.data.transport.session.SessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * S24 — the remote→Room refresher (§16: the server mirrors into Room; Room stays the only
 * read source). **Offline-first contract:** every pull failure answers
 * `OFFLINE_UNAVAILABLE` and changes nothing — the app keeps working from local data.
 *
 * Reconciliation is by `server_id`: a remote doc whose id is already mirrored updates that
 * row; otherwise it inserts a new local row. Rows the clerk created offline (server_id
 * null) are never touched — they belong to the outbox drain (S25).
 */
@Singleton
class MastersRefresher @Inject constructor(
    private val database: TransportDatabase,
    private val mastersApi: MastersApi,
    private val sessionRepository: SessionRepository,
) {

    suspend fun refreshAll(): Result<Int> {
        val parties = refreshParties()
        if (parties is Result.Failure) return parties
        val stations = refreshStations()
        if (stations is Result.Failure) return stations
        val total = (parties as Result.Success).value + (stations as Result.Success).value
        return Result.success(total)
    }

    suspend fun refreshParties(): Result<Int> {
        val session = sessionRepository.session.first()
        return when (val result = mastersApi.parties()) {
            is Result.Success -> Result.success(result.value.sumOf { remote -> upsertParty(session.companyId, remote) })
            is Result.Failure -> result
        }
    }

    suspend fun refreshStations(): Result<Int> {
        val session = sessionRepository.session.first()
        return when (val result = mastersApi.stations()) {
            is Result.Success -> Result.success(result.value.sumOf { remote -> upsertStation(session.companyId, remote) })
            is Result.Failure -> result
        }
    }

    /** Existing mirrored row updates in place; a new server doc inserts. Local-only rows untouched. */
    private suspend fun upsertParty(companyId: String, remote: com.example.transportapp.core.network.RemoteMaster): Int {
        val dao = database.mastersDao()
        val now = System.currentTimeMillis()
        val existing = remote.serverId.takeIf { it.isNotEmpty() }?.let { dao.getPartyByServerId(it) }
        val entity = (existing ?: newPartyShell(companyId)).copy(
            server_id = remote.serverId,
            updated_at_local = now,
            updated_at_server = now,
            sync_state = SyncState.SYNCED,
            name = remote.name,
            phone = remote.extra["phone"]?.toString()?.trim('"') ?: existing?.phone ?: "",
            gstin = remote.extra["gstin"]?.toString()?.trim('"')?.takeIf { it != "null" } ?: existing?.gstin,
            station = remote.extra["address"]?.toString()?.trim('"')?.takeIf { it != "null" } ?: existing?.station,
            type = remote.extra["type"]?.toString()?.trim('"') ?: existing?.type ?: "BOTH",
        )
        dao.upsertParty(entity)
        return 1
    }

    private suspend fun upsertStation(companyId: String, remote: com.example.transportapp.core.network.RemoteMaster): Int {
        val dao = database.mastersDao()
        val now = System.currentTimeMillis()
        val existing = remote.serverId.takeIf { it.isNotEmpty() }?.let { dao.getStationByServerId(it) }
        val entity = (existing ?: newStationShell(companyId)).copy(
            server_id = remote.serverId,
            updated_at_local = now,
            updated_at_server = now,
            sync_state = SyncState.SYNCED,
            name = remote.name,
            state = remote.extra["state"]?.toString()?.trim('"')?.takeIf { it != "null" } ?: existing?.state,
        )
        dao.upsertStation(entity)
        return 1
    }

    private fun newPartyShell(companyId: String) =
        com.example.transportapp.core.database.entity.PartyEntity(
            local_id = "party-" + java.util.UUID.randomUUID().toString(),
            server_id = null, updated_at_local = 0, updated_at_server = null,
            sync_state = SyncState.SYNCED, deleted_at = null,
            company_id = companyId, name = "", phone = "", email = null, type = "BOTH",
            street_address = null, station = null, pincode = null, gstin = null,
            usual_route_id = null, usual_payment_mode = null, display_bilty_count = 0,
        )

    private fun newStationShell(companyId: String) =
        com.example.transportapp.core.database.entity.StationEntity(
            local_id = "st-" + java.util.UUID.randomUUID().toString(),
            server_id = null, updated_at_local = 0, updated_at_server = null,
            sync_state = SyncState.SYNCED, deleted_at = null,
            company_id = companyId, name = "", state = null,
        )

    private fun offline(): Result<Int> =
        Result.failure(ErrorCode.OFFLINE_UNAVAILABLE, "You're offline — showing the data on this device")
}
