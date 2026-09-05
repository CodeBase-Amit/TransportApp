package com.example.transportapp.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.transportapp.core.database.entity.NumberLeaseEntity
import com.example.transportapp.core.database.entity.NumberSeriesEntity
import kotlinx.coroutines.flow.Flow

/**
 * Numbering access (§9). Lease consumption and granting happen inside repository
 * transactions — this DAO only reads and upserts, so the no-overlap guard and the
 * high-water update stay in one place.
 */
@Dao
interface NumberingDao {

    @Upsert
    suspend fun upsertSeries(entity: NumberSeriesEntity)

    @Query(
        """
        SELECT * FROM NUMBER_SERIES_E
        WHERE company_id = :companyId AND branch_id = :branchId AND doc_type = :docType AND deleted_at IS NULL
        """,
    )
    suspend fun getSeries(companyId: String, branchId: String, docType: String): NumberSeriesEntity?

    /** S27: the T28 edit dialog resolves by local id — no label parsing. */
    @Query("SELECT * FROM NUMBER_SERIES_E WHERE local_id = :localId AND company_id = :companyId AND deleted_at IS NULL")
    suspend fun getSeriesById(companyId: String, localId: String): NumberSeriesEntity?

    /** T28's rows: every series of the company, branch by branch. */
    @Query("SELECT * FROM NUMBER_SERIES_E WHERE company_id = :companyId AND deleted_at IS NULL ORDER BY branch_id, doc_type")
    fun observeSeriesForCompany(companyId: String): Flow<List<NumberSeriesEntity>>

    @Upsert
    suspend fun upsertLease(entity: NumberLeaseEntity)

    /** Live, unexpired leases for a series, lowest next_value first. */
    @Query(
        """
        SELECT * FROM NUMBER_LEASE_E
        WHERE series_id = :seriesId AND deleted_at IS NULL AND next_value <= range_end AND expires_at > :now
        ORDER BY next_value
        """,
    )
    suspend fun getActiveLeases(seriesId: String, now: Long): List<NumberLeaseEntity>

    @Query("SELECT * FROM NUMBER_LEASE_E WHERE series_id = :seriesId AND deleted_at IS NULL")
    suspend fun getLeasesForSeries(seriesId: String): List<NumberLeaseEntity>
}
