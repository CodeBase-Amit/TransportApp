package com.example.transportapp.data.transport.numbering

import androidx.room.withTransaction
import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.dao.NumberingDao
import com.example.transportapp.core.database.entity.NumberLeaseEntity
import com.example.transportapp.core.database.entity.NumberSeriesEntity
import com.example.transportapp.core.database.envelope.SyncState
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** A number looked at but not consumed — the T5 top bar shows it "from the first moment" (§3). */
data class ReservedNumber(val display: String, val provisional: Boolean)

/** A number stamped into the bilty at issue time (§9). */
data class IssuedNumber(val display: String, val provisional: Boolean, val rawValue: Long?)

/**
 * §9 numbering, offline-first. Leased blocks of [LEASE_BLOCK_SIZE] numbers per device per
 * series, consumed at issue time; the grant is simulated locally in Phase 2 (there is no
 * server to ask). When grants are unavailable and the lease is exhausted, booking continues
 * on the clearly-labelled provisional series — "PROV-<device>-<seq>" — whose numbers are
 * renumbered exactly once at first sync (§9); the sequence is derived from the highest
 * provisional number already on this device, so it survives restarts.
 *
 * Every mutation runs in a Room transaction; [issueNext] may be called inside the caller's
 * booking transaction (nested withTransaction joins it).
 */
interface NumberingRepository {

    suspend fun peekNext(companyId: String, branchId: String, docType: String): ReservedNumber?

    suspend fun issueNext(companyId: String, branchId: String, docType: String, now: Long = System.currentTimeMillis()): Result<IssuedNumber>

    /** Debug/demo: exhaust the active leases so the next issue falls through (§9 banner demo). */
    suspend fun debugShrinkActiveLease(companyId: String, branchId: String, docType: String)

    /** Debug/demo: simulate a server that cannot grant leases right now. */
    suspend fun debugSetGrantsEnabled(enabled: Boolean)

    /**
     * S18: provision a series for a freshly-registered company/branch (§9) so the first
     * booking has a real series to lease from. No-op when the triple already exists.
     */
    suspend fun ensureSeries(companyId: String, branchId: String, docType: String, prefix: String)
}

