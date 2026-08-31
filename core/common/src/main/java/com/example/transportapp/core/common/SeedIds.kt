package com.example.transportapp.core.common

/**
 * Stable local ids of the seeded demo dataset (§B6). Shared so the DataStore's mocked
 * session can point at the seeded org and the seeder writes the same rows — one source
 * of truth for ids that cross module boundaries. Pure Kotlin.
 */
object SeedIds {
    const val COMPANY_SHIVSHAKTI = "seed-company-shivshakti"
    const val COMPANY_BHARAT_CARGO = "seed-company-bharat-cargo"
    const val COMPANY_MALWA = "seed-company-malwa"

    const val BRANCH_INDORE = "seed-branch-indore"
    const val BRANCH_NAGPUR = "seed-branch-nagpur"
    const val BRANCH_BHIWANDI = "seed-branch-bhiwandi"
    const val BRANCH_BC_NAGPUR = "seed-branch-bc-nagpur"

    const val PARTY_DEEPAK_STEEL = "seed-party-deepak-steel"
    const val PARTY_DEEPAK_DUPLICATE = "seed-party-deepak-duplicate"
    const val PARTY_NASHIK_HARDWARE = "seed-party-nashik-hardware"
    const val ROUTE_INDORE_NASHIK = "seed-route-indore-nashik"
    const val GOODS_MS_PIPES = "seed-goods-ms-pipes"
    const val VEHICLE_MH15BK4412 = "seed-vehicle-mh15bk4412"
    const val DRIVER_GURMEET = "seed-driver-gurmeet"

    /** Register-fixture parties pinned onto filler slots (consignments 04183–04188, S5). */
    const val PARTY_SAI_ELECTRICALS = "seed-party-gen-11"
    const val PARTY_VIDARBHA_TRADERS = "seed-party-gen-12"
    const val PARTY_BHUSAWAL_CEMENT = "seed-party-gen-13"

    /** The register fixture's bilties; 04188 carries the seeded T6 snapshot. */
    const val BILTY_04188 = "IND/2627/04188"
    const val SERIES_BILTY_INDORE = "seed-series-bilty-indore"

    /** The seeded issued freight bills (S9) — T14's issued view and T16's ledger rows. */
    const val BILL_00298 = "seed-bill-00298"
    const val BILL_00311 = "seed-bill-00311"
}
