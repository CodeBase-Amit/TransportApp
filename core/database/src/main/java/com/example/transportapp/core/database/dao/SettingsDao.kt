package com.example.transportapp.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.transportapp.core.database.entity.CompanySettingEntity
import com.example.transportapp.core.database.entity.GoodsEntity
import com.example.transportapp.core.database.entity.RouteEntity
import com.example.transportapp.core.database.entity.StationEntity
import kotlinx.coroutines.flow.Flow

/** The picker lists' row shape: a route joined with its two station names. */
data class RouteOptionRow(
    val local_id: String,
    val from_name: String,
    val to_name: String,
    val distance_km: Long,
    val transit_days: Long,
)

/**
 * Dated company calculation settings (Phase 3 S14, §10.5) and the picker lists the booking
 * form reads (party/route/goods scope replaces the S4 demo hardcoding).
 */
@Dao
interface SettingsDao {

    @Upsert
    suspend fun upsertSetting(entity: CompanySettingEntity)

    /** The governing row: newest effective_from at or before [at]. */
    @Query(
        """
        SELECT * FROM COMPANY_SETTING_E
        WHERE company_id = :companyId AND deleted_at IS NULL AND effective_from <= :at
        ORDER BY effective_from DESC LIMIT 1
        """,
    )
    suspend fun governingSetting(companyId: String, at: Long): CompanySettingEntity?

    @Query("SELECT COUNT(*) FROM COMPANY_SETTING_E WHERE company_id = :companyId AND deleted_at IS NULL")
    suspend fun countSettings(companyId: String): Int

    // ── pickers (S14) ───────────────────────────────────────────────────

    @Query(
        """
        SELECT R.local_id AS local_id, OS.name AS from_name, DS.name AS to_name,
               R.distance_km AS distance_km, R.transit_days AS transit_days
        FROM ROUTE_E R
        JOIN STATION_E OS ON OS.local_id = R.origin_station_id
        JOIN STATION_E DS ON DS.local_id = R.dest_station_id
        WHERE R.company_id = :companyId AND R.deleted_at IS NULL
        ORDER BY OS.name, DS.name
        """,
    )
    suspend fun routeOptions(companyId: String): List<RouteOptionRow>

    @Query("SELECT * FROM GOODS_E WHERE company_id = :companyId AND deleted_at IS NULL ORDER BY name")
    suspend fun goodsOptions(companyId: String): List<GoodsEntity>

    @Query("SELECT * FROM STATION_E WHERE company_id = :companyId AND deleted_at IS NULL ORDER BY name")
    suspend fun stations(companyId: String): List<StationEntity>
}
