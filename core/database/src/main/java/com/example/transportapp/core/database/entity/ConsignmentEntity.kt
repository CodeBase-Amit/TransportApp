package com.example.transportapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.transportapp.core.database.envelope.SyncState

/**
 * CONSIGNMENT_E (§16.1) — the one business transaction every document projects from.
 * `status_projection` is a derived convenience, rebuilt by the repository from the event
 * log (D1, §3.4 #3) — never written by a client edit path. Totals are denormalised from
 * CHARGE_LINE_E (§3.4 #2). `party_names` is the FTS denorm ("consignor; consignee").
 * Bilty numbers are unique per company; a cancelled number is retained, never reused (§7.1).
 *
 * No foreign keys to masters on purpose: masters tombstone rather than hard-delete, and a
 * cascading delete here would violate §16.2's "never hard-delete a synced row". Referential
 * integrity is a repository concern.
 */
@Entity(
    tableName = "CONSIGNMENT_E",
    indices = [
        Index(value = ["company_id"]),
        Index(value = ["series_id"]),
        Index(value = ["company_id", "bilty_no"], unique = true),
        Index(value = ["company_id", "booking_branch_id", "booked_at"]),
        Index(value = ["company_id", "status_projection", "expected_arrival"]),
        Index(value = ["company_id", "payment_mode", "status_projection"]),
        Index(value = ["company_id", "consignor_id", "freight_bill_id"]),
        Index(value = ["company_id", "consignee_id", "freight_bill_id"]),
        Index(value = ["route_id"]),
    ],
)
data class ConsignmentEntity(
    @PrimaryKey val local_id: String,
    val server_id: String?,
    val updated_at_local: Long,
    val updated_at_server: Long?,
    val sync_state: SyncState,
    val deleted_at: Long?,
    val company_id: String,
    val series_id: String,
    /** Final number once stamped; null while a provisional number is in force (§9). */
    val bilty_no: String?,
    /** The PROV- number retained forever alongside the final one (§9). */
    val provisional_no: String?,
    /** Derived from the event log (§7.1) — BOOKED / LOADED / … */
    val status_projection: String,
    val booking_branch_id: String,
    val dest_branch_id: String?,
    val consignor_id: String,
    val consignee_id: String,
    val route_id: String,
    val from_station_id: String,
    val to_station_id: String,
    /** PAID / TOPAY / TBB (§16.1). */
    val payment_mode: String,
    /** OWNERS / CARRIERS. */
    val risk: String,
    /** GODOWN / DOOR. */
    val delivery_type: String,
    /** The stored §10.5 place of supply — printed and exported, never derived from stations. */
    val place_of_supply_state: String?,
    val eway_bill_no: String?,
    val private_mark: String?,
    val packages: Long,
    val actual_weight_g: Long,
    val chargeable_weight_g: Long,
    val declared_value_paise: Long,
    val freight_paise: Long,
    val gst_paise: Long,
    val total_paise: Long,
    val booked_at: Long,
    val booked_by_name: String,
    /** Computed once at booking (§7.3): booked_at + the route's transit days. */
    val expected_arrival: Long,
    /** "Consignor name; consignee name" — the CONSIGNMENT_FTS denorm. */
    val party_names: String,
    val freight_bill_id: String?,
    /** §16.1: an amendment is another consignment row linked to the original. */
    val amends_id: String?,
    /** §16.1: why the amendment exists — carried on the amendment row itself. */
    val amendment_reason: String?,
)
