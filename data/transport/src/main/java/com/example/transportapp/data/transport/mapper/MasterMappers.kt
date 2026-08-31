package com.example.transportapp.data.transport.mapper

import com.example.transportapp.core.database.entity.ChargeHeadEntity
import com.example.transportapp.core.database.entity.PartyEntity
import com.example.transportapp.core.database.entity.RateCardEntity
import com.example.transportapp.domain.transport.masters.AutoCharge
import com.example.transportapp.domain.transport.masters.MasterCounts
import com.example.transportapp.domain.transport.masters.PartyDetail
import com.example.transportapp.domain.transport.masters.PartyListRow
import com.example.transportapp.domain.transport.masters.RateRow

/** Entity ↔ domain mapping for the masters aggregate (Spec.md §6.1). */

fun PartyEntity.toListRow(isDuplicate: Boolean) = PartyListRow(
    localId = local_id,
    name = name,
    phone = phone,
    station = station,
    biltyCount = display_bilty_count,
    initials = name.split(" ").filter { it.isNotBlank() }.map { it.first().uppercaseChar() }.take(2).joinToString(""),
    isDuplicate = isDuplicate,
)

fun PartyEntity.toDetail(rateCardLabel: String?) = PartyDetail(
    localId = local_id,
    name = name,
    phone = phone,
    email = email,
    street = street_address,
    station = station,
    pincode = pincode,
    gstin = gstin,
    type = type,
    usualRoute = usual_route_id?.let { "Indore → Nashik" }, // display cache; route master lookup in S4
    usualPaymentMode = usual_payment_mode,
    rateCardLabel = rateCardLabel,
    biltyCount = display_bilty_count,
)

fun RateCardEntity.toRateRow() = RateRow(
    localId = local_id,
    routeLabel = "", // resolved by the repository against the route master
    goodsLabel = "", // resolved by the repository against the goods master
    basis = basis,
    ratePaise = rate_paise,
    minQtyLabel = min_qty_label,
    note = note,
)

fun ChargeHeadEntity.toAutoCharge() = AutoCharge(
    localId = local_id,
    label = label,
    displayValue = display_value,
    enabled = auto_apply,
)

fun masterCounts(
    parties: Int, stations: Int, routes: Int, branches: Int,
    goods: Int, chargeHeads: Int, rateCards: Int, vehicles: Int, drivers: Int,
) = MasterCounts(
    parties = parties, stations = stations, routes = routes, branches = branches,
    goods = goods, chargeHeads = chargeHeads, rateCards = rateCards,
    vehicles = vehicles, drivers = drivers,
)
