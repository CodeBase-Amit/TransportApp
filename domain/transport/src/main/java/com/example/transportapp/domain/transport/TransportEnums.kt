package com.example.transportapp.domain.transport

/**
 * A consignment's status, matching the state machine in TransportApp.md §7.1. Status is a
 * rebuildable projection over the event log, never a hand-edited column.
 *
 * The wordings are the eleven fixed journey-chip strings from Design.md §A8.
 */
enum class ConsignmentStatus(val wording: String) {
    DRAFT("Draft"),
    BOOKED("Booked"),
    LOADED("Loaded"),
    IN_TRANSIT("In transit"),
    AT_HUB("At hub"),
    ARRIVED("Arrived"),
    OUT_FOR_DELIVERY("Out for delivery"),
    DELIVERED("Delivered"),
    HELD("Held"),
    RETURNED("Returned"),
    CANCELLED("Cancelled")
}

/** Trip lifecycle, separate from the consignment lifecycle (TransportApp.md §11.1). */
enum class TripState(val wording: String) {
    OPEN("Open"),
    ISSUED("Issued"),
    DISPATCHED("Dispatched"),
    CLOSED("Closed"),
    CANCELLED("Cancelled")
}

/** The five ranked grantable roles (TransportApp.md §17.4.1). */
enum class Role(val rank: Int, val label: String) {
    OWNER(5, "Owner"),
    MANAGER(4, "Manager"),
    ACCOUNTANT(3, "Accountant"),
    BOOKING_CLERK(2, "Booking Clerk"),
    DELIVERY_CLERK(1, "Delivery Clerk")
}
