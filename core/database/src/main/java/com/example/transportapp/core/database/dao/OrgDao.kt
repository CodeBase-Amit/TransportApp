package com.example.transportapp.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.transportapp.core.database.entity.BranchEntity
import com.example.transportapp.core.database.entity.CompanyEntity
import com.example.transportapp.core.database.entity.MembershipEntity
import kotlinx.coroutines.flow.Flow

/** Org reads always filter tombstones (Spec.md §6.2). */
@Dao
interface OrgDao {

    // ── COMPANY_E ───────────────────────────────────────────────────────
    @Upsert
    suspend fun upsertCompany(entity: CompanyEntity)

    @Query("SELECT * FROM COMPANY_E WHERE deleted_at IS NULL ORDER BY name")
    fun observeCompanies(): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM COMPANY_E WHERE local_id = :localId AND deleted_at IS NULL")
    suspend fun getCompany(localId: String): CompanyEntity?

    // ── BRANCH_E ────────────────────────────────────────────────────────
    @Upsert
    suspend fun upsertBranch(entity: BranchEntity)

    @Query("SELECT * FROM BRANCH_E WHERE deleted_at IS NULL ORDER BY is_head_office DESC, name")
    fun observeBranches(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM BRANCH_E WHERE company_id = :companyId AND deleted_at IS NULL ORDER BY is_head_office DESC, name")
    suspend fun getBranchesForCompany(companyId: String): List<BranchEntity>

    @Query("SELECT COUNT(*) FROM BRANCH_E WHERE company_id = :companyId AND deleted_at IS NULL")
    suspend fun countBranches(companyId: String): Int

    // ── MEMBERSHIP_E ────────────────────────────────────────────────────
    @Upsert
    suspend fun upsertMembership(entity: MembershipEntity)

    @Query("SELECT * FROM MEMBERSHIP_E WHERE deleted_at IS NULL")
    fun observeMemberships(): Flow<List<MembershipEntity>>

    @Query("SELECT * FROM MEMBERSHIP_E WHERE local_id = :localId AND deleted_at IS NULL")
    suspend fun getMembership(localId: String): MembershipEntity?

    @Query("UPDATE MEMBERSHIP_E SET status = :status, sync_state = 'PENDING', updated_at_local = :now WHERE local_id = :localId")
    suspend fun setMembershipStatus(localId: String, status: String, now: Long)

    @Query("SELECT COUNT(*) FROM MEMBERSHIP_E WHERE company_id = :companyId AND role = 'OWNER' AND status = 'ACTIVE' AND deleted_at IS NULL")
    suspend fun countActiveOwners(companyId: String): Int
}
