package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * LORRY_HIRE_E (§16.1) — exactly one per trip. Exactly one of `owner_party_id` or
 * `broker_id` names the payee (repository guard; when a broker is named, the balance is
 * payable to the broker, §11.2). The four amounts must reconcile:
 * balance = hire − advance − deductions.
 */
@Entity(
    tableName = "LORRY_HIRE_E",
    indices = [Index(value = ["trip_id"], unique = true)],
    foreignKeys = [
        ForeignKey(entity = TripEntity::class, parentColumns = ["local_id"], childColumns = ["trip_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class LorryHireEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val trip_id: String,
    val owner_party_id: String?,
    val broker_id: String?,
    val hire_paise: Long,
    val advance_paise: Long,
    val deductions_paise: Long,
    val balance_paise: Long,
)
