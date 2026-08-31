package com.example.transportapp.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.transportapp.core.database.entity.BrokerEntity
import com.example.transportapp.core.database.entity.ChargeHeadEntity
import com.example.transportapp.core.database.entity.DriverEntity
import com.example.transportapp.core.database.entity.GoodsEntity
import com.example.transportapp.core.database.entity.PartyEntity
import com.example.transportapp.core.database.entity.RateCardEntity
import com.example.transportapp.core.database.entity.RouteEntity
import com.example.transportapp.core.database.entity.StationEntity
import com.example.transportapp.core.database.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Masters access (Phase2.md S3). All reads filter tombstones.
 * D7 amendment: search is a bounded LIKE (Room 2.8.4 + KSP 2.3.11 crashes on MATCH
 * queries); PARTY_FTS stays in the schema for the sync phase.
 */
@Dao
interface MastersDao {

    // ── PARTY_E ─────────────────────────────────────────────────────────
    @Upsert
    suspend fun upsertParty(entity: PartyEntity)

    @Query("SELECT * FROM PARTY_E WHERE local_id = :localId AND deleted_at IS NULL")
    suspend fun getParty(localId: String): PartyEntity?

    @Query(
        """
        SELECT PARTY_E.* FROM PARTY_E
        WHERE PARTY_E.company_id = :companyId
          AND PARTY_E.deleted_at IS NULL
          AND (:letter = '' OR PARTY_E.name LIKE :letter || '%')
        ORDER BY PARTY_E.name
        """,
    )
    fun observeParties(companyId: String, letter: String): Flow<List<PartyEntity>>

    @Query(
        """
        SELECT PARTY_E.* FROM PARTY_E
        WHERE PARTY_E.company_id = :companyId
          AND PARTY_E.deleted_at IS NULL
          AND (PARTY_E.name LIKE :pattern OR PARTY_E.phone LIKE :pattern)
        ORDER BY PARTY_E.name
        """,
    )
    fun searchParties(companyId: String, pattern: String): Flow<List<PartyEntity>>

    @Query(
        """
        SELECT * FROM PARTY_E
        WHERE company_id = :companyId
          AND deleted_at IS NULL
          AND phone IN (
              SELECT phone FROM PARTY_E
              WHERE company_id = :companyId AND deleted_at IS NULL
              GROUP BY phone HAVING COUNT(*) > 1
          )
        ORDER BY name
        """,
    )
    fun observeDuplicateParties(companyId: String): Flow<List<PartyEntity>>

    @Query("UPDATE PARTY_E SET deleted_at = :now, sync_state = 'PENDING', updated_at_local = :now WHERE local_id = :localId")
    suspend fun tombstoneParty(localId: String, now: Long)

    // ── RATE_CARD_E ─────────────────────────────────────────────────────
    @Upsert
    suspend fun upsertRateCard(entity: RateCardEntity)

    @Query("SELECT * FROM RATE_CARD_E WHERE party_id = :partyId AND deleted_at IS NULL ORDER BY sort_order")
    suspend fun getRateRowsForParty(partyId: String): List<RateCardEntity>

    /**
     * The §3 rate-candidate superset: every non-deleted row whose non-null scope dimensions
     * equal the booking's. Rows fully out of scope are excluded here; the domain resolver
     * picks the winning step (party+route+goods → party+route → route+goods → route →
     * company default). `= NULL` in SQL is false, so null booking dimensions only match
     * null row dimensions — exactly the semantics the resolver needs.
     */
    @Query(
        """
        SELECT * FROM RATE_CARD_E
        WHERE company_id = :companyId
          AND deleted_at IS NULL
          AND (party_id IS NULL OR party_id = :partyId)
          AND (route_id IS NULL OR route_id = :routeId)
          AND (goods_id IS NULL OR goods_id = :goodsId)
        ORDER BY sort_order
        """,
    )
    suspend fun getRateCandidates(companyId: String, partyId: String?, routeId: String?, goodsId: String?): List<RateCardEntity>

    @Query("UPDATE RATE_CARD_E SET rate_paise = :ratePaise, sync_state = 'PENDING', updated_at_local = :now WHERE local_id = :localId")
    suspend fun updateRatePaise(localId: String, ratePaise: Long, now: Long)

    // ── CHARGE_HEAD_E ───────────────────────────────────────────────────
    @Upsert
    suspend fun upsertChargeHead(entity: ChargeHeadEntity)

    @Query("SELECT * FROM CHARGE_HEAD_E WHERE company_id = :companyId AND deleted_at IS NULL AND auto_apply = 1 ORDER BY sort_order")
    suspend fun getAutoChargeHeads(companyId: String): List<ChargeHeadEntity>

    // ── Reference masters ───────────────────────────────────────────────
    @Upsert suspend fun upsertStation(entity: StationEntity)
    @Upsert suspend fun upsertRoute(entity: RouteEntity)
    @Upsert suspend fun upsertGoods(entity: GoodsEntity)
    @Upsert suspend fun upsertVehicle(entity: VehicleEntity)
    @Upsert suspend fun upsertDriver(entity: DriverEntity)
    @Upsert suspend fun upsertBroker(entity: BrokerEntity)

    @Query("SELECT * FROM ROUTE_E WHERE local_id = :localId AND deleted_at IS NULL")
    suspend fun getRoute(localId: String): RouteEntity?

    @Query("SELECT * FROM STATION_E WHERE local_id = :localId AND deleted_at IS NULL")
    suspend fun getStation(localId: String): StationEntity?

    // ── T17 counts (live) ───────────────────────────────────────────────
    @Query("SELECT COUNT(*) FROM PARTY_E WHERE company_id = :companyId AND deleted_at IS NULL")
    suspend fun countParties(companyId: String): Int

    @Query("SELECT COUNT(*) FROM STATION_E WHERE company_id = :companyId AND deleted_at IS NULL")
    suspend fun countStations(companyId: String): Int

    @Query("SELECT COUNT(*) FROM ROUTE_E WHERE company_id = :companyId AND deleted_at IS NULL")
    suspend fun countRoutes(companyId: String): Int

    @Query("SELECT COUNT(*) FROM GOODS_E WHERE company_id = :companyId AND deleted_at IS NULL")
    suspend fun countGoods(companyId: String): Int

    @Query("SELECT COUNT(*) FROM VEHICLE_E WHERE company_id = :companyId AND deleted_at IS NULL")
    suspend fun countVehicles(companyId: String): Int

    @Query("SELECT COUNT(*) FROM DRIVER_E WHERE company_id = :companyId AND deleted_at IS NULL")
    suspend fun countDrivers(companyId: String): Int

    @Query("SELECT COUNT(*) FROM BROKER_E WHERE company_id = :companyId AND deleted_at IS NULL")
    suspend fun countBrokers(companyId: String): Int

    @Query("SELECT COUNT(*) FROM CHARGE_HEAD_E WHERE company_id = :companyId AND deleted_at IS NULL")
    suspend fun countChargeHeads(companyId: String): Int

    @Query("SELECT COUNT(*) FROM RATE_CARD_E WHERE company_id = :companyId AND deleted_at IS NULL")
    suspend fun countRateCards(companyId: String): Int

    /** Parties referenced by at least one rate card — the MASTER_IN_USE guard (§18.3). */
    @Query("SELECT COUNT(*) FROM RATE_CARD_E WHERE party_id = :partyId AND deleted_at IS NULL")
    suspend fun countRateCardsForParty(partyId: String): Int
}
