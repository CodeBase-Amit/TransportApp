package com.example.transportapp.data.transport.account

import com.example.transportapp.core.database.dao.NumberingDao
import com.example.transportapp.core.database.dao.OrgDao
import com.example.transportapp.core.database.outbox.OutboxEntityType
import com.example.transportapp.core.database.outbox.OutboxOp
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.data.transport.session.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Company profile payload for the outbox (§16.2). */
internal fun companyProfilePayload(
    name: String, legalName: String, address: String, gstin: String, pan: String, transporterId: String?,
): String = org.json.JSONObject()
    .put("name", name)
    .put("legal_name", legalName)
    .put("address", address)
    .put("gstin", gstin)
    .put("pan", pan)
    .put("transporter_id", transporterId)
    .toString()

/** One branch row as T26 prints it. */
data class BranchRowData(val name: String, val code: String, val isHeadOffice: Boolean, val address: String?)

/** One member row as T27 prints it. */
data class MemberRowData(val name: String, val email: String, val role: String, val branchScope: String, val status: String)

/** One series row as T28 prints it. */
data class SeriesRowData(
    val localId: String,
    val branch: String,
    val docType: String,
    val prefix: String,
    val fyPart: String,
    val digits: Int,
    val lastIssued: Long,
    val nextValue: Long?,
)

