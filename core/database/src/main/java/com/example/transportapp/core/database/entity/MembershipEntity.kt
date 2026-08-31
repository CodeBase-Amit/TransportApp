package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * MEMBERSHIP_E (§16.1): role + branch scope (a branch local_id or the [SCOPE_ALL] sentinel).
 * `user_name` is denormalised so timelines survive a member being removed (§7.2).
 */
@Entity(
    tableName = "MEMBERSHIP_E",
    indices = [
        Index(value = ["company_id"]),
        Index(value = ["user_email"]),
        Index(value = ["company_id", "user_email", "status"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["local_id"],
            childColumns = ["company_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MembershipEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val user_name: String,
    val user_email: String,
    /** OWNER / MANAGER / ACCOUNTANT / BOOKING_CLERK / DELIVERY_CLERK (§17.4.1). */
    val role: String,
    val branch_scope: String,
    /** ACTIVE / INVITED / DECLINED */
    val status: String,
    val invited_by: String?,
    val invited_expires_at: Long?,
    val display_expires: String?,
) {
    companion object {
        const val SCOPE_ALL = "ALL"
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_INVITED = "INVITED"
        const val STATUS_DECLINED = "DECLINED"
    }
}
