package com.example.transportapp.data.transport.company

import androidx.room.withTransaction
import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.dao.OrgDao
import com.example.transportapp.core.database.entity.BranchEntity
import com.example.transportapp.core.database.entity.CompanyEntity
import com.example.transportapp.core.database.entity.MembershipEntity
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.outbox.OutboxEntityType
import com.example.transportapp.core.database.outbox.OutboxOp
import com.example.transportapp.core.datastore.context.ActiveContextStore
import com.example.transportapp.core.datastore.session.SessionStore
import com.example.transportapp.data.transport.mapper.toDomain
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.domain.transport.org.BranchSummary
import com.example.transportapp.domain.transport.org.CompanySummary
import com.example.transportapp.domain.transport.org.MembershipStatus
import com.example.transportapp.domain.transport.org.MembershipSummary
import com.example.transportapp.domain.transport.org.RegisterCompanyRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The org aggregate's single source of truth (Phase2.md S2). Every write happens in one Room
 * transaction with its outbox row (Spec.md §6.1/§6.3); prerequisites declare that a new
 * company's row must drain before its branch and membership rows.
 */
interface CompanyRepository {

    fun observeCompanies(): Flow<List<CompanySummary>>

    /** Every non-deleted branch, for picker chips and scope labels. */
    fun observeAllBranches(): Flow<List<BranchSummary>>

    /** ACTIVE member count per company local id (T2 "4 members"). */
    fun observeMemberCounts(): Flow<Map<String, Int>>

    /** Memberships of one user with company names resolved (T2 rows + invitations). */
    fun observeMembershipsForUser(userEmail: String): Flow<List<MembershipSummary>>

    suspend fun getBranchesForCompany(companyLocalId: String): List<BranchSummary>

    /** Selects a membership's company (and branch) as the active context for the app. */
    suspend fun selectCompanyAndBranch(membershipLocalId: String, branchLocalId: String?): Result<Unit>

    suspend fun setInvitationAccepted(membershipLocalId: String): Result<Unit>

    suspend fun setInvitationDeclined(membershipLocalId: String): Result<Unit>

    /** Creates company + first branch + Owner membership for the creator (§6.1, T3). */
    suspend fun registerCompany(request: RegisterCompanyRequest): Result<Unit>
}

