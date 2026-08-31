package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/** BRANCH_E (§16.1) — a company's branches; the code prints inside every bilty number. */
@Entity(
    tableName = "BRANCH_E",
    indices = [
        Index(value = ["company_id"]),
        Index(value = ["company_id", "name"], unique = true),
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
data class BranchEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val name: String,
    val code: String,
    val address: String?,
    val is_head_office: Boolean,
)
