package com.example.transportapp.core.ui.sample

data class SyncQueueItem(val ref: String, val description: String, val state: String)

/**
 * T31 Account and data demo data. UiState defaults all come from here so the
 * screen stays stateless and the Content composable never touches sample data.
 */
object AccountDataSampleData {

    const val TITLE = "Account and data"

    const val RECORDS = "1,204"
    const val SPACE = "42 MB"
    const val CACHED_PDFS = "12"
    const val LAST_SYNC = "Today, 10:42 AM"
    const val WAITING = "WAITING TO SYNC · 3"
    const val SYNC_NOTE = "They send themselves as soon as you have a connection. Nothing is lost by closing the app."
    const val TRY_NOW = "Try now"

    const val CLEAR_CACHED_LABEL = "Clear cached PDFs"
    const val CLEAR_CACHED_NOTE = "Frees space. They rebuild when printed again."

    const val SIGN_OUT_LABEL = "Sign out of TransportApp"
    const val SIGN_OUT_NOTE = "Data on this device will be kept"

    const val LEAVE_TITLE = "Leave Company"
    const val LEAVE_BODY = "Remove yourself from Shivshakti Roadlines. You will lose access, but company data remains."
    const val LEAVE_ACTION = "Leave this company"

    const val DELETE_TITLE = "Delete Company"
    const val DELETE_BODY = "Permanently destroy this company and all its data. This cannot be undone."
    const val DELETE_ACTION = "Delete this company"

    const val DOCKETS = "1,204"
    const val PARTIES = "342"
    const val VEHICLES = "85"

    val destroyLines = listOf(
        "Destroying 1,204 Dockets",
        "Destroying 342 Parties",
        "Destroying 85 Fleet Vehicles"
    )

    val downloadLabel = "Download your data"
    val downloadSub = "one Excel file, every record you can see"
    val privacyLabel = "Privacy Policy"

    val syncQueue: List<SyncQueueItem> = listOf(
        SyncQueueItem("Bilty #4092", "Update Status to Delivered", "Pending"),
        SyncQueueItem("New Party", "Shivshakti Traders", "Syncing"),
        SyncQueueItem("Payment ₹12,000", "for #4088", "Pending")
    )

    const val DELETE_DIALOG_TITLE = "Delete Shivshakti Roadlines?"
    const val DELETE_DIALOG_BODY = "You are about to queue this company for permanent deletion. Once the 7 day grace period ends, the following will be unrecoverable:"
    const val DELETE_DIALOG_PLACEHOLDER = "Type the company name to confirm"
    const val DELETE_DIALOG_ACTION = "Delete in 7 days"
    const val DELETE_DIALOG_CANCEL = "Keep company"
    const val DELETE_CONFIRM_COMPANY = "Shivshakti Roadlines"

    val deleteCounts = listOf("1,204 Dockets", "342 Parties", "85 Fleet Vehicles", "All Ledger Entries")
}
