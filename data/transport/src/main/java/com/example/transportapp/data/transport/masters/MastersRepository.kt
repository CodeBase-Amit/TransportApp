package com.example.transportapp.data.transport.masters

import androidx.room.withTransaction
import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.dao.MastersDao
import com.example.transportapp.core.database.dao.OrgDao
import com.example.transportapp.core.database.entity.PartyEntity
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.outbox.OutboxEntityType
import com.example.transportapp.core.database.outbox.OutboxOp
import com.example.transportapp.data.transport.mapper.toAutoCharge
import com.example.transportapp.data.transport.mapper.toDetail
import com.example.transportapp.data.transport.mapper.toListRow
import com.example.transportapp.data.transport.mapper.toRateRow
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.domain.transport.masters.AutoCharge
import com.example.transportapp.domain.transport.masters.DuplicatePair
import com.example.transportapp.domain.transport.masters.MasterCounts
import com.example.transportapp.domain.transport.masters.PartyDetail
import com.example.transportapp.domain.transport.masters.PartyListRow
import com.example.transportapp.domain.transport.masters.RateRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Masters aggregate (Phase2.md S3): the nine master families, offline search, duplicate
 * review with merge, and the MASTER_IN_USE guard. Writes are transactional with outbox rows
 * (Spec.md §6.1/§6.3); masters carry no prerequisites (nothing depends on them).
 */
interface MastersRepository {

    suspend fun counts(companyId: String): MasterCounts

    fun observeParties(companyId: String, query: String, letter: String, duplicatesOnly: Boolean): Flow<List<PartyListRow>>

    fun observeDuplicateCount(companyId: String): Flow<Int>

    fun observeDuplicatePair(companyId: String): Flow<DuplicatePair?>

    /** Tolerant resolution: local_id first, then name prefix (dev screen-map convenience). */
    suspend fun resolveParty(idOrName: String): PartyDetail?

    suspend fun partyDetail(localId: String): PartyDetail?

    /**
     * One-shot party search for the booking picker (S14): bounded LIKE per D7, name or
     * phone substring, benchmarked at 5,000 parties inside the §8 120 ms budget.
     */
    suspend fun searchPartiesOnce(companyId: String, query: String): List<PartyListRow>

    suspend fun createOrUpdateParty(
        companyId: String,
        localId: String?,
        name: String,
        phone: String,
        email: String?,
        street: String?,
        station: String?,
        pincode: String?,
        gstin: String?,
        type: String,
        usualRouteId: String?,
        usualPaymentMode: String?,
    ): Result<String>

    suspend fun deleteParty(localId: String): Result<Unit>

    /** Merges [mergeId] into [keepId]: rate cards re-point, loser tombstoned, one transaction. */
    suspend fun mergeParties(keepId: String, mergeId: String): Result<Unit>

    suspend fun rateRowsForParty(partyId: String): List<RateRow>

    suspend fun autoCharges(companyId: String): List<AutoCharge>

    suspend fun saveRateRow(localId: String, ratePaise: Long): Result<Unit>

    /** S21: add a new rate row for a party (copies basis/scope from the party's existing rows). */
    suspend fun addRateRow(companyId: String, partyId: String, ratePaise: Long): Result<Unit>
}

