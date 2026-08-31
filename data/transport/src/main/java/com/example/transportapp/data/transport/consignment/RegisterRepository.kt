package com.example.transportapp.data.transport.consignment

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.transportapp.core.database.dao.ConsignmentDao
import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.PaymentMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The register's filter state (Design T7's chip row). Status chips are mutually exclusive;
 * payment/unbilled/month toggles combine; the branch scope flips between the active branch
 * and all branches. Search runs as a bounded LIKE (D7) over number, party denorm and
 * private mark.
 */
data class RegisterFilter(
    val status: ConsignmentStatus? = null,
    val paymentMode: PaymentMode? = null,
    val unbilledOnly: Boolean = false,
    val allBranches: Boolean = false,
    val sinceAt: Long? = null,
    val search: String? = null,
)

/** One docket row as the screen renders it (Design T7's 88dp row). */
data class RegisterDocket(
    val localId: String,
    val displayNo: String,
    val consigneeName: String,
    val amountPaise: Long,
    val fromStation: String,
    val toStation: String,
    val status: ConsignmentStatus,
    val paymentMode: PaymentMode?,
    val packages: Long,
    val weightKg: Long,
    val bookedAt: Long,
    val syncPending: Boolean,
    val heldRemark: String?,
)

data class RegisterSummary(
    val matching: Int,
    val packages: Long,
    val amountPaise: Long,
)

/** Register reads (Phase2.md S6): Paging 3 for the list, one aggregate for the strip (D6). */
interface RegisterRepository {

    fun pagingRegister(companyId: String, branchId: String, filter: RegisterFilter): Flow<PagingData<RegisterDocket>>

    suspend fun summary(companyId: String, branchId: String, filter: RegisterFilter): RegisterSummary
}

@Singleton
class RegisterRepositoryImpl @Inject constructor(
    private val consignmentDao: ConsignmentDao,
) : RegisterRepository {

    override fun pagingRegister(companyId: String, branchId: String, filter: RegisterFilter): Flow<PagingData<RegisterDocket>> =
        Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PAGE_SIZE / 2, enablePlaceholders = false),
            pagingSourceFactory = {
                consignmentDao.pagingRegister(
                    companyId = companyId,
                    branchId = if (filter.allBranches) null else branchId,
                    status = filter.status?.name,
                    paymentMode = filter.paymentMode?.name,
                    unbilledOnly = filter.unbilledOnly,
                    sinceAt = filter.sinceAt,
                    pattern = filter.search?.trim()?.takeIf { it.isNotEmpty() }?.let { "%$it%" },
                )
            },
        ).flow.map { paging ->
            paging.map { row ->
                RegisterDocket(
                    localId = row.local_id,
                    displayNo = row.display_no,
                    consigneeName = row.consignee_name,
                    amountPaise = row.total_paise,
                    fromStation = row.from_station,
                    toStation = row.to_station,
                    status = runCatching { ConsignmentStatus.valueOf(row.status) }.getOrDefault(ConsignmentStatus.BOOKED),
                    paymentMode = runCatching { PaymentMode.valueOf(row.payment_mode) }.getOrNull(),
                    packages = row.packages,
                    weightKg = row.weight_kg,
                    bookedAt = row.booked_at,
                    syncPending = row.sync_state == "PENDING",
                    heldRemark = row.held_remark,
                )
            }
        }

    override suspend fun summary(companyId: String, branchId: String, filter: RegisterFilter): RegisterSummary {
        val row = consignmentDao.summaryRegister(
            companyId = companyId,
            branchId = if (filter.allBranches) null else branchId,
            status = filter.status?.name,
            paymentMode = filter.paymentMode?.name,
            unbilledOnly = filter.unbilledOnly,
            sinceAt = filter.sinceAt,
            pattern = filter.search?.trim()?.takeIf { it.isNotEmpty() }?.let { "%$it%" },
        )
        return RegisterSummary(row.matching, row.packages, row.amountPaise)
    }

    companion object {
        const val PAGE_SIZE = 30
    }
}
