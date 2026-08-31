package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * RATE_CARD_E (§16.1) — one row is one rate: scope (party? route? goods?), basis, rate in
 * paise, and the minimum-quantity label ("500 kg"). Rate resolution order is §3; the rows
 * a party-scoped card carries are what T20 edits.
 */
@Entity(
    tableName = "RATE_CARD_E",
    indices = [
        Index(value = ["company_id"]),
        Index(value = ["party_id"]),
        Index(value = ["company_id", "party_id", "route_id", "goods_id"]),
    ],
    foreignKeys = [
        ForeignKey(entity = PartyEntity::class, parentColumns = ["local_id"], childColumns = ["party_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = RouteEntity::class, parentColumns = ["local_id"], childColumns = ["route_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = GoodsEntity::class, parentColumns = ["local_id"], childColumns = ["goods_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class RateCardEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val party_id: String?,
    val route_id: String?,
    val goods_id: String?,
    /** PER_KG / PER_TONNE / PER_PACKAGE / PER_TRIP / FIXED (§10.2). */
    val basis: String,
    val rate_paise: Long,
    val min_qty_label: String?,
    /** §10.2 minimum-freight floor in paise, applied after the basis computation. */
    val min_freight_paise: Long?,
    /** §10.2 optional maximum-freight ceiling in paise. */
    val max_freight_paise: Long?,
    val note: String?,
    val sort_order: Int,
)
