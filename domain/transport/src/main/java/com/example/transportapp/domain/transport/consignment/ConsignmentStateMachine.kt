package com.example.transportapp.domain.transport.consignment

import com.example.transportapp.domain.transport.ConsignmentStatus

/**
 * §7.1 — the consignment state machine as data. Status is derived from the event log; this
 * table is what repositories consult before appending an event, so an illegal transition can
 * never enter the log and the projection can never disagree with it.
 *
 * Rules beyond the table (enforced by callers):
 * - only `Draft` content is mutable; from `Booked` on, corrections are amendments;
 * - `Delivered` needs a POD record or a Manager waiver; a To Pay consignment additionally
 *   needs the collection recorded or waived;
 * - exceptions (Held) require a remark of at least ten characters;
 * - `Cancelled` retains its bilty number forever.
 */
object ConsignmentStateMachine {

    private val transitions: Map<ConsignmentStatus, Set<ConsignmentStatus>> = mapOf(
        ConsignmentStatus.DRAFT to setOf(ConsignmentStatus.BOOKED),
        ConsignmentStatus.BOOKED to setOf(ConsignmentStatus.LOADED, ConsignmentStatus.CANCELLED),
        ConsignmentStatus.LOADED to setOf(ConsignmentStatus.IN_TRANSIT, ConsignmentStatus.HELD),
        ConsignmentStatus.IN_TRANSIT to setOf(ConsignmentStatus.AT_HUB, ConsignmentStatus.ARRIVED, ConsignmentStatus.HELD),
        ConsignmentStatus.AT_HUB to setOf(ConsignmentStatus.IN_TRANSIT, ConsignmentStatus.ARRIVED),
        ConsignmentStatus.ARRIVED to setOf(ConsignmentStatus.OUT_FOR_DELIVERY, ConsignmentStatus.DELIVERED, ConsignmentStatus.HELD),
        ConsignmentStatus.OUT_FOR_DELIVERY to setOf(ConsignmentStatus.DELIVERED),
        ConsignmentStatus.HELD to setOf(ConsignmentStatus.IN_TRANSIT, ConsignmentStatus.ARRIVED, ConsignmentStatus.RETURNED),
        // Terminal states take no further transitions; Cancelled/Returned numbers are never reused.
        ConsignmentStatus.DELIVERED to emptySet(),
        ConsignmentStatus.CANCELLED to emptySet(),
        ConsignmentStatus.RETURNED to emptySet(),
    )

    fun canTransition(from: ConsignmentStatus, to: ConsignmentStatus): Boolean =
        transitions[from]?.contains(to) ?: false

    /** The transition table itself, for tests and the case file's "what may happen next". */
    fun allowed(from: ConsignmentStatus): Set<ConsignmentStatus> =
        transitions[from] ?: emptySet()
}
