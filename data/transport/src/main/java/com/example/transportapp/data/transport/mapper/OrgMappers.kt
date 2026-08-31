package com.example.transportapp.data.transport.mapper

import com.example.transportapp.core.database.entity.BranchEntity
import com.example.transportapp.core.database.entity.CompanyEntity
import com.example.transportapp.core.database.entity.MembershipEntity
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.domain.transport.Role
import com.example.transportapp.domain.transport.org.BranchSummary
import com.example.transportapp.domain.transport.org.CompanySummary
import com.example.transportapp.domain.transport.org.MembershipStatus
import com.example.transportapp.domain.transport.org.MembershipSummary

/** Entity ↔ domain mapping for the org aggregate (Spec.md §6.1). Unknown enums degrade safely. */

fun CompanyEntity.toDomain() = CompanySummary(
    localId = local_id,
    name = name,
    gstin = gstin,
    displayBiltySeries = display_bilty_series,
)

fun BranchEntity.toDomain() = BranchSummary(
    localId = local_id,
    companyId = company_id,
    name = name,
    code = code,
    isHeadOffice = is_head_office,
)

fun MembershipEntity.toDomain() = MembershipSummary(
    localId = local_id,
    companyId = company_id,
    companyName = "", // resolved by the repository against COMPANY_E
    userName = user_name,
    userEmail = user_email,
    role = runCatching { Role.valueOf(role) }.getOrDefault(Role.DELIVERY_CLERK),
    branchScope = branch_scope,
    status = runCatching { MembershipStatus.valueOf(status) }.getOrDefault(MembershipStatus.UNKNOWN),
    invitedBy = invited_by,
    displayExpires = display_expires,
)

/** Fresh envelope for a new local row (Spec.md §6.2). */
object Envelope {
    fun fresh(now: Long) = EnvelopeFields(
        updatedAtLocal = now,
        syncState = SyncState.PENDING,
    )
}

data class EnvelopeFields(val updatedAtLocal: Long, val syncState: SyncState)
