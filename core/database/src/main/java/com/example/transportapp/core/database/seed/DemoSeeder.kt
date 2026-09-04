package com.example.transportapp.core.database.seed

import androidx.room.withTransaction
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.entity.BranchEntity
import com.example.transportapp.core.database.entity.ChargeHeadEntity
import com.example.transportapp.core.database.entity.ChargeLineEntity
import com.example.transportapp.core.database.entity.CompanyEntity
import com.example.transportapp.core.database.entity.CompanySettingEntity
import com.example.transportapp.core.database.entity.ConsignmentEntity
import com.example.transportapp.core.database.entity.ConsignmentItemEntity
import com.example.transportapp.core.database.entity.DocSnapshotEntity
import com.example.transportapp.core.database.entity.DriverEntity
import com.example.transportapp.core.database.entity.FreightBillEntity
import com.example.transportapp.core.database.entity.GoodsEntity
import com.example.transportapp.core.database.entity.MembershipEntity
import com.example.transportapp.core.database.entity.NumberLeaseEntity
import com.example.transportapp.core.database.entity.NumberSeriesEntity
import com.example.transportapp.core.database.entity.PartyEntity
import com.example.transportapp.core.database.entity.RateCardEntity
import com.example.transportapp.core.database.entity.ReceiptEntity
import com.example.transportapp.core.database.entity.RouteEntity
import com.example.transportapp.core.database.entity.StationEntity
import com.example.transportapp.core.database.entity.StatusEventEntity
import com.example.transportapp.core.database.entity.TemplateEntity
import com.example.transportapp.core.database.entity.VehicleEntity
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The demo dataset (TransportApp.md §B6), seeded as entities on first launch (Phase2.md §3.5,
 * D8). Values equal what the Phase-1 prototype displayed — the visible screens are the
 * acceptance fixture. Version-gated by SEED_VERSION; bumping re-seeds on next launch.
 *
 * v1 seeds the org (companies, branches, memberships). v2 adds the nine master families
 * with §B6 counts (1,284 parties incl. the 7 duplicate-flagged, 96 stations, 141 routes,
 * 38 goods, 22 vehicles, 17 drivers, 9 charge heads, 64 rate rows). v3 (S4) makes charge
 * heads machine-readable for the calculation engine, gives named stations their real
 * states for the §10.5 place-of-supply comparison, and adds the §3 company-default rate
 * row (65 rate rows). Consignments (S5) and money (S9) extend this file per sprint.
 */
