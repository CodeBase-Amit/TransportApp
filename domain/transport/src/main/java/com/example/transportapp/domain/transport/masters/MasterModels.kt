package com.example.transportapp.domain.transport.masters

import com.example.transportapp.domain.transport.Role

/** Masters read models (§16.1/§4.3) — pure Kotlin. */

data class MasterCounts(
    val parties: Int,
    val stations: Int,
    val routes: Int,
    val branches: Int,
    val goods: Int,
    val chargeHeads: Int,
    val rateCards: Int,
    val vehicles: Int,
    val drivers: Int,
)

data class PartyListRow(
    val localId: String,
    val name: String,
    val phone: String,
    val station: String?,
    val biltyCount: Int,
    val initials: String,
    val isDuplicate: Boolean,
)

data class PartyDetail(
    val localId: String,
    val name: String,
    val phone: String,
    val email: String?,
    val street: String?,
    val station: String?,
    val pincode: String?,
    val gstin: String?,
    /** CONSIGNOR / CONSIGNEE / BOTH */
    val type: String,
    val usualRoute: String?,
    val usualPaymentMode: String?,
    val rateCardLabel: String?,
    val biltyCount: Int,
)

data class RateRow(
    val localId: String,
    val routeLabel: String,
    val goodsLabel: String,
    /** PER_KG / PER_TONNE / PER_PACKAGE / PER_TRIP / FIXED (§10.2). */
    val basis: String,
    val ratePaise: Long,
    val minQtyLabel: String?,
    val note: String?,
)

data class AutoCharge(
    val localId: String,
    val label: String,
    val displayValue: String?,
    val enabled: Boolean,
)

data class DuplicatePair(
    val keepId: String,
    val keepName: String,
    val mergeId: String,
    val mergeName: String,
)

/** Who may edit masters: Manager and above (§17.4.1). */
fun canEditMasters(role: Role): Boolean = role.rank >= Role.MANAGER.rank
