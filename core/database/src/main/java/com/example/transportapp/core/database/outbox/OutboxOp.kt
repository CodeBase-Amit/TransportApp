package com.example.transportapp.core.database.outbox

/**
 * Outbox vocabulary (TransportApp.md §16.2). Entity types mirror the server table names;
 * the `_E` suffix is the only renaming allowed, so no separate enum value list may drift.
 */
enum class OutboxOp { INSERT, UPDATE, DELETE }

enum class OutboxEntityType {
    COMPANY, BRANCH, MEMBERSHIP,
    PARTY, STATION, ROUTE, GOODS, VEHICLE, DRIVER, BROKER, CHARGE_HEAD, RATE_CARD,
    NUMBER_SERIES, NUMBER_LEASE,
    CONSIGNMENT, CONSIGNMENT_ITEM, CHARGE_LINE, STATUS_EVENT, ATTACHMENT, POD, DOC_SNAPSHOT,
    TRIP, TRIP_LEG, TRIP_COST, LORRY_HIRE,
    FREIGHT_BILL, CREDIT_NOTE, RECEIPT, RECEIPT_ALLOCATION,
    TEMPLATE, TEMPLATE_REQUEST
}

/** Drain lifecycle of a queued operation. Phase 2 never moves rows past PENDING (drain is a no-op). */
enum class OutboxState { PENDING, IN_FLIGHT, DONE, FAILED }
