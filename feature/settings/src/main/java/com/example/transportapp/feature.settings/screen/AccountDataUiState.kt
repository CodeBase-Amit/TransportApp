package com.example.transportapp.feature.settings.screen

import com.example.transportapp.core.ui.sample.AccountDataSampleData
import com.example.transportapp.core.ui.sample.SyncQueueItem

data class AccountDataUiState(
    val title: String = AccountDataSampleData.TITLE,
    val records: String = AccountDataSampleData.RECORDS,
    val space: String = AccountDataSampleData.SPACE,
    val cachedPdfs: String = AccountDataSampleData.CACHED_PDFS,
    val lastSync: String = AccountDataSampleData.LAST_SYNC,
    val waiting: String = AccountDataSampleData.WAITING,
    val syncNote: String = AccountDataSampleData.SYNC_NOTE,
    val tryNow: String = AccountDataSampleData.TRY_NOW,
    val clearLabel: String = AccountDataSampleData.CLEAR_CACHED_LABEL,
    val clearNote: String = AccountDataSampleData.CLEAR_CACHED_NOTE,
    val signOutLabel: String = AccountDataSampleData.SIGN_OUT_LABEL,
    val signOutNote: String = AccountDataSampleData.SIGN_OUT_NOTE,
    val leaveTitle: String = AccountDataSampleData.LEAVE_TITLE,
    val leaveBody: String = AccountDataSampleData.LEAVE_BODY,
    val leaveAction: String = AccountDataSampleData.LEAVE_ACTION,
    val deleteTitle: String = AccountDataSampleData.DELETE_TITLE,
    val deleteBody: String = AccountDataSampleData.DELETE_BODY,
    val deleteAction: String = AccountDataSampleData.DELETE_ACTION,
    val destroyLines: List<String> = AccountDataSampleData.destroyLines,
    val downloadLabel: String = AccountDataSampleData.downloadLabel,
    val downloadSub: String = AccountDataSampleData.downloadSub,
    val privacyLabel: String = AccountDataSampleData.privacyLabel,
    val syncQueue: List<SyncQueueItem> = AccountDataSampleData.syncQueue,
    val deleteDialogTitle: String = AccountDataSampleData.DELETE_DIALOG_TITLE,
    val deleteDialogBody: String = AccountDataSampleData.DELETE_DIALOG_BODY,
    val deleteDialogPlaceholder: String = AccountDataSampleData.DELETE_DIALOG_PLACEHOLDER,
    val deleteDialogAction: String = AccountDataSampleData.DELETE_DIALOG_ACTION,
    val deleteDialogCancel: String = AccountDataSampleData.DELETE_DIALOG_CANCEL,
    val deleteConfirmCompany: String = AccountDataSampleData.DELETE_CONFIRM_COMPANY,
    val deleteCounts: List<String> = AccountDataSampleData.deleteCounts,
    val showDeleteDialog: Boolean = false
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