@Singleton
class CompanyRepositoryImpl @Inject constructor(
    private val database: TransportDatabase,
    private val orgDao: OrgDao,
    private val outboxWriter: OutboxWriter,
    private val sessionStore: SessionStore,
    private val activeContextStore: ActiveContextStore,
) : CompanyRepository {

    override fun observeCompanies(): Flow<List<CompanySummary>> =
        orgDao.observeCompanies().map { list -> list.map { it.toDomain() } }

    override fun observeAllBranches(): Flow<List<BranchSummary>> =
        orgDao.observeBranches().map { list -> list.map { it.toDomain() } }

    override fun observeMemberCounts(): Flow<Map<String, Int>> =
        orgDao.observeMemberships().map { list ->
            list
                .filter { it.status == MembershipEntity.STATUS_ACTIVE }
                .groupingBy { it.company_id }
                .eachCount()
        }

    override fun observeMembershipsForUser(userEmail: String): Flow<List<MembershipSummary>> =
        combine(
            orgDao.observeMemberships(),
            orgDao.observeCompanies(),
        ) { memberships, companies ->
            val nameById = companies.associate { it.local_id to it.name }
            memberships
                .filter { it.user_email.equals(userEmail, ignoreCase = true) }
                .map { it.toDomain().copy(companyName = nameById[it.company_id].orEmpty()) }
        }

    override suspend fun getBranchesForCompany(companyLocalId: String): List<BranchSummary> =
        orgDao.getBranchesForCompany(companyLocalId).map { it.toDomain() }

    override suspend fun selectCompanyAndBranch(membershipLocalId: String, branchLocalId: String?): Result<Unit> {
        val membership = orgDao.getMembership(membershipLocalId)
            ?: return Result.failure(ErrorCode.AUTH_NO_ACCESS, "Membership not found")
        val company = orgDao.getCompany(membership.company_id)
            ?: return Result.failure(ErrorCode.AUTH_NO_ACCESS, "Company not found")
        val branches = orgDao.getBranchesForCompany(company.local_id)
        val branch = branchLocalId?.let { id -> branches.firstOrNull { it.local_id == id } }
            ?: branches.firstOrNull { it.is_head_office }

        val branchLabel = when {
            branch != null -> branch.name
            membership.branch_scope == MembershipEntity.SCOPE_ALL -> "All branches"
            else -> branches.firstOrNull { it.local_id == membership.branch_scope }?.name.orEmpty()
        }

        sessionStore.setActiveContext(
            companyId = company.local_id,
            companyName = company.name,
            branchId = branch?.local_id.orEmpty(),
            branchName = branchLabel,
        )
        activeContextStore.setActive(companyId = company.local_id, branchId = branch?.local_id.orEmpty())
        return Result.success(Unit)
    }

    override suspend fun setInvitationAccepted(membershipLocalId: String): Result<Unit> {
        val now = System.currentTimeMillis()
        database.withTransaction {
            orgDao.setMembershipStatus(membershipLocalId, MembershipStatus.ACTIVE.name, now)
            outboxWriter.enqueue(
                op = OutboxOp.UPDATE,
                entityType = OutboxEntityType.MEMBERSHIP,
                entityLocalId = membershipLocalId,
                payloadJson = """{"status":"ACTIVE"}""",
                now = now,
            )
        }
        return Result.success(Unit)
    }

    override suspend fun setInvitationDeclined(membershipLocalId: String): Result<Unit> {
        val now = System.currentTimeMillis()
        database.withTransaction {
            orgDao.setMembershipStatus(membershipLocalId, MembershipStatus.DECLINED.name, now)
            outboxWriter.enqueue(
                op = OutboxOp.UPDATE,
                entityType = OutboxEntityType.MEMBERSHIP,
                entityLocalId = membershipLocalId,
                payloadJson = """{"status":"DECLINED"}""",
                now = now,
            )
        }
        return Result.success(Unit)
    }

    override suspend fun registerCompany(request: RegisterCompanyRequest): Result<Unit> {
        if (request.companyName.isBlank()) return Result.failure(ErrorCode.MASTER_IN_USE, "Company name is required")
        val now = System.currentTimeMillis()
        val companyId = UUID.randomUUID().toString()
        val branchId = UUID.randomUUID().toString()
        val membershipId = UUID.randomUUID().toString()

        database.withTransaction {
            val companyOpId = outboxWriter.enqueue(
                op = OutboxOp.INSERT,
                entityType = OutboxEntityType.COMPANY,
                entityLocalId = companyId,
                payloadJson = """{"name":"${request.companyName}"}""",
                now = now,
            )
            orgDao.upsertCompany(
                CompanyEntity(
                    local_id = companyId, server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.PENDING, deleted_at = null,
                    name = request.companyName, legal_name = request.companyName, address = request.address,
                    gstin = request.gstin, pan = null, transporter_id = null, gst_treatment = "FORWARD",
                    display_bilty_series = null,
                ),
            )

            val branchOpId = outboxWriter.enqueue(
                op = OutboxOp.INSERT,
                entityType = OutboxEntityType.BRANCH,
                entityLocalId = branchId,
                payloadJson = """{"name":"${request.branchName}"}""",
                prerequisites = listOf(companyOpId),
                now = now,
            )
            orgDao.upsertBranch(
                BranchEntity(
                    local_id = branchId, server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.PENDING, deleted_at = null, company_id = companyId,
                    name = request.branchName, code = request.branchCode, address = request.address,
                    is_head_office = true,
                ),
            )

            outboxWriter.enqueue(
                op = OutboxOp.INSERT,
                entityType = OutboxEntityType.MEMBERSHIP,
                entityLocalId = membershipId,
                payloadJson = """{"role":"OWNER"}""",
                prerequisites = listOf(companyOpId, branchOpId),
                now = now,
            )
            orgDao.upsertMembership(
                MembershipEntity(
                    local_id = membershipId, server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.PENDING, deleted_at = null, company_id = companyId,
                    user_name = request.ownerUserName, user_email = request.ownerUserEmail, role = "OWNER",
                    branch_scope = MembershipEntity.SCOPE_ALL, status = MembershipEntity.STATUS_ACTIVE,
                    invited_by = null, invited_expires_at = null, display_expires = null,
                ),
            )
        }

        // The creator lands inside the new company straight away (§6.1: creator is granted Owner).
        sessionStore.setActiveContext(
            companyId = companyId, companyName = request.companyName,
            branchId = branchId, branchName = request.branchName,
        )
        activeContextStore.setActive(companyId = companyId, branchId = branchId)
        return Result.success(Unit)
    }
}
