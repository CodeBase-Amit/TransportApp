package com.example.transportapp.data.transport.account

import com.example.transportapp.core.database.dao.NumberingDao
import com.example.transportapp.core.database.dao.OrgDao
import com.example.transportapp.core.database.outbox.OutboxEntityType
import com.example.transportapp.core.database.outbox.OutboxOp
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.data.transport.session.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
}
