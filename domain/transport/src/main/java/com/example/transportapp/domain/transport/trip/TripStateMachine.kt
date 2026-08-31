package com.example.transportapp.domain.transport.trip

import com.example.transportapp.domain.transport.TripState

/**
 * §11.1 — the trip lifecycle, deliberately separate from the consignment lifecycle
 * (§7.1). The two states the vehicle board calls "open" are Issued and Dispatched: a
 * vehicle holds at most one open trip, and that single rule is what makes the Available
 * group on the board trustworthy.
 *
 * Guards live with the repository; this table only refuses impossible moves:
 * - Open → Issued stamps the challan number and marks every leg Loaded;
 * - Issued → Dispatched is a separate action because loading and departure are hours apart;
 * - Cancelled (before dispatch only) returns the consignments to the pool they came from;
 * - Closed is terminal — settlement happens at close, read-only afterwards.
 */
object TripStateMachine {

    private val transitions: Map<TripState, Set<TripState>> = mapOf(
        TripState.OPEN to setOf(TripState.ISSUED, TripState.CANCELLED),
        TripState.ISSUED to setOf(TripState.DISPATCHED, TripState.CANCELLED),
        TripState.DISPATCHED to setOf(TripState.CLOSED),
        TripState.CLOSED to emptySet(),
        TripState.CANCELLED to emptySet(),
    )

    fun canTransition(from: TripState, to: TripState): Boolean =
        transitions[from]?.contains(to) ?: false

    fun allowed(from: TripState): Set<TripState> = transitions[from] ?: emptySet()

    /** §11.1: "an open trip" means Issued or Dispatched — the vehicle-busy family. */
    fun isOpen(state: TripState): Boolean =
        state == TripState.ISSUED || state == TripState.DISPATCHED
}
