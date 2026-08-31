package com.example.transportapp.domain.transport.org

import com.example.transportapp.domain.transport.Role

/** Org read models (§16.1/§16.2) — what repositories hand to ViewModels. Pure Kotlin. */

data class CompanySummary(
    val localId: String,
    val name: String,
    val gstin: String?,
    val displayBiltySeries: String?,
)

data class BranchSummary(
    val localId: String,
    val companyId: String,
    val name: String,
    val code: String,
    val isHeadOffice: Boolean,
)

data class MembershipSummary(
    val localId: String,
    val companyId: String,
    val companyName: String,
    val userName: String,
    val userEmail: String,
    val role: Role,
    /** Branch local_id, or [MembershipScope.ALL]. */
    val branchScope: String,
    val status: MembershipStatus,
    val invitedBy: String?,
    val displayExpires: String?,
)

enum class MembershipStatus { ACTIVE, INVITED, DECLINED, UNKNOWN }

object MembershipScope {
    const val ALL = "ALL"
}

data class RegisterCompanyRequest(
    val companyName: String,
    val address: String?,
    val gstin: String?,
    val branchName: String,
    val branchCode: String,
    val ownerUserName: String,
    val ownerUserEmail: String,
)