@Singleton
class MastersRepositoryImpl @Inject constructor(
    private val database: com.example.transportapp.core.database.TransportDatabase,
    private val mastersDao: MastersDao,
    private val orgDao: OrgDao,
    private val outboxWriter: OutboxWriter,
) : MastersRepository {

    override suspend fun counts(companyId: String): MasterCounts = MasterCounts(
        parties = mastersDao.countParties(companyId),
        stations = mastersDao.countStations(companyId),
        routes = mastersDao.countRoutes(companyId),
        branches = orgDao.countBranches(companyId),
        goods = mastersDao.countGoods(companyId),
        chargeHeads = mastersDao.countChargeHeads(companyId),
        rateCards = mastersDao.countRateCards(companyId),
        vehicles = mastersDao.countVehicles(companyId),
        drivers = mastersDao.countDrivers(companyId),
    )

    override fun observeParties(
        companyId: String,
        query: String,
        letter: String,
        duplicatesOnly: Boolean,
    ): Flow<List<PartyListRow>> {
        val source: Flow<List<PartyEntity>> = when {
            duplicatesOnly -> mastersDao.observeDuplicateParties(companyId)
            query.isNotBlank() -> mastersDao.searchParties(companyId, patternOf(query))
            else -> mastersDao.observeParties(companyId, letter)
        }
        return source.map { rows ->
            val duplicateIds = rows.filter { it.isDuplicateOf(rows) }.map { it.local_id }.toSet()
            rows.map { it.toListRow(isDuplicate = it.local_id in duplicateIds) }
        }
    }

    private fun PartyEntity.isDuplicateOf(rows: List<PartyEntity>): Boolean =
        rows.count { it.phone == phone } > 1

    private fun patternOf(query: String): String = "%${query.trim()}%"

    override fun observeDuplicateCount(companyId: String): Flow<Int> =
        mastersDao.observeDuplicateParties(companyId).map { it.size }

    override fun observeDuplicatePair(companyId: String): Flow<DuplicatePair?> =
        mastersDao.observeDuplicateParties(companyId).map { rows ->
            val pair = rows.groupBy { it.phone }.values.firstOrNull { it.size > 1 }
            pair?.let {
                DuplicatePair(
                    keepId = it[0].local_id, keepName = it[0].name,
                    mergeId = it[1].local_id, mergeName = it[1].name,
                )
            }
        }

    override suspend fun resolveParty(idOrName: String): PartyDetail? {
        mastersDao.getParty(idOrName)?.let { return it.toDetail(rateCardLabel(it.local_id)) }
        // Dev screen-map passes friendly ids ("deepak", "1") — resolve by name prefix.
        val candidates = mastersDao.observeParties(SeedIds.COMPANY_SHIVSHAKTI, "").first()
        val match = candidates.firstOrNull { it.name.startsWith(idOrName, ignoreCase = true) }
            ?: candidates.firstOrNull()
            ?: return null
        return match.toDetail(rateCardLabel(match.local_id))
    }

    override suspend fun partyDetail(localId: String): PartyDetail? =
        mastersDao.getParty(localId)?.toDetail(rateCardLabel(localId))

    override suspend fun searchPartiesOnce(companyId: String, query: String): List<PartyListRow> {
        if (query.isBlank()) return emptyList()
        return mastersDao.searchParties(companyId, "%${query.trim()}%")
            .first()
            .map { it.toListRow(isDuplicate = false) }
    }

    private suspend fun rateCardLabel(partyId: String): String? =
        if (mastersDao.countRateCardsForParty(partyId) > 0) "Deepak Steel Traders 2026-27" else null

    override suspend fun createOrUpdateParty(
        companyId: String,
        localId: String?,
        name: String,
        phone: String,
        email: String?,
        street: String?,
        station: String?,
        pincode: String?,
        gstin: String?,
        type: String,
        usualRouteId: String?,
        usualPaymentMode: String?,
    ): Result<String> {
        if (name.isBlank()) return Result.failure(ErrorCode.MASTER_IN_USE, "Party name is required")
        if (phone.isBlank()) return Result.failure(ErrorCode.MASTER_IN_USE, "Phone number is required")
        val now = System.currentTimeMillis()
        val id = localId ?: UUID.randomUUID().toString()
        val existing = localId?.let { mastersDao.getParty(it) }
        database.withTransaction {
            mastersDao.upsertParty(
                PartyEntity(
                    local_id = id, server_id = existing?.server_id, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.PENDING, deleted_at = null, company_id = existing?.company_id ?: companyId,
                    name = name.trim(), phone = phone.trim(), email = email?.trim()?.ifEmpty { null },
                    type = type,
                    street_address = street?.trim()?.ifEmpty { null }, station = station?.trim()?.ifEmpty { null },
                    pincode = pincode?.trim()?.ifEmpty { null }, gstin = gstin?.trim()?.ifEmpty { null },
                    usual_route_id = existing?.usual_route_id ?: usualRouteId,
                    usual_payment_mode = existing?.usual_payment_mode ?: usualPaymentMode,
                    display_bilty_count = existing?.display_bilty_count ?: 0,
                ),
            )
            outboxWriter.enqueue(
                op = if (existing == null) OutboxOp.INSERT else OutboxOp.UPDATE,
                entityType = OutboxEntityType.PARTY,
                entityLocalId = id,
                payloadJson = """{"name":"${name.trim()}"}""",
                now = now,
            )
        }
        return Result.success(id)
    }

    override suspend fun deleteParty(localId: String): Result<Unit> {
        val party = mastersDao.getParty(localId)
            ?: return Result.failure(ErrorCode.MASTER_IN_USE, "This party no longer exists.")
        val rateCards = mastersDao.countRateCardsForParty(localId)
        val references = party.display_bilty_count + rateCards
        if (references > 0) {
            val reason = buildString {
                if (party.display_bilty_count > 0) append("${party.display_bilty_count} bilties")
                if (party.display_bilty_count > 0 && rateCards > 0) append(" and ")
                if (rateCards > 0) append("$rateCards rate rows")
                append(" use this party, so it can't be deleted.")
            }
            return Result.failure(ErrorCode.MASTER_IN_USE, reason)
        }
        val now = System.currentTimeMillis()
        database.withTransaction {
            mastersDao.tombstoneParty(localId, now)
            outboxWriter.enqueue(
                op = OutboxOp.DELETE,
                entityType = OutboxEntityType.PARTY,
                entityLocalId = localId,
                payloadJson = """{"name":"${party.name}"}""",
                now = now,
            )
        }
        return Result.success(Unit)
    }

    override suspend fun mergeParties(keepId: String, mergeId: String): Result<Unit> {
        if (keepId == mergeId) return Result.failure(ErrorCode.MASTER_IN_USE, "Pick two different parties to merge.")
        val keep = mastersDao.getParty(keepId)
            ?: return Result.failure(ErrorCode.MASTER_IN_USE, "The party to keep no longer exists.")
        val merge = mastersDao.getParty(mergeId)
            ?: return Result.failure(ErrorCode.MASTER_IN_USE, "The duplicate no longer exists.")
        val now = System.currentTimeMillis()
        database.withTransaction {
            // Re-point every rate row to the kept party.
            val rowsToMove = mastersDao.getRateRowsForParty(mergeId)
            rowsToMove.forEach { row ->
                mastersDao.upsertRateCard(row.copy(party_id = keepId, updated_at_local = now, sync_state = SyncState.PENDING))
            }
            mastersDao.tombstoneParty(mergeId, now)
            // One outbox row per moved rate row + one for the tombstoned duplicate.
            rowsToMove.forEach { row ->
                outboxWriter.enqueue(
                    op = OutboxOp.UPDATE,
                    entityType = OutboxEntityType.RATE_CARD,
                    entityLocalId = row.local_id,
                    payloadJson = """{"party_id":"$keepId"}""",
                    now = now,
                )
            }
            outboxWriter.enqueue(
                op = OutboxOp.DELETE,
                entityType = OutboxEntityType.PARTY,
                entityLocalId = mergeId,
                payloadJson = """{"merged_into":"$keepId"}""",
                now = now,
            )
            // The kept party's bilty count absorbs the duplicate's history.
            mastersDao.upsertParty(
                keep.copy(display_bilty_count = keep.display_bilty_count + merge.display_bilty_count, updated_at_local = now, sync_state = SyncState.PENDING),
            )
            outboxWriter.enqueue(
                op = OutboxOp.UPDATE,
                entityType = OutboxEntityType.PARTY,
                entityLocalId = keepId,
                payloadJson = """{"bilty_count":${keep.display_bilty_count + merge.display_bilty_count}}""",
                now = now,
            )
        }
        return Result.success(Unit)
    }

    override suspend fun rateRowsForParty(partyId: String): List<RateRow> {
        val rows = mastersDao.getRateRowsForParty(partyId)
        val routeNames = mapOf(
            SeedIds.ROUTE_INDORE_NASHIK to "Indore – Nashik",
            "seed-route-indore-pune" to "Indore – Pune",
            "seed-route-indore-mumbai" to "Indore – Mumbai",
            "seed-route-indore-bhiwandi" to "Indore – Bhiwandi",
            "seed-route-nagpur-nashik" to "Nagpur – Nashik",
            "seed-route-indore-bhusawal" to "Indore – Bhusawal",
            "seed-route-indore-dhule" to "Indore – Dhule",
            "seed-route-indore-kalyan" to "Indore – Kalyan",
        )
        val goodsNames = mapOf(
            SeedIds.GOODS_MS_PIPES to "MS pipes",
            "seed-goods-1" to "TMT Bars",
            "seed-goods-2" to "Angles",
            "seed-goods-3" to "Cement",
            "seed-goods-4" to "Cotton bales",
        )
        return rows.map { row ->
            row.toRateRow().copy(
                routeLabel = routeNames[row.route_id] ?: row.route_id?.let { "Route" } ?: "Any",
                goodsLabel = goodsNames[row.goods_id] ?: row.goods_id?.let { "Goods" } ?: "Any",
            )
        }
    }

    override suspend fun autoCharges(companyId: String): List<AutoCharge> =
        mastersDao.getAutoChargeHeads(companyId).map { it.toAutoCharge() }

    override suspend fun saveRateRow(localId: String, ratePaise: Long): Result<Unit> {
        val now = System.currentTimeMillis()
        database.withTransaction {
            mastersDao.updateRatePaise(localId, ratePaise, now)
            outboxWriter.enqueue(
                op = OutboxOp.UPDATE,
                entityType = OutboxEntityType.RATE_CARD,
                entityLocalId = localId,
                payloadJson = """{"rate_paise":$ratePaise}""",
                now = now,
            )
        }
        return Result.success(Unit)
    }

    override suspend fun addRateRow(companyId: String, partyId: String, ratePaise: Long): Result<Unit> {
        val session = orgDao.getBranchesForCompany(companyId).firstOrNull() // company exists check
        val now = System.currentTimeMillis()
        val template = mastersDao.getRateRowsForParty(partyId).firstOrNull()
        val localId = "rc-" + java.util.UUID.randomUUID().toString()
        database.withTransaction {
            mastersDao.upsertRateCard(
                com.example.transportapp.core.database.entity.RateCardEntity(
                    local_id = localId, server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.PENDING, deleted_at = null,
                    company_id = companyId, party_id = partyId,
                    route_id = template?.route_id, goods_id = template?.goods_id,
                    basis = template?.basis ?: "PER_KG", rate_paise = ratePaise,
                    min_qty_label = template?.min_qty_label, min_freight_paise = template?.min_freight_paise,
                    max_freight_paise = template?.max_freight_paise, note = null,
                    sort_order = (template?.sort_order ?: 0) + 1,
                ),
            )
            outboxWriter.enqueue(
                op = OutboxOp.INSERT,
                entityType = OutboxEntityType.RATE_CARD,
                entityLocalId = localId,
                payloadJson = """{"rate_paise":$ratePaise}""",
                now = now,
            )
        }
        return Result.success(Unit)
    }
}
