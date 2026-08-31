package com.example.transportapp.domain.transport

/**
 * The §13 tile visibility rule as data. A tile the member cannot follow is not shown at all —
 * the Min-role column is both the drill permission and the visibility rule.
 */
object RoleRank {

    private val ranks = listOf(
        "BOOKING_CLERK",
        "DELIVERY_CLERK",
        "ACCOUNTANT",
        "MANAGER",
        "OWNER",
    )

    /**
     * True when [role]'s rank is at least [minimum]'s. The lowest gate is effectively "any
     * signed-in member"; an unknown role passes only that gate, nothing above it.
     */
    fun atLeast(role: String, minimum: String): Boolean {
        val r = ranks.indexOf(role)
        val m = ranks.indexOf(minimum)
        if (m < 0) return true
        if (m == 0) return true
        if (r < 0) return false
        return r >= m
    }

    fun rankOf(role: String): Int = ranks.indexOf(role)
}
