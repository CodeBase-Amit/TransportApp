package com.example.transportapp.domain.transport.trip

import com.example.transportapp.domain.transport.TripState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** §11.1 — the trip lifecycle, kept apart from the consignment state machine. */
class TripStateMachineTest {

    @Test
    fun `the working path open to closed is exactly four moves`() {
        assertTrue(TripStateMachine.canTransition(TripState.OPEN, TripState.ISSUED))
        assertTrue(TripStateMachine.canTransition(TripState.ISSUED, TripState.DISPATCHED))
        assertTrue(TripStateMachine.canTransition(TripState.DISPATCHED, TripState.CLOSED))
        assertFalse("closed is terminal", TripStateMachine.canTransition(TripState.CLOSED, TripState.ISSUED))
    }

    @Test
    fun `cancel is only possible before dispatch`() {
        assertTrue(TripStateMachine.canTransition(TripState.OPEN, TripState.CANCELLED))
        assertTrue(TripStateMachine.canTransition(TripState.ISSUED, TripState.CANCELLED))
        assertFalse("a dispatched vehicle cannot be cancelled, only closed", TripStateMachine.canTransition(TripState.DISPATCHED, TripState.CANCELLED))
        assertFalse(TripStateMachine.canTransition(TripState.CLOSED, TripState.CANCELLED))
    }

    @Test
    fun `illegal jumps are refused`() {
        assertFalse("cannot dispatch a trip that has no challan number", TripStateMachine.canTransition(TripState.OPEN, TripState.DISPATCHED))
        assertFalse("cannot close a trip still at the origin", TripStateMachine.canTransition(TripState.OPEN, TripState.CLOSED))
        assertFalse("cannot issue twice", TripStateMachine.canTransition(TripState.ISSUED, TripState.ISSUED))
        assertFalse(TripStateMachine.canTransition(TripState.CANCELLED, TripState.ISSUED))
    }

    @Test
    fun `open family is issued or dispatched only`() {
        assertTrue(TripStateMachine.isOpen(TripState.ISSUED))
        assertTrue(TripStateMachine.isOpen(TripState.DISPATCHED))
        assertFalse("a being-built trip holds no vehicle yet", TripStateMachine.isOpen(TripState.OPEN))
        assertFalse(TripStateMachine.isOpen(TripState.CLOSED))
        assertFalse(TripStateMachine.isOpen(TripState.CANCELLED))
    }

    @Test
    fun `allowed mirrors the table`() {
        assertEquals(setOf(TripState.ISSUED, TripState.CANCELLED), TripStateMachine.allowed(TripState.OPEN))
        assertEquals(emptySet<TripState>(), TripStateMachine.allowed(TripState.CLOSED))
    }
}