@Singleton
class DemoSeeder @Inject constructor(
    private val database: TransportDatabase,
) {

    suspend fun seedIfNeeded(now: Long = System.currentTimeMillis()) {
        val current = database.seedVersionDao().get()
        if (current?.version == SeedVersionEntity.SeedVersion.CURRENT) return

        database.withTransaction {
            seedOrg(now)
            seedMasters(now)
            seedNumbering(now)
            seedConsignments(now)
            seedMoney(now)
            seedTemplates(now)
            seedSettings(now)
            database.seedVersionDao().upsert(SeedVersionEntity(version = SeedVersionEntity.SeedVersion.CURRENT, seeded_at = now))
        }
    }

    private suspend fun seedOrg(now: Long) {
        val orgDao = database.orgDao()

        val shivshakti = CompanyEntity(
            local_id = ID_SHIVSHAKTI,
            server_id = null,
            updated_at_local = now,
            updated_at_server = null,
            sync_state = SyncState.SYNCED, // seeded rows are the device's mirror of demo truth
            deleted_at = null,
            name = "Shivshakti Roadlines",
            legal_name = "Shivshakti Roadlines",
            address = "Transport Nagar, Indore 452003",
            gstin = "23AABCS4521M1Z9",
            pan = "AABCS4521M",
            transporter_id = null,
            gst_treatment = "FORWARD",
            display_bilty_series = "IND/2627 · next number 04189",
                logo_ref = null,
        )
        val bharatCargo = CompanyEntity(
            local_id = ID_BHARAT_CARGO,
            server_id = null,
            updated_at_local = now,
            updated_at_server = null,
            sync_state = SyncState.SYNCED,
            deleted_at = null,
            name = "Bharat Cargo Carriers",
            legal_name = "Bharat Cargo Carriers",
            address = null,
            gstin = null,
            pan = null,
            transporter_id = null,
            gst_treatment = "FORWARD",
            display_bilty_series = null,
                logo_ref = null,
        )
        val malwa = CompanyEntity(
            local_id = ID_MALWA,
            server_id = null,
            updated_at_local = now,
            updated_at_server = null,
            sync_state = SyncState.SYNCED,
            deleted_at = null,
            name = "Malwa Goods Transport",
            legal_name = "Malwa Goods Transport",
            address = null,
            gstin = null,
            pan = null,
            transporter_id = null,
            gst_treatment = "FORWARD",
            display_bilty_series = null,
                logo_ref = null,
        )
        orgDao.upsertCompany(shivshakti)
        orgDao.upsertCompany(bharatCargo)
        orgDao.upsertCompany(malwa)

        val indore = BranchEntity(
            local_id = ID_BRANCH_INDORE, server_id = null, updated_at_local = now, updated_at_server = null,
            sync_state = SyncState.SYNCED, deleted_at = null, company_id = ID_SHIVSHAKTI,
            name = "Indore", code = "IND", address = "Transport Nagar, Indore 452003", is_head_office = true,
        )
        val nagpur = BranchEntity(
            local_id = ID_BRANCH_NAGPUR, server_id = null, updated_at_local = now, updated_at_server = null,
            sync_state = SyncState.SYNCED, deleted_at = null, company_id = ID_SHIVSHAKTI,
            name = "Nagpur", code = "NAG", address = "Wadi Naka, Nagpur", is_head_office = false,
        )
        val bhiwandi = BranchEntity(
            local_id = ID_BRANCH_BHIWANDI, server_id = null, updated_at_local = now, updated_at_server = null,
            sync_state = SyncState.SYNCED, deleted_at = null, company_id = ID_SHIVSHAKTI,
            name = "Bhiwandi", code = "BHW", address = "Kalyan Road, Bhiwandi", is_head_office = false,
        )
        val bcNagpur = BranchEntity(
            local_id = ID_BRANCH_BC_NAGPUR, server_id = null, updated_at_local = now, updated_at_server = null,
            sync_state = SyncState.SYNCED, deleted_at = null, company_id = ID_BHARAT_CARGO,
            name = "Nagpur", code = "BHN", address = null, is_head_office = true,
        )
        listOf(indore, nagpur, bhiwandi, bcNagpur).forEach { orgDao.upsertBranch(it) }

        // Memberships for the mocked offline user (Phase2.md §1, decision 4).
        val mahesh = MembershipEntity(
            local_id = ID_MEMBERSHIP_SHIVSHAKTI_OWNER, server_id = null, updated_at_local = now, updated_at_server = null,
            sync_state = SyncState.SYNCED, deleted_at = null, company_id = ID_SHIVSHAKTI,
            user_name = "Mahesh Patidar", user_email = EMAIL_DEMO_USER, role = "OWNER",
            branch_scope = MembershipEntity.SCOPE_ALL, status = MembershipEntity.STATUS_ACTIVE,
            invited_by = null, invited_expires_at = null, display_expires = null,
        )
        // Shivshakti's other members (§B6) — they make T2's "4 members" and T27's roster real.
        val sunita = MembershipEntity(
            local_id = ID_MEMBERSHIP_SHIVSHAKTI_SUNITA, server_id = null, updated_at_local = now, updated_at_server = null,
            sync_state = SyncState.SYNCED, deleted_at = null, company_id = ID_SHIVSHAKTI,
            user_name = "Sunita Jain", user_email = "sunita.jain@gmail.com", role = "MANAGER",
            branch_scope = MembershipEntity.SCOPE_ALL, status = MembershipEntity.STATUS_ACTIVE,
            invited_by = null, invited_expires_at = null, display_expires = null,
        )
        val ramesh = MembershipEntity(
            local_id = ID_MEMBERSHIP_SHIVSHAKTI_RAMESH, server_id = null, updated_at_local = now, updated_at_server = null,
            sync_state = SyncState.SYNCED, deleted_at = null, company_id = ID_SHIVSHAKTI,
            user_name = "Ramesh Yadav", user_email = "ramesh.yadav@gmail.com", role = "BOOKING_CLERK",
            branch_scope = ID_BRANCH_NAGPUR, status = MembershipEntity.STATUS_ACTIVE,
            invited_by = null, invited_expires_at = null, display_expires = null,
        )
        val iqbal = MembershipEntity(
            local_id = ID_MEMBERSHIP_SHIVSHAKTI_IQBAL, server_id = null, updated_at_local = now, updated_at_server = null,
            sync_state = SyncState.SYNCED, deleted_at = null, company_id = ID_SHIVSHAKTI,
            user_name = "Iqbal Shaikh", user_email = "iqbal.shaikh@gmail.com", role = "DELIVERY_CLERK",
            branch_scope = ID_BRANCH_BHIWANDI, status = MembershipEntity.STATUS_ACTIVE,
            invited_by = null, invited_expires_at = null, display_expires = null,
        )
        val bharatClerk = MembershipEntity(
            local_id = ID_MEMBERSHIP_BHARAT_CLERK, server_id = null, updated_at_local = now, updated_at_server = null,
            sync_state = SyncState.SYNCED, deleted_at = null, company_id = ID_BHARAT_CARGO,
            user_name = "Mahesh Patidar", user_email = EMAIL_DEMO_USER, role = "BOOKING_CLERK",
            branch_scope = ID_BRANCH_BC_NAGPUR, status = MembershipEntity.STATUS_ACTIVE,
            invited_by = null, invited_expires_at = null, display_expires = null,
        )
        // T2 ground truth shows the pending invitation on the signed-in user's picker
        // (§B6 lists accounts.bhiwandi@gmail.com as the invitee; the picker renders the
        // invitation regardless, so it is seeded under the demo user's email).
        val malwaInvite = MembershipEntity(
            local_id = ID_MEMBERSHIP_MALWA_INVITE, server_id = null, updated_at_local = now, updated_at_server = null,
            sync_state = SyncState.SYNCED, deleted_at = null, company_id = ID_MALWA,
            user_name = "Mahesh Patidar", user_email = EMAIL_DEMO_USER, role = "ACCOUNTANT",
            branch_scope = MembershipEntity.SCOPE_ALL, status = MembershipEntity.STATUS_INVITED,
            invited_by = "sunita.jain@gmail.com", invited_expires_at = now + FIVE_DAYS_MS, display_expires = "5 days",
        )
        listOf(mahesh, sunita, ramesh, iqbal, bharatClerk, malwaInvite).forEach { orgDao.upsertMembership(it) }
    }

    /**
     * The nine master families with §B6 counts. Named rows carry the ground-truth values the
     * screens show; generated rows pad to the canonical counts so T17's figures and the
     * party-search scale are real. Filler is deterministic (no RNG — Spec.md §14.2).
     */
    private suspend fun seedMasters(now: Long) {
        val dao = database.mastersDao()
        val company = SeedIds.COMPANY_SHIVSHAKTI

        fun envelopeFields() = Triple(now, null as Long?, SyncState.SYNCED)

        // ── Stations (96) — named rows carry their real state, the §10.5 place-of-supply
        //    comparison is only honest if station.state is the state's full name ──
        val namedStations = listOf(
            "Indore", "Nashik", "Dhule", "Bhusawal", "Nagpur", "Bhiwandi", "Pune", "Ahmedabad",
            "Jaipur", "Kalyan", "Dewas", "Ujjain", "Ratlam", "Bhopal", "Mumbai", "Surat",
            "Vadodara", "Delhi",
        )
        val stateOfStation = mapOf(
            "Indore" to "Madhya Pradesh", "Dewas" to "Madhya Pradesh", "Ujjain" to "Madhya Pradesh",
            "Ratlam" to "Madhya Pradesh", "Bhopal" to "Madhya Pradesh",
            "Nashik" to "Maharashtra", "Mumbai" to "Maharashtra", "Kalyan" to "Maharashtra",
            "Bhiwandi" to "Maharashtra", "Pune" to "Maharashtra", "Dhule" to "Maharashtra",
            "Bhusawal" to "Maharashtra", "Nagpur" to "Maharashtra",
            "Ahmedabad" to "Gujarat", "Surat" to "Gujarat", "Vadodara" to "Gujarat",
            "Jaipur" to "Rajasthan", "Delhi" to "Delhi",
        )
        val stations = buildList {
            namedStations.forEachIndexed { i, name ->
                add(StationEntity("seed-station-$i", null, now, null, SyncState.SYNCED, null, company, name, stateOfStation.getValue(name)))
            }
            for (i in namedStations.size until 96) {
                add(StationEntity("seed-station-$i", null, now, null, SyncState.SYNCED, null, company, "Station %02d".format(i), "Madhya Pradesh"))
            }
        }
        stations.forEach { dao.upsertStation(it) }
        val stationByName = stations.associate { it.name to it.local_id }

        // ── Routes (141) ────────────────────────────────────────────────
        val namedRoutes = listOf(
            Triple("Indore", "Nashik", SeedIds.ROUTE_INDORE_NASHIK to (585 to 2)),
            Triple("Indore", "Pune", "seed-route-indore-pune" to (400 to 2)),
            Triple("Indore", "Mumbai", "seed-route-indore-mumbai" to (510 to 2)),
            Triple("Indore", "Bhiwandi", "seed-route-indore-bhiwandi" to (530 to 2)),
            Triple("Nagpur", "Nashik", "seed-route-nagpur-nashik" to (600 to 3)),
            Triple("Indore", "Bhusawal", "seed-route-indore-bhusawal" to (250 to 1)),
            Triple("Indore", "Dhule", "seed-route-indore-dhule" to (160 to 1)),
            Triple("Indore", "Kalyan", "seed-route-indore-kalyan" to (520 to 2)),
        )
        val usedPairs = mutableSetOf<Pair<String, String>>()
        val routes = buildList {
            namedRoutes.forEach { (from, to, idKm) ->
                val o = stationByName.getValue(from)
                val d = stationByName.getValue(to)
                usedPairs.add(o to d)
                add(
                    RouteEntity(
                        idKm.first, null, now, null, SyncState.SYNCED, null, company,
                        o, d, idKm.second.first, idKm.second.second,
                    ),
                )
            }
            for (i in namedRoutes.size until 141) {
                val oi = i % stations.size
                val o = stations[oi].local_id
                var di = (oi + 1) % stations.size
                while ((o to stations[di].local_id) in usedPairs && di != oi) {
                    di = (di + 1) % stations.size
                }
                val d = stations[di].local_id
                if (o to d !in usedPairs) {
                    usedPairs.add(o to d)
                    add(RouteEntity("seed-route-gen-$i", null, now, null, SyncState.SYNCED, null, company, o, d, 100 + (i * 13) % 700, 1 + (i % 3)))
                }
            }
        }
        routes.forEach { dao.upsertRoute(it) }
        fun routeId(from: String, to: String) =
            routes.first { it.origin_station_id == stationByName.getValue(from) && it.dest_station_id == stationByName.getValue(to) }.local_id

        // ── Goods (38) ──────────────────────────────────────────────────
        val namedGoods = listOf("MS pipes" to SeedIds.GOODS_MS_PIPES, "TMT Bars" to null, "Angles" to null, "Cement" to null, "Cotton bales" to null)
        val goods = buildList {
            namedGoods.forEachIndexed { i, (name, id) ->
                add(GoodsEntity(id ?: "seed-goods-$i", null, now, null, SyncState.SYNCED, null, company, name))
            }
            for (i in namedGoods.size until 38) {
                add(GoodsEntity("seed-goods-$i", null, now, null, SyncState.SYNCED, null, company, "Goods %02d".format(i)))
            }
        }
        goods.forEach { dao.upsertGoods(it) }
        fun goodsId(name: String) = goods.first { it.name == name }.local_id

        // ── Parties (1,284) — 7 duplicate-flagged (3 pairs + 1 generated pair) ──
        data class PartySeed(val name: String, val phone: String, val bilties: Int, val id: String? = null, val station: String? = null)
        val namedParties = listOf(
            PartySeed("Agarwal Logistics Pvt Ltd", "+91 98765 43210", 12),
            PartySeed("Krishna Alloys", "+91 98765 43210", 18, station = "Dewas"), // duplicate of Agarwal
            PartySeed("Balaji Roadways", "+91 91234 56789", 6),
            PartySeed("Choudhary Transport", "+91 99887 76655", 9),
            PartySeed("Choudhary Transport Co", "+91 99887 76655", 3), // duplicate
            PartySeed("Deepak Steel Traders", "+91 94250 61183", 41, id = SeedIds.PARTY_DEEPAK_STEEL, station = "Indore"),
            PartySeed("Deepak Steel Trader", "+91 94250 61183", 2, id = SeedIds.PARTY_DEEPAK_DUPLICATE), // duplicate
            PartySeed("Nashik Hardware Mart", "+91 98600 27419", 27, id = SeedIds.PARTY_NASHIK_HARDWARE, station = "Nashik"),
            PartySeed("Kalyan Steel Mart", "+91 98220 33445", 7, station = "Kalyan"),
            PartySeed("Express Freight Carriers", "+91 90000 11111", 14),
            PartySeed("First Line Logistics", "+91 88888 22222", 22),
        )
        val prefixes = listOf("Agarwal", "Bansal", "Bharat", "Deep", "Gupta", "Jain", "Kumar", "Malwa", "Patel", "Rathore", "Sharma", "Singh", "Verma", "Yadav")
        val suffixes = listOf("Traders", "Transport", "Logistics", "Roadlines", "Carriers")
        val parties = buildList {
            namedParties.forEach { seed ->
                add(
                    PartyEntity(
                        local_id = seed.id ?: "seed-party-${seed.name.hashCode()}", server_id = null, now, null,
                        SyncState.SYNCED, null, company, seed.name, seed.phone,
                        email = null, type = "BOTH",
                        street_address = if (seed.station != null) "Plot 14, Transport Nagar" else null,
                        station = seed.station, pincode = if (seed.station != null) "452003" else null,
                        gstin = if (seed.name.startsWith("Deepak Steel Traders")) "23AACDS8812K1Z4" else null,
                        usual_route_id = if (seed.name.startsWith("Deepak Steel Traders")) SeedIds.ROUTE_INDORE_NASHIK else null,
                        usual_payment_mode = if (seed.name.startsWith("Deepak Steel Traders")) "TBB" else null,
                        display_bilty_count = seed.bilties,
                    ),
                )
            }
            var used = 212 - namedParties.sumOf { it.bilties }.coerceAtLeast(0)
            // Three filler slots carry the register-fixture parties the consignments reference.
            val pinnedFillers = mapOf(
                11 to Triple("Sai Electricals", "Dhule", 18),
                12 to Triple("Vidarbha Traders", "Nagpur", 6),
                13 to Triple("Bhusawal Cement Agency", "Bhusawal", 12),
            )
            for (i in 0 until (1284 - namedParties.size)) {
                val dupPair = i == 500 // the 7th flagged duplicate rides on Balaji's phone
                val pinned = pinnedFillers[i]
                val name = pinned?.first ?: "${prefixes[i % prefixes.size]} ${suffixes[(i / prefixes.size) % suffixes.size]} %03d".format(i)
                val phone = when {
                    dupPair -> "+91 99887 76655" // joins Choudhary's group — the 7th flagged duplicate
                    else -> "+91 9%08d".format(50000000 + i * 7)
                }
                val bilties = if (pinned != null) pinned.third else if (used > 0) ((i % 5) + 1).also { used -= it } else 0
                add(
                    PartyEntity(
                        "seed-party-gen-$i", null, now, null, SyncState.SYNCED, null, company, name, phone,
                        email = null, type = "BOTH",
                        street_address = if (pinned != null) "Plot 7, Transport Nagar" else null,
                        station = pinned?.second, pincode = if (pinned != null) "452003" else null,
                        gstin = null, usual_route_id = null, usual_payment_mode = null, display_bilty_count = bilties,
                    ),
                )
            }
        }
        parties.forEach { dao.upsertParty(it) }

        // ── Vehicles (22) & drivers (17) ────────────────────────────────
        val vehicles = buildList {
            add(VehicleEntity(SeedIds.VEHICLE_MH15BK4412, null, now, null, SyncState.SYNCED, null, company, "MH 15 BK 4412", 9000, "OWN"))
            add(VehicleEntity("seed-vehicle-mp09gh2207", null, now, null, SyncState.SYNCED, null, company, "MP 09 GH 2207", 8000, "OWN"))
            add(VehicleEntity("seed-vehicle-gj05kt8891", null, now, null, SyncState.SYNCED, null, company, "GJ 05 KT 8891", 9000, "ATTACHED"))
            add(VehicleEntity("seed-vehicle-rj14pa3345", null, now, null, SyncState.SYNCED, null, company, "RJ 14 PA 3345", 7500, "OWN"))
            for (i in 4 until 22) {
                add(VehicleEntity("seed-vehicle-$i", null, now, null, SyncState.SYNCED, null, company, "MP 09 XY 4%03d".format(i), 6000 + (i % 4) * 1000, if (i % 3 == 0) "ATTACHED" else "OWN"))
            }
        }
        vehicles.forEach { dao.upsertVehicle(it) }

        val drivers = buildList {
            add(DriverEntity(SeedIds.DRIVER_GURMEET, null, now, null, SyncState.SYNCED, null, company, "Gurmeet Singh", "MH1520190004512", "+91 90280 41176"))
            add(DriverEntity("seed-driver-balwinder", null, now, null, SyncState.SYNCED, null, company, "Balwinder Singh", "PB0320190011223", "+91 98140 22331"))
            add(DriverEntity("seed-driver-rafiq", null, now, null, SyncState.SYNCED, null, company, "Rafiq Mansuri", "MH1220200007741", "+91 90220 55110"))
            for (i in 3 until 17) {
                add(DriverEntity("seed-driver-$i", null, now, null, SyncState.SYNCED, null, company, "Driver %02d".format(i), "MH%02d2021000%04d".format(10 + i, i), "+91 90000 6%04d".format(i)))
            }
        }
        drivers.forEach { dao.upsertDriver(it) }

        // ── Charge heads (9) — §10.3 data: basis is a machine token the S4 calculator
        //    interprets; default_value_paise is paise for flat/per-package bases and
        //    percent×100 for percent bases; the three §3 auto-charge templates are on ──
        data class HeadSeed(
            val code: String, val label: String, val basis: String,
            val defaultValuePaise: Long, val taxable: Boolean, val auto: Boolean,
            val display: String? = null,
        )
        val chargeHeads = listOf(
            HeadSeed("freight", "Freight", "COMPUTED", 0, taxable = true, auto = false),
            HeadSeed("hamali", "Hamali", "PER_PACKAGE", 800, taxable = true, auto = true, display = "₹8.00 / art"),
            HeadSeed("unloading", "Unloading", "PER_PACKAGE", 1000, taxable = true, auto = false, display = "₹10.00 / art"),
            HeadSeed("door_delivery", "Door delivery", "FLAT", 15000, taxable = true, auto = true, display = "₹150.00 / LR"),
            HeadSeed("surcharge", "Surcharge", "PERCENT_OF_FREIGHT", 500, taxable = true, auto = false, display = "5% of freight"),
            HeadSeed("demurrage", "Demurrage", "PER_DAY", 5000, taxable = true, auto = false, display = "₹50.00 / day"),
            HeadSeed("insurance", "Insurance", "PERCENT_OF_VALUE", 50, taxable = false, auto = false, display = "0.5% of value"),
            HeadSeed("other", "Other", "FLAT", 0, taxable = true, auto = false),
            HeadSeed("discount", "Discount", "FLAT", 0, taxable = true, auto = false),
        )
        chargeHeads.forEachIndexed { i, head ->
            dao.upsertChargeHead(
                ChargeHeadEntity(
                    "seed-chargehead-${head.code}", null, now, null, SyncState.SYNCED, null, company,
                    head.code, head.label, basis = head.basis, display_value = head.display,
                    default_value_paise = head.defaultValuePaise, bearer = "CONSIGNOR",
                    taxable = head.taxable, auto_apply = head.auto, sort_order = i,
                ),
            )
        }

        // ── Rate cards (65): 12 party-scoped rows for Deepak Steel Traders (T20),
        //    52 company route rows, and the §3 company-default row every booking falls
        //    back to when no party/route row matches ──
        data class RateSeed(val from: String, val to: String, val goods: String?, val basis: String, val rupees: String, val min: String, val note: String? = null)
        val deepakRates = listOf(
            RateSeed("Indore", "Nashik", "MS pipes", "PER_KG", "4.50", "500 kg"),
            RateSeed("Indore", "Pune", "Angles", "PER_TONNE", "3,200.00", "1 Ton"),
            RateSeed("Indore", "Mumbai", "TMT Bars", "PER_TONNE", "4,500.00", "5 Ton"),
            RateSeed("Indore", "Nashik", null, "PER_KG", "5.00", "500 kg", "wider — used when goods don't match"),
            RateSeed("Indore", "Bhiwandi", "MS pipes", "PER_KG", "4.20", "1,000 kg"),
            RateSeed("Indore", "Pune", null, "PER_PACKAGE", "180.00", "5 pkg"),
            RateSeed("Nagpur", "Nashik", "Cement", "PER_TONNE", "1,240.00", "3 t"),
            RateSeed("Indore", "Bhusawal", null, "PER_TONNE", "1,600.00", "2 t"),
            RateSeed("Indore", "Dhule", null, "PER_TONNE", "1,200.00", "1 t"),
            RateSeed("Indore", "Kalyan", "MS pipes", "PER_KG", "4.80", "500 kg"),
            RateSeed("Indore", "Bhiwandi", "Angles", "PER_TONNE", "3,600.00", "1 Ton"),
            RateSeed("Indore", "Mumbai", null, "PER_TONNE", "3,900.00", "2 Ton"),
        )
        fun parsePaise(text: String): Long {
            val plain = text.replace(",", "")
            return if (plain.contains('.')) {
                val (r, p) = plain.split('.')
                (r.toLong() * 100) + p.padEnd(2, '0').take(2).toLong()
            } else {
                plain.toLong() * 100
            }
        }
        deepakRates.forEachIndexed { i, rate ->
            dao.upsertRateCard(
                RateCardEntity(
                    "seed-rate-deepak-$i", null, now, null, SyncState.SYNCED, null, company,
                    party_id = SeedIds.PARTY_DEEPAK_STEEL,
                    route_id = routeId(rate.from, rate.to),
                    goods_id = rate.goods?.let { goodsId(it) },
                    basis = rate.basis, rate_paise = parsePaise(rate.rupees),
                    min_qty_label = rate.min, min_freight_paise = null, max_freight_paise = null,
                    note = rate.note, sort_order = i,
                ),
            )
        }
        for (i in 0 until 52) {
            val route = routes[(i * 3 + 5) % routes.size]
            dao.upsertRateCard(
                RateCardEntity(
                    "seed-rate-co-$i", null, now, null, SyncState.SYNCED, null, company,
                    party_id = null, route_id = route.local_id, goods_id = null,
                    basis = "PER_KG", rate_paise = 300L + (i % 30) * 15L,
                    min_qty_label = "500 kg", min_freight_paise = null, max_freight_paise = null,
                    note = null, sort_order = i,
                ),
            )
        }
        // The §3 step-5 company default: no scope at all, so every booking resolves.
        dao.upsertRateCard(
            RateCardEntity(
                "seed-rate-co-default", null, now, null, SyncState.SYNCED, null, company,
                party_id = null, route_id = null, goods_id = null,
                basis = "PER_KG", rate_paise = 450,
                min_qty_label = "500 kg", min_freight_paise = null, max_freight_paise = null,
                note = "company default", sort_order = 0,
            ),
        )
    }

    /**
     * §9 numbering: five series (Bilty/Challan/FB/RCPT Indore + Bilty Bhiwandi never-used),
     * plus the initial 50-number bilty lease whose first number is exactly 04189 — the
     * number T5's top bar shows before anything is booked.
     */
    private suspend fun seedNumbering(now: Long) {
        val dao = database.numberingDao()
        data class SeriesSeed(val id: String, val branch: String, val docType: String, val prefix: String, val lastIssued: Long)
        listOf(
            SeriesSeed(SeedIds.SERIES_BILTY_INDORE, SeedIds.BRANCH_INDORE, "BILTY", "IND/2627/", 4188),
            SeriesSeed("seed-series-bilty-bhiwandi", SeedIds.BRANCH_BHIWANDI, "BILTY", "BWD/2627/", 0),
            SeriesSeed("seed-series-challan-indore", SeedIds.BRANCH_INDORE, "CHALLAN", "CHL/IND/2627/", 741),
            SeriesSeed("seed-series-fb-indore", SeedIds.BRANCH_INDORE, "FREIGHT_BILL", "FB/IND/2627/", 310),
            SeriesSeed("seed-series-rcpt-indore", SeedIds.BRANCH_INDORE, "RECEIPT", "RCPT/IND/2627/", 128),
            SeriesSeed("seed-series-rcpt-bhiwandi", SeedIds.BRANCH_BHIWANDI, "RECEIPT", "RCPT/BWD/2627/", 0),
        ).forEach { seed ->
            // Counters are sacred (§9): a re-seed must never roll last_issued back over
            // numbers already stamped into bilties — only first-time seeding writes them.
            if (dao.getSeries(SeedIds.COMPANY_SHIVSHAKTI, seed.branch, seed.docType) != null) return@forEach
            dao.upsertSeries(
                NumberSeriesEntity(
                    local_id = seed.id, server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, company_id = SeedIds.COMPANY_SHIVSHAKTI,
                    branch_id = seed.branch, doc_type = seed.docType, prefix = seed.prefix, fy_part = "2627",
                    digits = 5, last_issued = seed.lastIssued, reset_rule = "FINANCIAL_YEARLY",
                ),
            )
        }
        // The initial lease is granted only when the bilty series has none — a re-seed
        // must not rewind next_value under numbers the device has already issued.
        val biltySeries = dao.getSeries(SeedIds.COMPANY_SHIVSHAKTI, SeedIds.BRANCH_INDORE, "BILTY") ?: return
        if (dao.getLeasesForSeries(biltySeries.local_id).isEmpty()) {
            dao.upsertLease(
                NumberLeaseEntity(
                    local_id = "seed-lease-bilty-indore-1", server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null,
                    series_id = biltySeries.local_id, device_id = "seed-device",
                    range_start = 4189, range_end = 4238, next_value = 4189,
                    expires_at = now + 30L * 24 * 60 * 60 * 1000,
                ),
            )
        }
    }

    /**
     * The register fixture (§B6 via the Phase-1 sample): bilties 04183–04188 with the exact
     * statuses the register shows, events derived per §7.1, charge lines that sum to the
     * fixture totals (§3.4 #2), and the 04188 snapshot so T6's dev entry renders the real
     * paper. Booking starts at 04189 — the S5 demo books 04189→04191 on top of these.
     */
    private suspend fun seedConsignments(now: Long) {
        val dao = database.consignmentDao()
        val masters = database.mastersDao()

        val indoreNashik = SeedIds.ROUTE_INDORE_NASHIK
        val routeIds = mapOf(
            "seed-route-indore-nashik" to Pair("Indore", "Nashik"),
            "seed-route-indore-bhusawal" to Pair("Indore", "Bhusawal"),
            "seed-route-indore-dhule" to Pair("Indore", "Dhule"),
            "seed-route-nagpur-nashik" to Pair("Nagpur", "Nashik"),
            "seed-route-indore-kalyan" to Pair("Indore", "Kalyan"),
        )
        val stationIds = buildMap {
            for (i in 0 until 18) {
                val name = when (i) {
                    0 -> "Indore"; 1 -> "Nashik"; 2 -> "Dhule"; 3 -> "Bhusawal"; 4 -> "Nagpur"
                    5 -> "Bhiwandi"; 6 -> "Pune"; 7 -> "Ahmedabad"; 8 -> "Jaipur"; 9 -> "Kalyan"
                    10 -> "Dewas"; 11 -> "Ujjain"; 12 -> "Ratlam"; 13 -> "Bhopal"; 14 -> "Mumbai"
                    15 -> "Surat"; 16 -> "Vadodara"; 17 -> "Delhi"
                    else -> error("unreachable")
                }
                put(name, "seed-station-$i")
            }
        }
        val hour = 60L * 60 * 1000
        val deepak = SeedIds.PARTY_DEEPAK_STEEL

        data class Row(
            val number: Long, val consigneeId: String, val consigneeName: String,
            val routeId: String, val from: String, val to: String,
            val branchId: String, val status: String, val payment: String,
            val packages: Long, val weightKg: Long, val totalPaise: Long,
            val bookedAt: Long, val goods: String, val goodsId: String?,
            val deliveryType: String, val ratePaise: Long?,
            val eventChain: List<Pair<String, Pair<String?, String?>>>, // type to (reason, remark)
        )
        fun chain(vararg types: String) = types.map { it to (null as String? to null as String?) }
        val holdEvent = "HELD" to ("SHORTAGE" to "Shortage - 1 bundle short, weighed at Bhusawal hub")
        val returnEvent = "RETURNED" to ("OTHER" to "RTO - consignee refused the load at destination")

        val rows = listOf(
            Row(
                4188, SeedIds.PARTY_NASHIK_HARDWARE, "Nashik Hardware Mart", indoreNashik, "Indore", "Nashik",
                SeedIds.BRANCH_INDORE, "IN_TRANSIT", "TOPAY", 12, 780, 394_400, now - 2 * hour,
                "MS PIPES", SeedIds.GOODS_MS_PIPES, "DOOR", 450,
                chain("BOOKED", "LOADED", "IN_TRANSIT"),
            ),
            Row(
                4187, SeedIds.PARTY_BHUSAWAL_CEMENT, "Bhusawal Cement Agency", "seed-route-indore-bhusawal", "Indore", "Bhusawal",
                SeedIds.BRANCH_INDORE, "AT_HUB", "TBB", 40, 4200, 1_218_000, now - 26 * hour,
                "CEMENT", null, "DOOR", 265,
                chain("BOOKED", "LOADED", "IN_TRANSIT", "AT_HUB"),
            ),
            Row(
                4186, SeedIds.PARTY_SAI_ELECTRICALS, "Sai Electricals", "seed-route-indore-dhule", "Indore", "Dhule",
                SeedIds.BRANCH_INDORE, "DELIVERED", "PAID", 8, 310, 675_000, now - 28 * hour,
                "ELECTRICAL GOODS", null, "GODOWN", 2053,
                chain("BOOKED", "LOADED", "IN_TRANSIT", "ARRIVED", "DELIVERED"),
            ),
            Row(
                4185, SeedIds.PARTY_NASHIK_HARDWARE, "Nashik Hardware Mart", "seed-route-indore-dhule", "Indore", "Dhule",
                SeedIds.BRANCH_INDORE, "HELD", "TOPAY", 6, 180, 241_000, now - 30 * hour,
                "HARDWARE", null, "GODOWN", 1248,
                chain("BOOKED", "LOADED", "IN_TRANSIT") + listOf(holdEvent),
            ),
            Row(
                4184, SeedIds.PARTY_VIDARBHA_TRADERS, "Vidarbha Traders", "seed-route-nagpur-nashik", "Nagpur", "Nashik",
                SeedIds.BRANCH_NAGPUR, "OUT_FOR_DELIVERY", "TBB", 55, 6100, 1_892_000, now - 50 * hour,
                "AUTO PARTS", null, "DOOR", 285,
                chain("BOOKED", "LOADED", "IN_TRANSIT", "ARRIVED", "OUT_FOR_DELIVERY"),
            ),
            Row(
                4183, "seed-party-${"Kalyan Steel Mart".hashCode()}", "Kalyan Steel Mart", "seed-route-indore-kalyan", "Indore", "Kalyan",
                SeedIds.BRANCH_INDORE, "RETURNED", "TOPAY", 9, 420, 406_000, now - 52 * hour,
                "STEEL BARS", null, "GODOWN", 903,
                chain("BOOKED", "LOADED", "IN_TRANSIT", "ARRIVED") + listOf(returnEvent),
            ),
        )

        rows.forEach { row ->
            val bookedAt = row.bookedAt
            val routePair = routeIds.getValue(row.routeId)
            val (freightPaise, gstPaise, roundingPaise, taxablePaise, hamaliPaise, doorPaise) = if (row.number == 4188L) {
                // The §10.6 canonical lines, exactly.
                Charges(351_000, 18_780, 20, 375_600, 9_600, 15_000)
            } else {
                solveCharges(row.totalPaise, row.packages * 800, if (row.deliveryType == "DOOR") 15_000 else 0)
            }
            val consignmentId = "seed-consignment-${row.number}"
            val consignorName = "Deepak Steel Traders"
            dao.upsertConsignment(
                ConsignmentEntity(
                    local_id = consignmentId, server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, company_id = SeedIds.COMPANY_SHIVSHAKTI,
                    series_id = SeedIds.SERIES_BILTY_INDORE, bilty_no = "IND/2627/%05d".format(row.number),
                    provisional_no = null, status_projection = row.status,
                    booking_branch_id = row.branchId, dest_branch_id = null,
                    consignor_id = deepak, consignee_id = row.consigneeId,
                    route_id = row.routeId,
                    from_station_id = stationIds.getValue(routePair.first), to_station_id = stationIds.getValue(routePair.second),
                    payment_mode = row.payment, risk = "OWNERS", delivery_type = row.deliveryType,
                    place_of_supply_state = "Maharashtra", eway_bill_no = if (row.number == 4188L) "281047556392" else null,
                    private_mark = if (row.number == 4188L) "DST-114" else null,
                    packages = row.packages, actual_weight_g = row.weightKg * 1000, chargeable_weight_g = row.weightKg * 1000,
                    declared_value_paise = 0,
                    freight_paise = freightPaise, gst_paise = gstPaise, total_paise = row.totalPaise,
                    booked_at = bookedAt, booked_by_name = "Mahesh Patidar",
                    expected_arrival = bookedAt + 2L * 24 * 60 * 60 * 1000,
                    party_names = "$consignorName; ${row.consigneeName}",
                    freight_bill_id = null, amends_id = null,
                    amendment_reason = null,
                ),
            )
            dao.upsertItem(
                ConsignmentItemEntity(
                    local_id = "seed-item-${row.number}", server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, consignment_id = consignmentId,
                    goods_id = row.goodsId, description = row.goods, packages = row.packages,
                    actual_weight_g = row.weightKg * 1000, chargeable_weight_g = row.weightKg * 1000,
                    rate_paise = row.ratePaise, basis = "PER_KG", freight_paise = freightPaise,
                ),
            )
            buildList {
                add(ChargeLineEntity(
                    local_id = "seed-cl-${row.number}-freight", server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, consignment_id = consignmentId,
                    head_code = "freight", label = "Freight", basis = "PER_KG",
                    input_value = "${row.weightKg} kg", computed_paise = freightPaise, taxable = true, sort_order = 0,
                ))
                if (hamaliPaise > 0) add(ChargeLineEntity(
                    local_id = "seed-cl-${row.number}-hamali", server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, consignment_id = consignmentId,
                    head_code = "hamali", label = "Hamali", basis = "PER_PACKAGE",
                    input_value = "${row.packages} × 8.00", computed_paise = hamaliPaise, taxable = true, sort_order = 1,
                ))
                if (doorPaise > 0) add(ChargeLineEntity(
                    local_id = "seed-cl-${row.number}-door", server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, consignment_id = consignmentId,
                    head_code = "door_delivery", label = "Door delivery", basis = "FLAT",
                    input_value = "fixed", computed_paise = doorPaise, taxable = true, sort_order = 2,
                ))
                add(ChargeLineEntity(
                    local_id = "seed-cl-${row.number}-gst", server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, consignment_id = consignmentId,
                    head_code = "gst", label = "GST 5% — we pay, forward charge", basis = "PERCENT",
                    input_value = "5% of freight", computed_paise = gstPaise, taxable = false, sort_order = 3,
                ))
                if (roundingPaise != 0L) add(ChargeLineEntity(
                    local_id = "seed-cl-${row.number}-rounding", server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, consignment_id = consignmentId,
                    head_code = "rounding", label = "Rounding", basis = "ROUNDING",
                    input_value = null, computed_paise = roundingPaise, taxable = false, sort_order = 4,
                ))
            }.forEach { dao.upsertChargeLine(it) }

            row.eventChain.forEachIndexed { index, (type, reasonRemark) ->
                val (reason, remark) = reasonRemark
                dao.insertStatusEvent(
                    StatusEventEntity(
                        local_id = "seed-ev-${row.number}-$index", server_id = null, updated_at_local = now, updated_at_server = null,
                        sync_state = SyncState.SYNCED, deleted_at = null, company_id = SeedIds.COMPANY_SHIVSHAKTI,
                        consignment_id = consignmentId, client_event_id = "seed-event-${row.number}-$index",
                        event_type = type, occurred_at = bookedAt + index * 3 * hour, recorded_at = bookedAt + index * 3 * hour,
                        actor_member_id = null, actor_name = "Mahesh Patidar", branch_id = row.branchId,
                        location = null, photo_ref = null, reason_code = reason, remark = remark, challan_ref = null,
                    ),
                )
            }

            if (row.number == 4188L) {
                dao.upsertSnapshot(seedBiltySnapshot(now, consignmentId, bookedAt))
            }
        }
    }

    /**
     * The money fixtures (S9, §12). Twenty-nine TBB consignments booked at Nagpur across the
     * last two months — all consignor Deepak Steel Traders — of which four sit on issued bill
     * FB/IND/2627/00298 and four on FB/IND/2627/00311. Together with the two original seeded
     * TBB bilties (04187, 04184) that leaves exactly the design fixture's twenty-three in the
     * unbilled pool. Receipts 00126–00128 (the seeded RCPT series runs to 128) are recorded
     * unallocated, so T16 also demonstrates the on-account credit. Fixture amounts are
     * re-derived from the seeded consignments rather than pasted from the prototype (D42)
     * so every screen reconciles against real rows.
     */
    private suspend fun seedMoney(now: Long) {
        val dao = database.consignmentDao()
        val billing = database.billingDao()
        val day = 24L * 60 * 60 * 1000

        val company = SeedIds.COMPANY_SHIVSHAKTI
        val branch = SeedIds.BRANCH_NAGPUR
        val deepak = SeedIds.PARTY_DEEPAK_STEEL

        // Nagpur → Nashik is the one named route leaving Nagpur; the rest reuse it.
        val nagpurNashik = "seed-route-nagpur-nashik"
        val stationIds = mapOf("Nagpur" to "seed-station-4", "Nashik" to "seed-station-1")
        val route = database.mastersDao().getRoute(nagpurNashik)!!

        val consigneeIds = listOf(
            SeedIds.PARTY_SAI_ELECTRICALS to "Sai Electricals",
            SeedIds.PARTY_VIDARBHA_TRADERS to "Vidarbha Traders",
            SeedIds.PARTY_BHUSAWAL_CEMENT to "Bhusawal Cement Agency",
        )

        data class MoneyCn(val i: Int, val ageDays: Long, val freightPaise: Long, val gstPaise: Long, val totalPaise: Long, val packages: Long)
        fun moneyCn(i: Int): MoneyCn {
            val freight = 100_000L + (i % 5) * 20_000L
            val gst = freight / 20 // exactly 5%
            return MoneyCn(i, 3L + 2L * i, freight, gst, freight + gst, 6L + (i % 4))
        }
        // Bills take the four oldest slots each: 00298 ← i 25..28, 00311 ← i 21..24.
        val onBill298 = (25..28).toSet()
        val onBill311 = (21..24).toSet()

        val bookedAtOf = { i: Int -> now - moneyCn(i).ageDays * day }

        for (i in 0 until 29) {
            val cn = moneyCn(i)
            val consignmentId = "seed-money-cn-$i"
            val (consigneeId, consigneeName) = consigneeIds[i % consigneeIds.size]
            val bookedAt = bookedAtOf(i)
            dao.upsertConsignment(
                ConsignmentEntity(
                    local_id = consignmentId, server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, company_id = company,
                    series_id = SeedIds.SERIES_BILTY_INDORE, bilty_no = "IND/2627/%05d".format(4001 + i),
                    provisional_no = null, status_projection = "BOOKED",
                    booking_branch_id = branch, dest_branch_id = null,
                    consignor_id = deepak, consignee_id = consigneeId,
                    route_id = nagpurNashik,
                    from_station_id = route.origin_station_id, to_station_id = route.dest_station_id,
                    payment_mode = "TBB", risk = "OWNERS", delivery_type = "GODOWN",
                    place_of_supply_state = "Maharashtra", eway_bill_no = null, private_mark = null,
                    packages = cn.packages, actual_weight_g = cn.packages * 1000, chargeable_weight_g = cn.packages * 1000,
                    declared_value_paise = 0,
                    freight_paise = cn.freightPaise, gst_paise = cn.gstPaise, total_paise = cn.totalPaise,
                    booked_at = bookedAt, booked_by_name = "Mahesh Patidar",
                    expected_arrival = bookedAt + 2 * day,
                    party_names = "Deepak Steel Traders; $consigneeName",
                    freight_bill_id = null, amends_id = null,
                    amendment_reason = null,
                ),
            )
            dao.upsertItem(
                ConsignmentItemEntity(
                    local_id = "seed-money-item-$i", server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, consignment_id = consignmentId,
                    goods_id = null, description = "MS PIPES", packages = cn.packages,
                    actual_weight_g = cn.packages * 1000, chargeable_weight_g = cn.packages * 1000,
                    rate_paise = null, basis = "PER_KG", freight_paise = cn.freightPaise,
                ),
            )
            dao.upsertChargeLine(
                ChargeLineEntity(
                    local_id = "seed-money-cl-$i-freight", server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, consignment_id = consignmentId,
                    head_code = "freight", label = "Freight", basis = "PER_KG",
                    input_value = "${cn.packages} kg", computed_paise = cn.freightPaise, taxable = true, sort_order = 0,
                ),
            )
            dao.upsertChargeLine(
                ChargeLineEntity(
                    local_id = "seed-money-cl-$i-gst", server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, consignment_id = consignmentId,
                    head_code = "gst", label = "GST 5% — we pay, forward charge", basis = "PERCENT",
                    input_value = "5% of freight", computed_paise = cn.gstPaise, taxable = false, sort_order = 1,
                ),
            )
            dao.insertStatusEvent(
                StatusEventEntity(
                    local_id = "seed-money-ev-$i", server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, company_id = company,
                    consignment_id = consignmentId, client_event_id = "seed-money-event-$i",
                    event_type = "BOOKED", occurred_at = bookedAt, recorded_at = bookedAt,
                    actor_member_id = null, actor_name = "Mahesh Patidar", branch_id = branch,
                    location = null, photo_ref = null, reason_code = null, remark = null, challan_ref = null,
                ),
            )
        }

        suspend fun seedBill(localId: String, billNo: String, indices: List<Int>, issuedDaysAgo: Long) {
            val cons = indices.map { moneyCn(it) }
            val issuedAt = now - issuedDaysAgo * day
            billing.upsertBill(
                FreightBillEntity(
                    local_id = localId, server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, company_id = company,
                    series_id = "seed-series-fb-indore", bill_no = billNo, state = "ISSUED",
                    party_id = deepak,
                    period_start = indices.minOf { bookedAtOf(it) }, period_end = indices.maxOf { bookedAtOf(it) },
                    due_at = issuedAt + 30 * day,
                    freight_paise = cons.sumOf { it.freightPaise },
                    other_charges_paise = 0,
                    taxable_paise = cons.sumOf { it.freightPaise },
                    gst_paise = cons.sumOf { it.gstPaise },
                    total_paise = cons.sumOf { it.totalPaise },
                    gst_treatment = "INTERSTATE",
                    notes = null, issued_at = issuedAt, issued_by_name = "Mahesh Patidar", cancelled_at = null,
                ),
            )
            indices.forEach { i ->
                dao.upsertConsignment(
                    dao.getConsignment("seed-money-cn-$i")!!.copy(freight_bill_id = localId, updated_at_local = now),
                )
            }
        }
        seedBill("seed-bill-00298", "FB/IND/2627/00298", listOf(25, 26, 27, 28), issuedDaysAgo = 44)
        seedBill("seed-bill-00311", "FB/IND/2627/00311", listOf(21, 22, 23, 24), issuedDaysAgo = 35)

        // Receipts 00126–00128: the last three of the seeded RCPT series, recorded unallocated
        // so the statement demonstrates the on-account credit (§12.2).
        data class ReceiptSeed(val no: String, val localId: String, val payer: String, val paise: Long, val instrument: String, val ref: String?, val daysAgo: Long)
        listOf(
            ReceiptSeed("RCPT/IND/2627/00126", "seed-receipt-00126", SeedIds.PARTY_NASHIK_HARDWARE, 1_800_000, "CASH", null, 36),
            ReceiptSeed("RCPT/IND/2627/00127", "seed-receipt-00127", "seed-party-${"Agarwal Logistics Pvt Ltd".hashCode()}", 3_100_000, "NEFT", "SBIN0099887766", 28),
            ReceiptSeed("RCPT/IND/2627/00128", "seed-receipt-00128", deepak, 5_000_000, "NEFT", "SBIN0026412188", 10),
        ).forEach { seed ->
            billing.upsertReceipt(
                ReceiptEntity(
                    local_id = seed.localId, server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, company_id = company,
                    series_id = "seed-series-rcpt-indore", receipt_no = seed.no,
                    party_id = seed.payer, amount_paise = seed.paise,
                    instrument = seed.instrument, instrument_ref = seed.ref,
                    received_at = now - seed.daysAgo * day,
                    received_at_branch_id = SeedIds.BRANCH_INDORE,
                    received_by_name = "Mahesh Patidar", notes = null,
                ),
            )
        }
    }

    /** The §10.6 fixture as the 04188 snapshot payload — key-compatible with the S5 decoder. */
    private fun seedBiltySnapshot(now: Long, consignmentId: String, bookedAt: Long): DocSnapshotEntity {
        val payload = JSONObject().apply {
            put("companyName", "SHIVSHAKTI ROADLINES")
            put("addressLine", "Plot 14, Transport Nagar, Indore 452003")
            put("contactLine", "Ph 94250 61183 · GSTIN 23AABCS4521M1Z9")
            put("consignorName", "Deepak Steel Traders")
            put("consignorContact", "Indore · +91 94250 61183")
            put("consignorGstin", "GSTIN 23AACDS8812K1Z4")
            put("consignorAddress", "Plot 14, Transport Nagar, Indore, 452003")
            put("consigneeName", "Nashik Hardware Mart")
            put("consigneeContact", "Nashik · +91 98600 27419")
            put("consigneeGstin", "GSTIN 27AAFCN3390L1Z8")
            put("consigneeAddress", "MIDC Ambad, Nashik")
            put("docNo", "IND/2627/04188")
            put("date", java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.ENGLISH).format(java.util.Date(bookedAt)))
            put("fromStation", "INDORE")
            put("toStation", "NASHIK")
            put("packages", "12")
            put("goodsDescription", "MS PIPES")
            put("actualWeight", "780 kg")
            put("chargeableWeight", "780 kg")
            put("rate", "4.50")
            put("freight", "3,510.00")
            put("hamali", "96.00")
            put("doorDelivery", "150.00")
            put("taxable", "3,756.00")
            put("gst", "187.80")
            put("rounding", "0.20")
            put("gstLabel", "GST 5% — we pay, forward charge")
            put("totalLabel", "Grand Total")
            put("grandTotal", "3,944.00")
            put("amountInWords", "Rupees three thousand nine hundred forty four only")
            put("stamp", "TO PAY")
            put("footer", "At owner's risk · Door delivery · Private mark DST-114 · E-way bill 281047556392")
            put("provisionalCrossRef", JSONObject.NULL)
        }
        val payloadJson = payload.toString()
        return DocSnapshotEntity(
            local_id = "seed-snapshot-4188", server_id = null, updated_at_local = now, updated_at_server = null,
            sync_state = SyncState.SYNCED, deleted_at = null, consignment_id = consignmentId,
            document_type = "BILTY", template_id = "tpl-bilty-default", template_version = "1", version = 1,
            payload_json = payloadJson,
            content_hash = fnv1a(payloadJson).toString(16),
            copy_count = 4,
        )
    }

    /**
     * The default BILTY template (Phase 3 S11, §9.15) as one row of TEMPLATE_E. Its section
     * field keys are exactly the keys the DOC_SNAPSHOT payloads already carry (docNo, date,
     * consignorName, …, grandTotal, amountInWords, footer), so the seeded 04188 snapshot
     * renders through the S12 renderer with zero data changes. Versions are rows: this row
     * is version 1 and active; the counter-guard pattern applies — a re-seed never
     * duplicates a version the unique index already holds.
     */
    private suspend fun seedTemplates(now: Long) {
        val dao = database.templateDao()
        val company = SeedIds.COMPANY_SHIVSHAKTI

        // Keep the JSON block stable and readable; the engine (S12) reads strictly by key.
        val contentJson = """
        {
          "schemaVersion": 1,
          "id": "tpl-bilty-default",
          "name": "Default Bilty",
          "version": 1,
          "paper": { "size": "A4", "marginMm": 10, "orientation": "portrait" },
          "theme": { "primaryColor": "#0E4D38", "textOnPrimary": "#FFFFFF", "fontFamily": "sans" },
          "business": {
            "shopName": "SHIVSHAKTI ROADLINES",
            "address": "Plot 14, Transport Nagar, Indore 452003",
            "mobile": "94250 61183",
            "taxId": "23AABCS4521M1Z9"
          },
          "sections": [
            { "type": "header" },
            { "type": "title", "title": "CONSIGNMENT NOTE" },
            { "type": "meta", "fields": [
              { "key": "docNo", "label": "GR No", "required": true },
              { "key": "date", "label": "Date" },
              { "key": "fromStation", "label": "From" },
              { "key": "toStation", "label": "To" },
              { "key": "stamp", "label": "Payment" }
            ] },
            { "type": "customer", "fields": [
              { "key": "consignorName", "label": "Consignor", "required": true },
              { "key": "consignorAddress", "label": "Address" },
              { "key": "consigneeName", "label": "Consignee", "required": true },
              { "key": "consigneeAddress", "label": "Address" }
            ] },
            { "type": "items", "minRows": 6, "columns": [
              { "key": "goodsDescription", "label": "Goods", "widthMm": 40 },
              { "key": "packages", "label": "Pkgs", "widthMm": 14 },
              { "key": "actualWeight", "label": "Weight", "widthMm": 20 },
              { "key": "rate", "label": "Rate", "widthMm": 18 },
              { "key": "freight", "label": "Freight", "widthMm": 24 }
            ] },
            { "type": "totals", "fields": [
              { "key": "hamali", "label": "Hamali" },
              { "key": "doorDelivery", "label": "Door delivery" },
              { "key": "taxable", "label": "Taxable" },
              { "key": "gst", "label": "GST 5%" },
              { "key": "rounding", "label": "Rounding" },
              { "key": "grandTotal", "label": "Grand Total" }
            ] },
            { "type": "footer", "fields": [
              { "key": "amountInWords", "label": "Amount in words" },
              { "key": "footer", "label": "Terms" }
            ] }
          ]
        }
        """.trimIndent()

        val entity = TemplateEntity(
            local_id = "seed-template-bilty-v1", server_id = null, updated_at_local = now, updated_at_server = null,
            sync_state = SyncState.SYNCED, deleted_at = null, company_id = company,
            template_key = "tpl-bilty-default", version = 1, is_active = true,
            schema_version = 1, content_json = contentJson,
            content_hash = fnv1a(contentJson).toString(16),
            visibility = "BUILT-IN", created_by_name = "Engine",
        )
        // Versions are rows and (company, key, version) is unique: never write over a
        // version the device already holds — a re-seed must not churn template identity.
        if (dao.getTemplateVersion(company, entity.template_key, entity.version) == null) {
            dao.upsertTemplate(entity)
        }
    }

    /**
     * The dated company calculation setting (Phase 3 S14, §10.5): GST at the §10.5 demo's
     * 5%, and the volumetric divisor LIVE at the §10.1 standard 6000 — the engine's
     * volumetric branch was implemented and tested in S4 but unreachable while the
     * hard-coded setting disabled it (the audit's D1). Effective from 90 days before the
     * seed moment so a second, newer row can be inserted in the demo to show the dated
     * freeze (already-booked bilties keep their figures). Insert-once per company.
     */
    private suspend fun seedSettings(now: Long) {
        val dao = database.settingsDao()
        val company = SeedIds.COMPANY_SHIVSHAKTI
        if (dao.countSettings(company) > 0) return
        dao.upsertSetting(
            CompanySettingEntity(
                local_id = "seed-setting-v1", server_id = null, updated_at_local = now, updated_at_server = null,
                sync_state = SyncState.SYNCED, deleted_at = null, company_id = company,
                effective_from = now - 90L * 24 * 60 * 60 * 1000,
                gst_rate_bp = 500, weight_step_g = 1000, volumetric_divisor_g = 6000,
                gst_treatment = "FORWARD", rounding = "NEAREST_RUPEE", created_by_name = "Engine",
            ),
        )
    }

    private fun fnv1a(text: String): Long {
        var hash = -0x340d631b7bdddcdbL
        text.forEach { ch ->
            hash = hash xor ch.code.toLong()
            hash *= 0x100000001b3L
        }
        return hash
    }

    /** Consignment charge sums as one immutable struct. */
    private data class Charges(
        val freightPaise: Long, val gstPaise: Long, val roundingPaise: Long,
        val taxablePaise: Long, val hamaliPaise: Long, val doorPaise: Long,
    )

    /**
     * Finds the freight whose taxable value prices out to the fixture total with a
     * nearest-rupee rounding within ±50 paise — the register amounts print exactly
     * (§3.4 #2) without inventing rates. Deterministic search around total/1.05.
     */
    private fun solveCharges(totalPaise: Long, hamaliPaise: Long, doorPaise: Long): Charges {
        fun gst(taxable: Long) = (taxable * 500 + 5_000) / 10_000
        val approx = totalPaise * 20 / 21
        (-150..150).map { approx + it }.forEach { taxable ->
            if (taxable < hamaliPaise + doorPaise) return@forEach
            val rounding = totalPaise - (taxable + gst(taxable))
            if (rounding in -50..50) {
                return Charges(
                    freightPaise = taxable - hamaliPaise - doorPaise,
                    gstPaise = gst(taxable),
                    roundingPaise = rounding,
                    taxablePaise = taxable,
                    hamaliPaise = hamaliPaise,
                    doorPaise = doorPaise,
                )
            }
        }
        error("no charge split sums to $totalPaise within a rupee of rounding")
    }

    companion object {
        const val EMAIL_DEMO_USER = "mahesh.patidar@gmail.com"
        private const val FIVE_DAYS_MS = 5L * 24 * 60 * 60 * 1000

        const val ID_SHIVSHAKTI = "seed-company-shivshakti"
        const val ID_BHARAT_CARGO = "seed-company-bharat-cargo"
        const val ID_MALWA = "seed-company-malwa"
        const val ID_BRANCH_INDORE = "seed-branch-indore"
        const val ID_BRANCH_NAGPUR = "seed-branch-nagpur"
        const val ID_BRANCH_BHIWANDI = "seed-branch-bhiwandi"
        const val ID_BRANCH_BC_NAGPUR = "seed-branch-bc-nagpur"
        const val ID_MEMBERSHIP_SHIVSHAKTI_OWNER = "seed-membership-shivshakti-owner"
        const val ID_MEMBERSHIP_SHIVSHAKTI_SUNITA = "seed-membership-shivshakti-sunita"
        const val ID_MEMBERSHIP_SHIVSHAKTI_RAMESH = "seed-membership-shivshakti-ramesh"
        const val ID_MEMBERSHIP_SHIVSHAKTI_IQBAL = "seed-membership-shivshakti-iqbal"
        const val ID_MEMBERSHIP_BHARAT_CLERK = "seed-membership-bharat-clerk"
        const val ID_MEMBERSHIP_MALWA_INVITE = "seed-membership-malwa-invite"
    }
}
