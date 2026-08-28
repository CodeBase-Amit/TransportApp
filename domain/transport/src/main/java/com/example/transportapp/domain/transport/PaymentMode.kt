package com.example.transportapp.domain.transport

/**
 * The three payment modes that drive all downstream money logic. Sealed to a fixed set —
 * "payment mode" means exactly these three values and nothing else.
 */
enum class PaymentMode(val stampText: String, val label: String) {
    PAID("PAID", "Paid"),
    TOPAY("TO PAY", "To Pay"),
    TBB("TBB", "TBB")
}
