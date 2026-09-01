package com.example.transportapp.core.ui

import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result

/**
 * Centralised user-facing copy for the §18.3 error-code catalogue (Spec.md §9). The data
 * layer returns typed codes; this is the only place a code becomes clerk-readable words.
 * A raw HTTP status or internal message is never shown directly (TransportApp.md §17.3:
 * "errors state cause and fix and never apologise").
 *
 * Every entry follows the fixed shape: cause, then fix. Sentence case, no greeting, no
 * apology. Wording may be tuned; the mapping must stay total over [ErrorCode] so a new
 * code cannot ship without copy.
 */
object ErrorCopy {

    /**
     * The copy to render for a failure. Repositories attach a specific detail (the
     * offending field, the exceeded figure) via [Result.Failure.message]; when present
     * that detail is shown after the canned cause+fix line so neither is lost.
     */
    fun resolve(error: Result.Failure): String {
        val detail = error.message?.takeIf { it.isNotBlank() }
        val canned = message(error)
        return if (detail == null) canned else "$canned ($detail)"
    }

    /** One-line cause + fix for a failure, ready to render in a banner or sheet. */
    fun message(error: Result.Failure): String = when (error.code) {
        ErrorCode.AUTH_EXPIRED -> "Session ended. Sign in again to continue."
        ErrorCode.AUTH_NO_ACCESS -> "This account cannot access that area. Ask the owner for the right role."
        ErrorCode.TENANT_MISMATCH -> "That record belongs to another company. Switch company first."
        ErrorCode.LEASE_EXHAUSTED -> "Number block used up and no server connection. The next booking uses a provisional number starting with PROV-; it is renumbered at the next sync."
        ErrorCode.LEASE_INVALID -> "Number series is not set up correctly. Check Settings > Numbering."
        ErrorCode.DUP_CLIENT_OP -> "This change was already saved a moment ago. Nothing else is pending."
        ErrorCode.CONSIGNMENT_IMMUTABLE -> "A delivered or cancelled consignment cannot be changed. Raise a supplement instead."
        ErrorCode.ALREADY_BILLED -> "This consignment is already on a freight bill. Cancel that bill first to return it to the pool."
        ErrorCode.TOPAY_UNCOLLECTED -> "To Pay is still uncollected. Collect it (or a manager can waive it) before delivery."
        ErrorCode.POD_REQUIRED -> "Delivery needs a proof of delivery, or a manager's waiver."
        ErrorCode.CAPACITY_EXCEEDED -> "Load exceeds vehicle capacity. A manager can approve the overload."
        ErrorCode.MASTER_IN_USE -> "That record is in use by bookings. Rename it, or merge duplicates instead of deleting."
        ErrorCode.TEMPLATE_VERSION_MISSING -> "The printed template version is missing on this device. Reconnect to sync templates before reprinting."
        ErrorCode.TEMPLATE_FIELD_UNKNOWN -> "The template refers to a field this app does not fill. Rebuild the template for this version."
        ErrorCode.PHOTO_QUALITY -> "Photo is blurry, dark or too small. Retake it flat, in even light, with all four edges visible."
        ErrorCode.EXPORT_TOO_LARGE -> "Export is over the 2,00,000-row limit. Narrow the date range or split by branch."
        ErrorCode.BILL_MIXED_TREATMENT -> "A bill cannot mix GST treatments. Split into separate bills per treatment."
        ErrorCode.TRIP_VEHICLE_BUSY -> "That vehicle is already on another trip. Choose a free vehicle or close the other challan."
        ErrorCode.SYNC_RESYNC_REQUIRED -> "Local data fell out of step. Re-sync will re-pull recent records; nothing you saved offline is lost."
        ErrorCode.OFFLINE_UNAVAILABLE -> "Needs a connection. Everything else keeps working offline — this finishes when you are back online."
    }

    /**
     * Short verb for the recovery action button, or null when the failure is terminal
     * (fix is outside the screen). Never a bare "OK".
     */
    fun action(error: Result.Failure): String? = when (error.code) {
        ErrorCode.AUTH_EXPIRED -> "Sign in"
        ErrorCode.AUTH_NO_ACCESS -> null
        ErrorCode.TENANT_MISMATCH -> "Switch company"
        ErrorCode.LEASE_EXHAUSTED -> null // the PROV- path proceeds automatically
        ErrorCode.LEASE_INVALID -> "Open numbering"
        ErrorCode.DUP_CLIENT_OP -> null
        ErrorCode.CONSIGNMENT_IMMUTABLE -> null
        ErrorCode.ALREADY_BILLED -> "Open bill"
        ErrorCode.TOPAY_UNCOLLECTED -> "Collect now"
        ErrorCode.POD_REQUIRED -> null
        ErrorCode.CAPACITY_EXCEEDED -> null // manager override is a separate flow, not an error-button
        ErrorCode.MASTER_IN_USE -> null
        ErrorCode.TEMPLATE_VERSION_MISSING -> "Retry"
        ErrorCode.TEMPLATE_FIELD_UNKNOWN -> null
        ErrorCode.PHOTO_QUALITY -> "Retake"
        ErrorCode.EXPORT_TOO_LARGE -> "Narrow range"
        ErrorCode.BILL_MIXED_TREATMENT -> null
        ErrorCode.TRIP_VEHICLE_BUSY -> null // the vehicle board is the fix, not a button here
        ErrorCode.SYNC_RESYNC_REQUIRED -> "Re-sync"
        ErrorCode.OFFLINE_UNAVAILABLE -> null // typed, expected state — the screen explains itself
    }
}
