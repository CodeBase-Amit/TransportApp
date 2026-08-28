package com.example.transportapp.core.common

/**
 * Typed, stable error codes from TransportApp.md §18.3. The UI maps each to plain-language
 * copy and a recovery action; a raw HTTP status is never shown to a clerk.
 */
enum class ErrorCode {
    AUTH_EXPIRED,
    AUTH_NO_ACCESS,
    TENANT_MISMATCH,
    LEASE_EXHAUSTED,
    LEASE_INVALID,
    DUP_CLIENT_OP,
    CONSIGNMENT_IMMUTABLE,
    ALREADY_BILLED,
    TOPAY_UNCOLLECTED,
    POD_REQUIRED,
    CAPACITY_EXCEEDED,
    MASTER_IN_USE,
    TEMPLATE_VERSION_MISSING,
    TEMPLATE_FIELD_UNKNOWN,
    PHOTO_QUALITY,
    EXPORT_TOO_LARGE,
    BILL_MIXED_TREATMENT,
    TRIP_VEHICLE_BUSY,
    SYNC_RESYNC_REQUIRED,
    OFFLINE_UNAVAILABLE
}
