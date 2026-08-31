package com.example.transportapp.domain.transport.calc

import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.consignment.ConsignmentStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** §7.1 — the transition table, including the illegal moves a naive app allows. */
class ConsignmentStateMachineTest {

    @Test
    fun `the happy path forwards exactly as the diagram`() {
        val path = listOf(
            ConsignmentStatus.DRAFT,
            ConsignmentStatus.BOOKED,
            ConsignmentStatus.LOADED,
            ConsignmentStatus.IN_TRANSIT,
            ConsignmentStatus.AT_HUB,
            ConsignmentStatus.IN_TRANSIT,
            ConsignmentStatus.ARRIVED,
            ConsignmentStatus.OUT_FOR_DELIVERY,
            ConsignmentStatus.DELIVERED,
        )
        path.zipWithNext().forEach { (from, to) ->
            assertTrue("DRAFT→BOOKED…DELIBERED: $from → $to must be legal", ConsignmentStateMachine.canTransition(from, to))
        }
    }

    @Test
    fun `every diagrammed branch is legal`() {
        assertTrue(ConsignmentStateMachine.canTransition(ConsignmentStatus.BOOKED, ConsignmentStatus.CANCELLED))
        assertTrue(ConsignmentStateMachine.canTransition(ConsignmentStatus.LOADED, ConsignmentStatus.HELD))
        assertTrue(ConsignmentStateMachine.canTransition(ConsignmentStatus.IN_TRANSIT, ConsignmentStatus.ARRIVED))
        assertTrue(ConsignmentStateMachine.canTransition(ConsignmentStatus.AT_HUB, ConsignmentStatus.ARRIVED))
        assertTrue(ConsignmentStateMachine.canTransition(ConsignmentStatus.ARRIVED, ConsignmentStatus.DELIVERED))
        assertTrue(ConsignmentStateMachine.canTransition(ConsignmentStatus.ARRIVED, ConsignmentStatus.HELD))
        assertTrue(ConsignmentStateMachine.canTransition(ConsignmentStatus.HELD, ConsignmentStatus.IN_TRANSIT))
        assertTrue(ConsignmentStateMachine.canTransition(ConsignmentStatus.HELD, ConsignmentStatus.ARRIVED))
        assertTrue(ConsignmentStateMachine.canTransition(ConsignmentStatus.HELD, ConsignmentStatus.RETURNED))
    }

    @Test
    fun `illegal jumps are refused`() {
        assertFalse("draft cannot skip to delivered", ConsignmentStateMachine.canTransition(ConsignmentStatus.DRAFT, ConsignmentStatus.DELIVERED))
        assertFalse("loaded cannot go straight to arrived", ConsignmentStateMachine.canTransition(ConsignmentStatus.LOADED, ConsignmentStatus.ARRIVED))
        assertFalse("delivered is terminal", ConsignmentStateMachine.canTransition(ConsignmentStatus.DELIVERED, ConsignmentStatus.BOOKED))
        assertFalse("cancelled is terminal and its number is never reused", ConsignmentStateMachine.canTransition(ConsignmentStatus.CANCELLED, ConsignmentStatus.BOOKED))
        assertFalse("returned is terminal", ConsignmentStateMachine.canTransition(ConsignmentStatus.RETURNED, ConsignmentStatus.IN_TRANSIT))
        assertFalse("cancelled cannot be held", ConsignmentStateMachine.canTransition(ConsignmentStatus.CANCELLED, ConsignmentStatus.HELD))
        assertFalse("booked cannot be delivered", ConsignmentStateMachine.canTransition(ConsignmentStatus.BOOKED, ConsignmentStatus.DELIVERED))
    }

    @Test
    fun `allowed mirrors canTransition`() {
        assertEquals(emptySet<ConsignmentStatus>(), ConsignmentStateMachine.allowed(ConsignmentStatus.DELIVERED))
        assertEquals(
            setOf(ConsignmentStatus.IN_TRANSIT, ConsignmentStatus.ARRIVED, ConsignmentStatus.RETURNED),
            ConsignmentStateMachine.allowed(ConsignmentStatus.HELD),
        )
        assertTrue(ConsignmentStateMachine.allowed(ConsignmentStatus.DRAFT).contains(ConsignmentStatus.BOOKED))
        assertFalse(ConsignmentStateMachine.allowed(ConsignmentStatus.OUT_FOR_DELIVERY).contains(ConsignmentStatus.HELD))
    }
}