@Singleton
class NumberingRepositoryImpl @Inject constructor(
    private val database: TransportDatabase,
    private val numberingDao: NumberingDao,
    private val deviceIdProvider: DeviceIdProvider,
    private val numberingApi: com.example.transportapp.core.network.NumberingApi,
) : NumberingRepository {

    @Volatile
    private var grantsEnabled: Boolean = true

    override suspend fun peekNext(companyId: String, branchId: String, docType: String): ReservedNumber? {
        val series = numberingDao.getSeries(companyId, branchId, docType) ?: return null
        val now = System.currentTimeMillis()
        val lease = numberingDao.getActiveLeases(series.local_id, now).firstOrNull()
        return when {
            lease != null -> ReservedNumber(series.format(lease.next_value), provisional = false)
            grantsEnabled -> ReservedNumber(series.format(nextGrantStart(series, now)), provisional = false)
            else -> ReservedNumber(provisionalDisplay(companyId, now), provisional = true)
        }
    }

    override suspend fun issueNext(companyId: String, branchId: String, docType: String, now: Long): Result<IssuedNumber> =
        database.withTransaction {
            val series = numberingDao.getSeries(companyId, branchId, docType)
                ?: return@withTransaction Result.failure(ErrorCode.LEASE_INVALID, "No number series for $docType at this branch")
            val lease = numberingDao.getActiveLeases(series.local_id, now).firstOrNull()
                ?: (if (grantsEnabled) {
                    // S24: try the server lease first (atomic, cross-device safe). Any
                    // failure — offline, no server series — falls through to the local
                    // block grant, preserving the offline-first contract (D62).
                    serverLeaseInto(docType, series, now)
                } else null) ?: (if (grantsEnabled) grantLease(series, now) else null)

            if (lease != null) {
                val value = lease.next_value
                numberingDao.upsertLease(
                    lease.copy(next_value = value + 1, updated_at_local = now),
                )
                numberingDao.upsertSeries(
                    series.copy(last_issued = maxOf(series.last_issued, value), updated_at_local = now),
                )
                Result.success(IssuedNumber(series.format(value), provisional = false, rawValue = value))
            } else {
                Result.success(IssuedNumber(provisionalNumber(companyId, now), provisional = true, rawValue = null))
            }
        }

    /**
     * S24: lease one number from the server and park it on a one-number local lease so
     * the existing consumption path is unchanged. Null when the server is unreachable —
     * the caller falls back to the local grant (D62: the app never waits on a network).
     */
    private suspend fun serverLeaseInto(docType: String, series: com.example.transportapp.core.database.entity.NumberSeriesEntity, now: Long): com.example.transportapp.core.database.entity.NumberLeaseEntity? {
        val remote = when (val result = numberingApi.lease(docType)) {
            is Result.Success -> result.value
            is Result.Failure -> return null
        }
        // Parse the trailing digits from the server's formatted number ("IND/2627/04191").
        val value = remote.substringAfterLast('/').toLongOrNull() ?: return null
        val lease = com.example.transportapp.core.database.entity.NumberLeaseEntity(
            local_id = "lease-" + java.util.UUID.randomUUID().toString(),
            server_id = null, updated_at_local = now, updated_at_server = now,
            sync_state = SyncState.SYNCED, deleted_at = null,
            series_id = series.local_id, device_id = "server",
            range_start = value, range_end = value, next_value = value,
            expires_at = now + LEASE_EXPIRY_MS,
        )
        numberingDao.upsertLease(lease)
        return lease
    }

    override suspend fun debugShrinkActiveLease(companyId: String, branchId: String, docType: String) {
        val series = numberingDao.getSeries(companyId, branchId, docType) ?: return
        database.withTransaction {
            numberingDao.getLeasesForSeries(series.local_id).forEach { lease ->
                numberingDao.upsertLease(lease.copy(range_end = lease.next_value - 1, updated_at_local = System.currentTimeMillis()))
            }
        }
    }

    override suspend fun debugSetGrantsEnabled(enabled: Boolean) {
        grantsEnabled = enabled
    }

    override suspend fun ensureSeries(companyId: String, branchId: String, docType: String, prefix: String) {
        val now = System.currentTimeMillis()
        if (numberingDao.getSeries(companyId, branchId, docType) != null) return
        val fyPart = financialYearPart(now)
        numberingDao.upsertSeries(
            NumberSeriesEntity(
                local_id = "series-" + UUID.randomUUID().toString(),
                server_id = null,
                updated_at_local = now,
                updated_at_server = null,
                sync_state = SyncState.PENDING,
                deleted_at = null,
                company_id = companyId,
                branch_id = branchId,
                doc_type = docType,
                prefix = "$prefix/$fyPart/",
                fy_part = fyPart,
                digits = 5,
                last_issued = 0L,
                reset_rule = "FINANCIAL_YEARLY",
            ),
        )
    }

    /** §9: the Indian financial year the counter resets at — "2627" for Apr 2026–Mar 2027. */
    private fun financialYearPart(now: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val startYear = if (cal.get(java.util.Calendar.MONTH) >= java.util.Calendar.APRIL) cal.get(java.util.Calendar.YEAR) else cal.get(java.util.Calendar.YEAR) - 1
        val y1 = (startYear % 100).toString().padStart(2, '0')
        val y2 = ((startYear + 1) % 100).toString().padStart(2, '0')
        return "$y1$y2"
    }

    /**
     * A new lease starts beyond every number this series has ever touched — the issued
     * high-water mark and every lease range ever granted — so live leases can never
     * overlap (the server-side partial unique index's local counterpart, §9).
     */
    private suspend fun nextGrantStart(series: NumberSeriesEntity, now: Long): Long =
        maxOf(series.last_issued, numberingDao.getLeasesForSeries(series.local_id).maxOfOrNull { it.range_end } ?: 0L) + 1

    private suspend fun grantLease(series: NumberSeriesEntity, now: Long): NumberLeaseEntity {
        val start = nextGrantStart(series, now)
        val lease = NumberLeaseEntity(
            local_id = "lease-" + UUID.randomUUID().toString(),
            server_id = null,
            updated_at_local = now,
            updated_at_server = null,
            sync_state = SyncState.PENDING,
            deleted_at = null,
            series_id = series.local_id,
            device_id = deviceIdProvider.shortId(),
            range_start = start,
            range_end = start + LEASE_BLOCK_SIZE - 1,
            next_value = start,
            expires_at = now + LEASE_EXPIRY_MS,
        )
        numberingDao.upsertLease(lease)
        return lease
    }

    /** Read-only: what the next provisional number would be. A peek never consumes (§9). */
    private suspend fun provisionalDisplay(companyId: String, now: Long): String {
        val device = deviceIdProvider.shortId()
        val existing = numberingDao.getSeries(companyId, PROV_BRANCH_PREFIX + device, PROV_DOC_TYPE)
        val next = (existing?.last_issued ?: 0L) + 1
        return PROV_PREFIX + device + "-" + next.toString().padStart(6, '0')
    }

    /** Restart-safe: the provisional high-water lives on a per-device PROVISIONAL series row. */
    private suspend fun provisionalNumber(companyId: String, now: Long): String {
        val device = deviceIdProvider.shortId()
        val branchKey = PROV_BRANCH_PREFIX + device
        val existing = numberingDao.getSeries(companyId, branchKey, PROV_DOC_TYPE)
        val next = (existing?.last_issued ?: 0L) + 1
        val series = existing?.copy(last_issued = next, updated_at_local = now)
            ?: NumberSeriesEntity(
                local_id = "series-prov-$companyId-$device",
                server_id = null,
                updated_at_local = now,
                updated_at_server = null,
                sync_state = SyncState.PENDING,
                deleted_at = null,
                company_id = companyId,
                branch_id = branchKey,
                doc_type = PROV_DOC_TYPE,
                prefix = PROV_PREFIX + device + "-",
                fy_part = "prov",
                digits = 6,
                last_issued = next,
                reset_rule = "NEVER",
            )
        numberingDao.upsertSeries(series)
        return series.prefix + next.toString().padStart(series.digits, '0')
    }

    private fun NumberSeriesEntity.format(value: Long): String = prefix + value.toString().padStart(digits, '0')

    companion object {
        const val LEASE_BLOCK_SIZE = 50L
        const val LEASE_EXPIRY_MS = 30L * 24 * 60 * 60 * 1000
        const val PROV_PREFIX = "PROV-"
        const val PROV_BRANCH_PREFIX = "prov-device-"
        const val PROV_DOC_TYPE = "PROVISIONAL"
    }
}
