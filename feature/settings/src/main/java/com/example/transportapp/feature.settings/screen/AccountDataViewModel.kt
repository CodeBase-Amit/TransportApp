package com.example.transportapp.feature.settings.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.transport.account.AccountDataRepository
import com.example.transportapp.data.transport.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

/**
 * T31 — Account and data (§B31): storage facts, the real OUTBOX queue read as sentences,
 * and sign-out. The destructive leave/delete blocks stay visual until the online tier.
 */
@HiltViewModel
class AccountDataViewModel @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val sessionRepository: SessionRepository,
    @ApplicationContext appContext: android.content.Context,
) : ViewModel() {

    private val dbFile: File = appContext.getDatabasePath("transport.db")

    private val _uiState = MutableStateFlow(AccountDataUiState())
    val uiState: StateFlow<AccountDataUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            val data = accountDataRepository.phoneData()
            val walBytes = File(dbFile.parentFile, dbFile.name + "-wal").takeIf { it.exists() }?.length() ?: 0L
            val dbBytes = dbFile.length() + walBytes
            _uiState.update {
                it.copy(
                    records = data.records.toString(),
                    space = "${dbBytes / (1024 * 1024) + 1} MB",
                    waiting = if (data.queue.isEmpty()) "EVERYTHING IS SYNCED" else "WAITING TO SYNC · ${data.queue.size}",
                    syncQueue = data.queue.take(3).map { row ->
                        SyncQueueRowUi(
                            description = row.description,
                            atText = timeFormat.format(row.createdAt),
                            state = if (row.pending) "Pending" else "Syncing",
                        )
                    },
                )
            }
        }
    }

    fun onEvent(event: AccountDataEvent) {
        when (event) {
            AccountDataEvent.TrySync -> refresh()
            AccountDataEvent.ClearCached -> _uiState.update { it }
            AccountDataEvent.SignOut -> viewModelScope.launch {
                sessionRepository.signOut()
                _uiState.update { it.copy(signedOut = true) }
            }
            AccountDataEvent.Leave -> _uiState.update { it }
            AccountDataEvent.RequestDelete -> _uiState.update { it.copy(showDeleteDialog = true) }
            AccountDataEvent.CancelDelete -> _uiState.update { it.copy(showDeleteDialog = false) }
            AccountDataEvent.ConfirmDelete -> _uiState.update { it.copy(showDeleteDialog = false) }
        }
    }
}