/**
 * Settings reads (Phase2.md S10): the live org data behind T24/T26/T27/T28.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val orgDao: OrgDao,
    private val numberingDao: NumberingDao,
    private val sessionRepository: SessionRepository,
    private val outboxWriter: OutboxWriter,
) {

    fun branches(): Flow<List<BranchRowData>> =
        combine(orgDao.observeBranches(), sessionRepository.session) { all, s ->
            all.filter { it.company_id == s.companyId && it.deleted_at == null }
                .map { BranchRowData(it.name, it.code, it.is_head_office, it.address) }
        }

    fun members(): Flow<List<MemberRowData>> =
        combine(orgDao.observeMemberships(), sessionRepository.session) { all, s ->
            all.filter { it.company_id == s.companyId && it.deleted_at == null }
                .map { MemberRowData(it.user_name, it.user_email, it.role, it.branch_scope, it.status) }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun series(): Flow<List<SeriesRowData>> =
        sessionRepository.session.map { it.companyId }
            .flatMapLatest { companyId -> numberingDao.observeSeriesForCompany(companyId) }
            .map { series ->
                series.map { s ->
                    SeriesRowData(
                        localId = s.local_id,
                        branch = s.branch_id, docType = s.doc_type, prefix = s.prefix,
                        fyPart = s.fy_part, digits = s.digits, lastIssued = s.last_issued,
                        nextValue = null,
                    )
                }
            }

    /** The T25 company profile as loaded from COMPANY_E (S16). */
    data class CompanyProfile(
        val name: String,
        val legalName: String?,
        val address: String?,
        val gstin: String?,
        val pan: String?,
        val transporterId: String?,
    )

    suspend fun companyProfile(companyId: String): CompanyProfile? {
        val company = orgDao.getCompany(companyId) ?: return null
        return CompanyProfile(
            name = company.name,
            legalName = company.legal_name,
            address = company.address,
            gstin = company.gstin,
            pan = company.pan,
            transporterId = company.transporter_id,
        )
    }

    /** §17.4.1: the profile is Owner data; the save updates COMPANY_E and queues the sync. */
    suspend fun saveCompanyProfile(
        companyId: String,
        name: String,
        legalName: String,
        address: String,
        gstin: String,
        pan: String,
        transporterId: String?,
    ) {
        val company = orgDao.getCompany(companyId) ?: return
        orgDao.upsertCompany(
            company.copy(
                name = name,
                legal_name = legalName,
                address = address,
                gstin = gstin,
                pan = pan,
                transporter_id = transporterId,
                updated_at_local = System.currentTimeMillis(),
            ),
        )
        outboxWriter.enqueue(
            op = com.example.transportapp.core.database.outbox.OutboxOp.UPDATE,
            entityType = com.example.transportapp.core.database.outbox.OutboxEntityType.COMPANY,
            entityLocalId = companyId,
            payloadJson = com.example.transportapp.data.transport.account.companyProfilePayload(name, legalName, address, gstin, pan, transporterId),
            now = System.currentTimeMillis(),
        )
    }

    /**
     * S19 — the §9 counter change (T28's Edit). Owner-only; the counter moves *forward*
     * only (a moved-back counter re-issues printed numbers); the update and its audit
     * outbox row commit together. `newLastIssued` is the new high-water mark.
     */
    suspend fun changeSeriesCounter(companyId: String, branchId: String, docType: String, newLastIssued: Long): com.example.transportapp.core.common.Result<Unit> {
        val session = sessionRepository.session.first()
        if (session.role != "OWNER") {
            return com.example.transportapp.core.common.Result.failure(
                com.example.transportapp.core.common.ErrorCode.AUTH_NO_ACCESS, "Only the Owner can change a numbering counter"
            )
        }
        val series = numberingDao.getSeries(companyId, branchId, docType)
            ?: return com.example.transportapp.core.common.Result.failure(
                com.example.transportapp.core.common.ErrorCode.LEASE_INVALID, "Series not found"
            )
        if (newLastIssued < series.last_issued) {
            return com.example.transportapp.core.common.Result.failure(
                com.example.transportapp.core.common.ErrorCode.LEASE_INVALID,
                "The counter can only move forward — it is already at ${series.last_issued}",
            )
        }
        val now = System.currentTimeMillis()
        numberingDao.upsertSeries(series.copy(last_issued = newLastIssued, updated_at_local = now))
        outboxWriter.enqueue(
            op = com.example.transportapp.core.database.outbox.OutboxOp.UPDATE,
            entityType = com.example.transportapp.core.database.outbox.OutboxEntityType.NUMBER_SERIES,
            entityLocalId = series.local_id,
            payloadJson = """{"last_issued":$newLastIssued}""",
            now = now,
        )
        return com.example.transportapp.core.common.Result.success(Unit)
    }

    /** The branch's local id for the T28 edit dialog (rows today key by branch name). */
    suspend fun branchIdForName(companyId: String, branchName: String): String? =
        orgDao.getBranchesForCompany(companyId).firstOrNull { it.name == branchName }?.local_id

    /**
     * S19 — the §17.4.1 invite: Owner-only; writes an INVITED membership row with a
     * five-day expiry and its outbox INSERT (the invite travels with the sync, §16.2).
     * A member already active on that email is refused.
     */
    suspend fun inviteMember(companyId: String, email: String, role: String, invitedByName: String): com.example.transportapp.core.common.Result<Unit> {
        val session = sessionRepository.session.first()
        if (session.role != "OWNER") {
            return com.example.transportapp.core.common.Result.failure(
                com.example.transportapp.core.common.ErrorCode.AUTH_NO_ACCESS, "Only the Owner can invite members"
            )
        }
        val now = System.currentTimeMillis()
        val existing = orgDao.observeMemberships().first()
            .firstOrNull { it.company_id == companyId && it.user_email == email && it.deleted_at == null }
        if (existing != null && existing.status == com.example.transportapp.core.database.entity.MembershipEntity.STATUS_ACTIVE) {
            return com.example.transportapp.core.common.Result.failure(
                com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE, "$email is already a member"
            )
        }
        val membershipId = java.util.UUID.randomUUID().toString()
        orgDao.upsertMembership(
            com.example.transportapp.core.database.entity.MembershipEntity(
                local_id = membershipId, server_id = null,
                updated_at_local = now, updated_at_server = null,
                sync_state = com.example.transportapp.core.database.envelope.SyncState.PENDING, deleted_at = null,
                company_id = companyId, user_name = email.substringBefore('@'), user_email = email,
                role = role, branch_scope = com.example.transportapp.core.database.entity.MembershipEntity.SCOPE_ALL,
                status = com.example.transportapp.core.database.entity.MembershipEntity.STATUS_INVITED,
                invited_by = invitedByName, invited_expires_at = now + 5L * 24 * 60 * 60 * 1000,
                display_expires = "expires in 5 days",
            ),
        )
        outboxWriter.enqueue(
            op = OutboxOp.INSERT,
            entityType = OutboxEntityType.MEMBERSHIP,
            entityLocalId = membershipId,
            payloadJson = """{"email":"$email","role":"$role","status":"INVITED"}""",
            now = now,
        )
        return com.example.transportapp.core.common.Result.success(Unit)
    }
}
