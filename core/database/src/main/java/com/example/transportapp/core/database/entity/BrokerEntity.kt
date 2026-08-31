package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/** BROKER_E (§16.1) — optional broker on a lorry hire (§11.2). */
@Entity(
    tableName = "BROKER_E",
    indices = [Index(value = ["company_id"])],
)
data class BrokerEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val name: String,
    val phone: String?,
)
