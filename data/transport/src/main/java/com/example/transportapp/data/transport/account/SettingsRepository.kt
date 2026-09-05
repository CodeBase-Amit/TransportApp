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

/** Company profile payload for the outbox (§16.2). S27 (D66): the full letterhead. */
internal fun companyProfilePayload(
    name: String, legalName: String, address: String, gstin: String, pan: String, transporterId: String?,
    constitution: String?, city: String?, pincode: String?, state: String?,
    phone: String?, altPhone: String?, email: String?, website: String?, footerClause: String?,
): String = org.json.JSONObject()
    .put("name", name)
    .put("legal_name", legalName)
    .put("address", address)
    .put("gstin", gstin)
    .put("pan", pan)
    .put("transporter_id", transporterId)
    .put("constitution", constitution)
    .put("city", city)
    .put("pincode", pincode)
    .put("state", state)
    .put("phone", phone)
    .put("alt_phone", altPhone)
    .put("email", email)
    .put("website", website)
    .put("footer_clause", footerClause)
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
    private val photoImporter: com.example.transportapp.data.transport.tracking.PhotoImporter,
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
        val logoRef: String?,
        val constitution: String? = null,
        val city: String? = null,
        val pincode: String? = null,
        val state: String? = null,
        val phone: String? = null,
        val altPhone: String? = null,
        val email: String? = null,
        val website: String? = null,
        val footerClause: String? = null,
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
            logoRef = company.logo_ref,
            constitution = company.constitution,
            city = company.city,
            pincode = company.pincode,
            state = company.state,
            phone = company.phone,
            altPhone = company.alt_phone,
            email = company.email,
            website = company.website,
            footerClause = company.footer_clause,
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
        constitution: String? = null,
        city: String? = null,
        pincode: String? = null,
        state: String? = null,
        phone: String? = null,
        altPhone: String? = null,
        email: String? = null,
        website: String? = null,
        footerClause: String? = null,
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
                constitution = constitution,
                city = city,
                pincode = pincode,
                state = state,
                phone = phone,
                alt_phone = altPhone,
                email = email,
                website = website,
                footer_clause = footerClause,
                updated_at_local = System.currentTimeMillis(),
            ),
        )
        outboxWriter.enqueue(
            op = com.example.transportapp.core.database.outbox.OutboxOp.UPDATE,
            entityType = com.example.transportapp.core.database.outbox.OutboxEntityType.COMPANY,
            entityLocalId = companyId,
            payloadJson = com.example.transportapp.data.transport.account.companyProfilePayload(
                name, legalName, address, gstin, pan, transporterId,
                constitution, city, pincode, state, phone, altPhone, email, website, footerClause,
            ),
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

    /**
     * S27 — the id-keyed form of the §9 counter change: the T28 dialog resolves the series
     * by its local id directly (the label-parsing path failed on every seed row because the
     * label carries the branch_id, not a branch name). Owner-only; same forward-only rule
     * and audit outbox row as [changeSeriesCounter].
     */
    suspend fun changeSeriesCounterById(companyId: String, seriesLocalId: String, newLastIssued: Long): com.example.transportapp.core.common.Result<Unit> {
        val session = sessionRepository.session.first()
        if (session.role != "OWNER") {
            return com.example.transportapp.core.common.Result.failure(
                com.example.transportapp.core.common.ErrorCode.AUTH_NO_ACCESS, "Only the Owner can change a numbering counter"
            )
        }
        val series = numberingDao.getSeriesById(companyId, seriesLocalId)
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

    /** S21: the invite row's X resolves by email (the UI row carries no membership id). */
    suspend fun cancelInvitationByMail(companyId: String, email: String): com.example.transportapp.core.common.Result<Unit> {
        val membership = orgDao.observeMemberships().first().firstOrNull {
            it.company_id == companyId && it.user_email == email &&
                it.status == com.example.transportapp.core.database.entity.MembershipEntity.STATUS_INVITED &&
                it.deleted_at == null
        } ?: return com.example.transportapp.core.common.Result.failure(
            com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE, "That invitation no longer exists"
        )
        return cancelInvitation(membership.local_id)
    }

    /**
     * S22 — save the company logo (D60): the picked image is imported into app files,
     * COMPANY_E.logo_ref is set, and the change rides the outbox.
     */
    suspend fun saveLogo(companyId: String, source: android.net.Uri): com.example.transportapp.core.common.Result<String> {
        val imported = photoImporter.importToAppFiles(source, "logos")
            ?: return com.example.transportapp.core.common.Result.failure(
                com.example.transportapp.core.common.ErrorCode.PHOTO_QUALITY, "That image could not be read. Try another one."
            )
        val company = orgDao.getCompany(companyId) ?: return com.example.transportapp.core.common.Result.failure(
            com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE, "No company on this device"
        )
        val now = System.currentTimeMillis()
        orgDao.upsertCompany(company.copy(logo_ref = imported.first, updated_at_local = now))
        outboxWriter.enqueue(
            op = OutboxOp.UPDATE,
            entityType = OutboxEntityType.COMPANY,
            entityLocalId = companyId,
            payloadJson = """{"logo_ref":"${imported.first}"}""",
            now = now,
        )
        return com.example.transportapp.core.common.Result.success(imported.first)
    }

    /**
     * S21 — add a branch (T26's Add a branch): Owner-only; BRANCH_E + outbox INSERT.
     * A branch name that already exists in the company is refused.
     */
    suspend fun addBranch(companyId: String, name: String, code: String, address: String): com.example.transportapp.core.common.Result<Unit> {
        val session = sessionRepository.session.first()
        if (session.role != "OWNER" && session.role != "MANAGER") {
            return com.example.transportapp.core.common.Result.failure(
                com.example.transportapp.core.common.ErrorCode.AUTH_NO_ACCESS, "Only a Manager can add a branch"
            )
        }
        if (name.isBlank() || code.isBlank()) {
            return com.example.transportapp.core.common.Result.failure(
                com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE, "Branch name and code are required"
            )
        }
        val existing = orgDao.getBranchesForCompany(companyId).firstOrNull { it.deleted_at == null && it.name.equals(name.trim(), ignoreCase = true) }
        if (existing != null) {
            return com.example.transportapp.core.common.Result.failure(
                com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE, "A branch named $name already exists"
            )
        }
        val now = System.currentTimeMillis()
        val branchId = java.util.UUID.randomUUID().toString()
        orgDao.upsertBranch(
            com.example.transportapp.core.database.entity.BranchEntity(
                local_id = branchId, server_id = null,
                updated_at_local = now, updated_at_server = null,
                sync_state = com.example.transportapp.core.database.envelope.SyncState.PENDING, deleted_at = null,
                company_id = companyId, name = name.trim(), code = code.trim().uppercase(),
                address = address.trim().takeIf { it.isNotEmpty() }, is_head_office = false,
            ),
        )
        outboxWriter.enqueue(
            op = OutboxOp.INSERT,
            entityType = OutboxEntityType.BRANCH,
            entityLocalId = branchId,
            payloadJson = """{"name":"${name.trim()}","code":"${code.trim().uppercase()}"}""",
            now = now,
        )
        return com.example.transportapp.core.common.Result.success(Unit)
    }

    /**
     * S21 — cancel an invitation: the membership row is tombstoned (never hard-deleted,
     * §16.2) and the change rides the outbox. Owner-only.
     */
    suspend fun cancelInvitation(membershipLocalId: String): com.example.transportapp.core.common.Result<Unit> {
        val session = sessionRepository.session.first()
        if (session.role != "OWNER") {
            return com.example.transportapp.core.common.Result.failure(
                com.example.transportapp.core.common.ErrorCode.AUTH_NO_ACCESS, "Only the Owner can cancel an invitation"
            )
        }
        val now = System.currentTimeMillis()
        val membership = orgDao.getMembership(membershipLocalId)
            ?: return com.example.transportapp.core.common.Result.failure(
                com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE, "That invitation no longer exists"
            )
        orgDao.upsertMembership(
            membership.copy(deleted_at = now, updated_at_local = now),
        )
        outboxWriter.enqueue(
            op = OutboxOp.DELETE,
            entityType = OutboxEntityType.MEMBERSHIP,
            entityLocalId = membershipLocalId,
            payloadJson = """{"email":"${membership.user_email}"}""",
            now = now,
        )
        return com.example.transportapp.core.common.Result.success(Unit)
    }

    /**
     * S21 — the §17.4.1 invite: Owner-only; writes an INVITED membership row with a
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
