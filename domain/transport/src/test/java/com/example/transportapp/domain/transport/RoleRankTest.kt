package com.example.transportapp.domain.transport

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** §13's role-gating matrix: Owner sees all; a Booking Clerk hides Accountant and above. */
class RoleRankTest {

    @Test
    fun `owner sees every tile`() {
        listOf("BOOKING_CLERK", "DELIVERY_CLERK", "ACCOUNTANT", "MANAGER", "OWNER").forEach {
            assertTrue(RoleRank.atLeast("OWNER", it))
        }
    }

    @Test
    fun `a booking clerk hides every gated tile`() {
        assertTrue(RoleRank.atLeast("BOOKING_CLERK", "BOOKING_CLERK"))
        assertFalse(RoleRank.atLeast("BOOKING_CLERK", "DELIVERY_CLERK"))
        assertFalse(RoleRank.atLeast("BOOKING_CLERK", "ACCOUNTANT"))
        assertFalse(RoleRank.atLeast("BOOKING_CLERK", "MANAGER"))
        assertFalse(RoleRank.atLeast("BOOKING_CLERK", "OWNER"))
    }

    @Test
    fun `the accountant sees delivery and own tiles but not manager ones`() {
        assertTrue(RoleRank.atLeast("ACCOUNTANT", "DELIVERY_CLERK"))
        assertTrue(RoleRank.atLeast("ACCOUNTANT", "ACCOUNTANT"))
        assertFalse(RoleRank.atLeast("ACCOUNTANT", "MANAGER"))
    }

    @Test
    fun `an unknown role passes only the lowest gate`() {
        assertTrue(RoleRank.atLeast("GHOST", "BOOKING_CLERK"))
        assertFalse(RoleRank.atLeast("GHOST", "DELIVERY_CLERK"))
        assertFalse(RoleRank.atLeast("GHOST", "MANAGER"))
    }
}
