package com.example.transportapp.feature.settings.screen

/** One row of the §B31 sync queue — the OUTBOX read as human sentences. */
data class SyncQueueRowUi(val description: String, val atText: String, val state: String)

data class AccountDataUiState(
    val title: String = "Account and data",
    val records: String = "…",
    val space: String = "…",
    val cachedPdfs: String = "0",
    val lastSync: String = "not yet synced",
    val waiting: String = "WAITING TO SYNC",
    val syncNote: String = "They send themselves as soon as you have a connection. Nothing is lost by closing the app.",
    val tryNow: String = "Try now",
    val clearLabel: String = "Clear cached PDFs",
    val clearNote: String = "Frees space. They rebuild when printed again.",
    val signOutLabel: String = "Sign out of TransportApp",
    val signOutNote: String = "Data on this device will be kept",
    val leaveTitle: String = "Leave Company",
    val leaveBody: String = "Remove yourself from Shivshakti Roadlines. You will lose access, but company data remains.",
    val leaveAction: String = "Leave this company",
    val deleteTitle: String = "Delete Company",
    val deleteBody: String = "Permanently destroy this company and all its data. This cannot be undone.",
    val deleteAction: String = "Delete this company",
    val destroyLines: List<String> = emptyList(),
    val downloadLabel: String = "Download your data",
    val downloadSub: String = "one Excel file, every record you can see",
    val privacyLabel: String = "Privacy Policy",
    val syncQueue: List<SyncQueueRowUi> = emptyList(),
    val deleteDialogTitle: String = "Delete Shivshakti Roadlines?",
    val deleteDialogBody: String = "You are about to queue this company for permanent deletion. Once the 7 day grace period ends, the following will be unrecoverable:",
    val deleteDialogPlaceholder: String = "Type the company name to confirm",
    val deleteDialogAction: String = "Delete in 7 days",
    val deleteDialogCancel: String = "Keep company",
    val deleteConfirmCompany: String = "Shivshakti Roadlines",
    val deleteCounts: List<String> = emptyList(),
    val showDeleteDialog: Boolean = false,
    val signedOut: Boolean = false,
)

sealed interface AccountDataEvent {
    data object TrySync : AccountDataEvent
    data object ClearCached : AccountDataEvent
    data object SignOut : AccountDataEvent
    data object Leave : AccountDataEvent
    data object RequestDelete : AccountDataEvent
    data object CancelDelete : AccountDataEvent
    data object ConfirmDelete : AccountDataEvent
}
