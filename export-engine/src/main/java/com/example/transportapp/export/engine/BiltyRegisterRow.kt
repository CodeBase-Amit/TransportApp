package com.example.transportapp.export.engine

/**
 * One bilty as the freight register and the CSV pack print it (§14). The row is a plain
 * value: the export engine never sees Room, only what the repository already read.
 */
data class BiltyRegisterRow(
    val biltyNo: String,
    val bookedAt: Long,
    val branch: String,
    val consignor: String,
    val consignee: String,
    val route: String,
    val packages: Long,
    val weightKg: Long,
    val freightPaise: Long,
    val gstPaise: Long,
    val totalPaise: Long,
    /** CANCELLED rows export at the tail of the register, struck through on paper. */
    val cancelled: Boolean = false,
)

/** Column names for BiltyRegisterRow, in the sheet's canonical order. */
val BILTY_REGISTER_COLUMNS = listOf(
    "Bilty no.", "Date", "Branch", "Consignor", "Consignee", "Route", "Packages",
    "Weight (kg)", "Freight", "GST", "Total", "Status",
)
